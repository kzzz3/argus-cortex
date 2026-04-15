package com.kzzz3.argus.cortex.payment.web;

import com.kzzz3.argus.cortex.payment.domain.PaymentRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentHistoryItemResponse(
		String paymentId,
		String payerAccountId,
		String payerDisplayName,
		String recipientAccountId,
		String recipientDisplayName,
		BigDecimal amount,
		String currency,
		String status,
		LocalDateTime paidAt
) {
	public static PaymentHistoryItemResponse from(PaymentRecord paymentRecord) {
		return new PaymentHistoryItemResponse(
				paymentRecord.paymentId(),
				paymentRecord.payerAccountId(),
				paymentRecord.payerDisplayName(),
				paymentRecord.recipientAccountId(),
				paymentRecord.recipientDisplayName(),
				paymentRecord.amount(),
				paymentRecord.currency(),
				paymentRecord.status(),
				paymentRecord.createdAt()
		);
	}
}
