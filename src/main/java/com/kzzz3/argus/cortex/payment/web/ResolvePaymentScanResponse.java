package com.kzzz3.argus.cortex.payment.web;

import com.kzzz3.argus.cortex.payment.domain.PaymentScanSession;
import java.math.BigDecimal;

public record ResolvePaymentScanResponse(
		String scanSessionId,
		String recipientAccountId,
		String recipientDisplayName,
		String currency,
		BigDecimal requestedAmount,
		boolean amountEditable,
		String requestedNote
) {
	public static ResolvePaymentScanResponse from(PaymentScanSession session) {
		return new ResolvePaymentScanResponse(
				session.sessionId(),
				session.recipientAccountId(),
				session.recipientDisplayName(),
				session.currency(),
				session.requestedAmount(),
				session.amountEditable(),
				session.requestedNote()
		);
	}
}
