package com.kzzz3.argus.cortex.payment.web;

import com.kzzz3.argus.cortex.payment.domain.PaymentScanSession;
import java.math.BigDecimal;

public record ResolvePaymentScanResponse(
		String scanSessionId,
		String merchantAccountId,
		String merchantDisplayName,
		String currency,
		BigDecimal suggestedAmount,
		boolean amountEditable,
		String suggestedNote
) {
	public static ResolvePaymentScanResponse from(PaymentScanSession session) {
		return new ResolvePaymentScanResponse(
				session.sessionId(),
				session.merchantAccountId(),
				session.merchantDisplayName(),
				session.currency(),
				session.suggestedAmount(),
				session.amountEditable(),
				session.suggestedNote()
		);
	}
}
