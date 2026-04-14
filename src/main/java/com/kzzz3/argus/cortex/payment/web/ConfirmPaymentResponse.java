package com.kzzz3.argus.cortex.payment.web;

import com.kzzz3.argus.cortex.payment.domain.PaymentRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConfirmPaymentResponse(
		String paymentId,
		String status,
		String merchantAccountId,
		String merchantDisplayName,
		String conversationId,
		BigDecimal amount,
		String currency,
		String note,
		LocalDateTime paidAt
) {
	public static ConfirmPaymentResponse from(PaymentRecord paymentRecord) {
		return new ConfirmPaymentResponse(
				paymentRecord.paymentId(),
				paymentRecord.status(),
				paymentRecord.merchantAccountId(),
				paymentRecord.merchantDisplayName(),
				paymentRecord.conversationId(),
				paymentRecord.amount(),
				paymentRecord.currency(),
				paymentRecord.note(),
				paymentRecord.createdAt()
		);
	}
}
