package com.kzzz3.argus.cortex.payment.application;

import com.kzzz3.argus.cortex.auth.application.AuthenticatedAccountResolver;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.payment.domain.PaymentRecipientNotFoundException;
import com.kzzz3.argus.cortex.payment.domain.PaymentRecord;
import com.kzzz3.argus.cortex.payment.domain.PaymentRecordNotFoundException;
import com.kzzz3.argus.cortex.payment.domain.PaymentRecordStore;
import com.kzzz3.argus.cortex.payment.domain.PaymentScanSession;
import com.kzzz3.argus.cortex.payment.domain.PaymentScanSessionNotFoundException;
import com.kzzz3.argus.cortex.payment.domain.PaymentScanSessionStore;
import com.kzzz3.argus.cortex.payment.domain.WalletBalanceInsufficientException;
import com.kzzz3.argus.cortex.payment.domain.WalletRecord;
import com.kzzz3.argus.cortex.payment.domain.WalletStore;
import com.kzzz3.argus.cortex.payment.web.WalletSummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
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
	private static final BigDecimal DEFAULT_INITIAL_BALANCE = new BigDecimal("1000.00");

	private final AuthenticatedAccountResolver authenticatedAccountResolver;
	private final AccountStore accountStore;
	private final PaymentScanSessionStore paymentScanSessionStore;
	private final PaymentRecordStore paymentRecordStore;
	private final WalletStore walletStore;

	public PaymentApplicationService(
			AuthenticatedAccountResolver authenticatedAccountResolver,
			AccountStore accountStore,
			PaymentScanSessionStore paymentScanSessionStore,
			PaymentRecordStore paymentRecordStore,
			WalletStore walletStore
	) {
		this.authenticatedAccountResolver = authenticatedAccountResolver;
		this.accountStore = accountStore;
		this.paymentScanSessionStore = paymentScanSessionStore;
		this.paymentRecordStore = paymentRecordStore;
		this.walletStore = walletStore;
	}

	@Transactional
	public WalletSummaryResponse getWalletSummary() {
		AccountRecord account = authenticatedAccountResolver.resolveCurrent();
		WalletRecord walletRecord = ensureWallet(account);
		return WalletSummaryResponse.from(account, walletRecord);
	}

	@Transactional
	public PaymentScanSession resolveScan(ResolvePaymentScanCommand command) {
		AccountRecord payer = authenticatedAccountResolver.resolveCurrent();
		ensureWallet(payer);
		ParsedPaymentQr parsedPaymentQr = parseScanPayload(command.scanPayload());
		AccountRecord recipient = accountStore.findByAccountId(parsedPaymentQr.recipientAccountId())
				.orElseThrow(() -> new PaymentRecipientNotFoundException(parsedPaymentQr.recipientAccountId()));
		ensureWallet(recipient);

		if (payer.accountId().equals(recipient.accountId())) {
			throw new IllegalArgumentException("You cannot pay your own collection code.");
		}

		PaymentScanSession session = new PaymentScanSession(
				"payscan-" + UUID.randomUUID(),
				payer.accountId(),
				recipient.accountId(),
				recipient.displayName(),
				DEFAULT_CURRENCY,
				parsedPaymentQr.amount(),
				parsedPaymentQr.amount() == null,
				parsedPaymentQr.note(),
				LocalDateTime.now()
		);
		return paymentScanSessionStore.save(session);
	}

	@Transactional
	public PaymentRecord confirmPayment(String sessionId, ConfirmPaymentCommand command) {
		AccountRecord payer = authenticatedAccountResolver.resolveCurrent();
		ensureWallet(payer);
		PaymentScanSession session = paymentScanSessionStore.findBySessionId(sessionId)
				.orElseThrow(() -> new PaymentScanSessionNotFoundException(sessionId));

		if (!payer.accountId().equals(session.payerAccountId())) {
			throw new IllegalArgumentException("Payment scan session does not belong to the current account.");
		}

		var existingRecord = paymentRecordStore.findBySessionId(sessionId);
		if (existingRecord.isPresent()) {
			return existingRecord.get();
		}

		AccountRecord recipient = accountStore.findByAccountId(session.recipientAccountId())
				.orElseThrow(() -> new PaymentRecipientNotFoundException(session.recipientAccountId()));
		ensureWallet(recipient);
		BigDecimal amount = resolvePaymentAmount(session, command.amount());
		String note = resolvePaymentNote(session, command.note());
		WalletRecord debitedPayerWallet = walletStore.debit(payer.accountId(), amount)
				.orElseThrow(() -> new WalletBalanceInsufficientException(payer.accountId()));
		WalletRecord creditedRecipientWallet = walletStore.credit(recipient.accountId(), amount);

		PaymentRecord paymentRecord = new PaymentRecord(
				"payment-" + UUID.randomUUID(),
				session.sessionId(),
				payer.accountId(),
				payer.displayName(),
				debitedPayerWallet.balance(),
				recipient.accountId(),
				session.recipientDisplayName(),
				creditedRecipientWallet.balance(),
				amount,
				session.currency(),
				note,
				"COMPLETED",
				LocalDateTime.now()
		);
		return paymentRecordStore.save(paymentRecord);
	}

	public List<PaymentRecord> listPayments() {
		AccountRecord account = authenticatedAccountResolver.resolveCurrent();
		ensureWallet(account);
		return paymentRecordStore.listByParticipantAccountId(account.accountId());
	}

	public PaymentRecord getPaymentReceipt(String paymentId) {
		AccountRecord account = authenticatedAccountResolver.resolveCurrent();
		ensureWallet(account);
		return paymentRecordStore.findByPaymentIdAndParticipantAccountId(paymentId, account.accountId())
				.orElseThrow(() -> new PaymentRecordNotFoundException(paymentId));
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
		String recipientAccountId = query.getOrDefault("recipientAccountId", "").trim();
		if (recipientAccountId.isBlank()) {
			throw new IllegalArgumentException("Payment QR code is missing recipientAccountId.");
		}

		BigDecimal amount = null;
		String rawAmount = query.get("amount");
		if (rawAmount != null && !rawAmount.isBlank()) {
			amount = normalizeAmount(rawAmount);
		}

		String note = query.getOrDefault("note", "").trim();
		return new ParsedPaymentQr(recipientAccountId, amount, note);
	}

	private BigDecimal resolvePaymentAmount(PaymentScanSession session, BigDecimal requestedAmount) {
		if (session.requestedAmount() != null) {
			if (requestedAmount == null) {
				return session.requestedAmount();
			}
			BigDecimal normalizedRequested = normalizeAmount(requestedAmount);
			if (normalizedRequested.compareTo(session.requestedAmount()) != 0) {
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
		String resolved = requestedNote == null || requestedNote.isBlank() ? session.requestedNote() : requestedNote.trim();
		return resolved.length() > 255 ? resolved.substring(0, 255) : resolved;
	}

	private WalletRecord ensureWallet(AccountRecord accountRecord) {
		return walletStore.findByAccountId(accountRecord.accountId())
				.orElseGet(() -> walletStore.save(new WalletRecord(
						accountRecord.accountId(),
						DEFAULT_CURRENCY,
						DEFAULT_INITIAL_BALANCE,
						LocalDateTime.now()
				)));
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

	private record ParsedPaymentQr(String recipientAccountId, BigDecimal amount, String note) {
	}
}
