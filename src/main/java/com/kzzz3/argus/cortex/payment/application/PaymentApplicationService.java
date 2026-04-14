package com.kzzz3.argus.cortex.payment.application;

import com.kzzz3.argus.cortex.auth.application.AuthenticatedAccountResolver;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.payment.domain.PaymentMerchantNotFoundException;
import com.kzzz3.argus.cortex.payment.domain.PaymentRecord;
import com.kzzz3.argus.cortex.payment.domain.PaymentRecordStore;
import com.kzzz3.argus.cortex.payment.domain.PaymentScanSession;
import com.kzzz3.argus.cortex.payment.domain.PaymentScanSessionNotFoundException;
import com.kzzz3.argus.cortex.payment.domain.PaymentScanSessionStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentApplicationService {

	private static final String SUPPORTED_SCHEME = "argus";
	private static final String SUPPORTED_HOST = "pay";
	private static final String DEFAULT_CURRENCY = "CNY";

	private final AuthenticatedAccountResolver authenticatedAccountResolver;
	private final AccountStore accountStore;
	private final ConversationStore conversationStore;
	private final PaymentScanSessionStore paymentScanSessionStore;
	private final PaymentRecordStore paymentRecordStore;

	public PaymentApplicationService(
			AuthenticatedAccountResolver authenticatedAccountResolver,
			AccountStore accountStore,
			ConversationStore conversationStore,
			PaymentScanSessionStore paymentScanSessionStore,
			PaymentRecordStore paymentRecordStore
	) {
		this.authenticatedAccountResolver = authenticatedAccountResolver;
		this.accountStore = accountStore;
		this.conversationStore = conversationStore;
		this.paymentScanSessionStore = paymentScanSessionStore;
		this.paymentRecordStore = paymentRecordStore;
	}

	@Transactional
	public PaymentScanSession resolveScan(String accessToken, ResolvePaymentScanCommand command) {
		AccountRecord payer = authenticatedAccountResolver.resolve(accessToken);
		ParsedPaymentQr parsedPaymentQr = parseScanPayload(command.scanPayload());
		AccountRecord merchant = accountStore.findByAccountId(parsedPaymentQr.merchantAccountId())
				.orElseThrow(() -> new PaymentMerchantNotFoundException(parsedPaymentQr.merchantAccountId()));

		if (payer.accountId().equals(merchant.accountId())) {
			throw new IllegalArgumentException("You cannot pay your own merchant code.");
		}

		PaymentScanSession session = new PaymentScanSession(
				"payscan-" + UUID.randomUUID(),
				payer.accountId(),
				merchant.accountId(),
				merchant.displayName(),
				DEFAULT_CURRENCY,
				parsedPaymentQr.amount(),
				parsedPaymentQr.amount() == null,
				parsedPaymentQr.note(),
				LocalDateTime.now()
		);
		return paymentScanSessionStore.save(session);
	}

	@Transactional
	public PaymentRecord confirmPayment(String accessToken, String sessionId, ConfirmPaymentCommand command) {
		AccountRecord payer = authenticatedAccountResolver.resolve(accessToken);
		PaymentScanSession session = paymentScanSessionStore.findBySessionId(sessionId)
				.orElseThrow(() -> new PaymentScanSessionNotFoundException(sessionId));

		if (!payer.accountId().equals(session.payerAccountId())) {
			throw new IllegalArgumentException("Payment scan session does not belong to the current account.");
		}

		var existingRecord = paymentRecordStore.findBySessionId(sessionId);
		if (existingRecord.isPresent()) {
			return existingRecord.get();
		}

		AccountRecord merchant = accountStore.findByAccountId(session.merchantAccountId())
				.orElseThrow(() -> new PaymentMerchantNotFoundException(session.merchantAccountId()));
		BigDecimal amount = resolvePaymentAmount(session, command.amount());
		String note = resolvePaymentNote(session, command.note());
		var conversation = conversationStore.ensureDirectConversation(payer, merchant);

		PaymentRecord paymentRecord = new PaymentRecord(
				"payment-" + UUID.randomUUID(),
				session.sessionId(),
				payer.accountId(),
				merchant.accountId(),
				session.merchantDisplayName(),
				conversation.id(),
				amount,
				session.currency(),
				note,
				"COMPLETED",
				LocalDateTime.now()
		);
		return paymentRecordStore.save(paymentRecord);
	}

	private ParsedPaymentQr parseScanPayload(String scanPayload) {
		if (scanPayload == null || scanPayload.isBlank()) {
			throw new IllegalArgumentException("Scan payload is required.");
		}

		URI uri;
		try {
			uri = URI.create(scanPayload.trim());
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Scan payload is not a valid Argus QR code.");
		}

		if (!SUPPORTED_SCHEME.equalsIgnoreCase(uri.getScheme()) || !SUPPORTED_HOST.equalsIgnoreCase(uri.getHost())) {
			throw new IllegalArgumentException("Only Argus payment QR codes are supported.");
		}

		Map<String, String> query = parseQuery(uri.getRawQuery());
		String merchantAccountId = query.getOrDefault("merchantAccountId", "").trim();
		if (merchantAccountId.isBlank()) {
			throw new IllegalArgumentException("Payment QR code is missing merchantAccountId.");
		}

		BigDecimal amount = null;
		String rawAmount = query.get("amount");
		if (rawAmount != null && !rawAmount.isBlank()) {
			amount = normalizeAmount(rawAmount);
		}

		String note = query.getOrDefault("note", "").trim();
		return new ParsedPaymentQr(merchantAccountId, amount, note);
	}

	private BigDecimal resolvePaymentAmount(PaymentScanSession session, BigDecimal requestedAmount) {
		if (session.suggestedAmount() != null) {
			if (requestedAmount == null) {
				return session.suggestedAmount();
			}
			BigDecimal normalizedRequested = normalizeAmount(requestedAmount);
			if (normalizedRequested.compareTo(session.suggestedAmount()) != 0) {
				throw new IllegalArgumentException("This QR code has a fixed amount and cannot be edited.");
			}
			return normalizedRequested;
		}

		if (requestedAmount == null) {
			throw new IllegalArgumentException("Payment amount is required for editable QR codes.");
		}

		return normalizeAmount(requestedAmount);
	}

	private String resolvePaymentNote(PaymentScanSession session, String requestedNote) {
		String resolved = requestedNote == null || requestedNote.isBlank() ? session.suggestedNote() : requestedNote.trim();
		return resolved.length() > 255 ? resolved.substring(0, 255) : resolved;
	}

	private BigDecimal normalizeAmount(String rawAmount) {
		try {
			return normalizeAmount(new BigDecimal(rawAmount));
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Payment amount must be a valid decimal value.");
		}
	}

	private BigDecimal normalizeAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Payment amount must be greater than zero.");
		}
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private Map<String, String> parseQuery(String rawQuery) {
		Map<String, String> result = new LinkedHashMap<>();
		if (rawQuery == null || rawQuery.isBlank()) {
			return result;
		}

		for (String entry : rawQuery.split("&")) {
			if (entry.isBlank()) {
				continue;
			}
			String[] parts = entry.split("=", 2);
			String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
			String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
			result.put(key, value);
		}
		return result;
	}

	private record ParsedPaymentQr(String merchantAccountId, BigDecimal amount, String note) {
	}
}
