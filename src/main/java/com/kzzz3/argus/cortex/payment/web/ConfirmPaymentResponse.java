package com.kzzz3.argus.cortex.payment.web;

import com.kzzz3.argus.cortex.payment.domain.PaymentRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConfirmPaymentResponse(
		String paymentId,
		String scanSessionId,
		String status,
		String payerAccountId,
		String payerDisplayName,
		BigDecimal payerBalanceAfter,
		String recipientAccountId,
		String recipientDisplayName,
		BigDecimal recipientBalanceAfter,
		BigDecimal amount,
		String currency,
		String note,
		LocalDateTime paidAt
) {
	public static ConfirmPaymentResponse from(PaymentRecord paymentRecord) {
		return new ConfirmPaymentResponse(
				paymentRecord.paymentId(),
				paymentRecord.sessionId(),
				paymentRecord.status(),
				paymentRecord.payerAccountId(),
				paymentRecord.payerDisplayName(),
				paymentRecord.payerBalanceAfter(),
				paymentRecord.recipientAccountId(),
				paymentRecord.recipientDisplayName(),
				paymentRecord.recipientBalanceAfter(),
				paymentRecord.amount(),
				paymentRecord.currency(),
				paymentRecord.note(),
				paymentRecord.createdAt()
		);
	}
}
