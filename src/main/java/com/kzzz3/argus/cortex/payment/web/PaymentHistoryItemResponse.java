package com.kzzz3.argus.cortex.payment.web;

import com.kzzz3.argus.cortex.payment.domain.PaymentRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentHistoryItemResponse(
		String paymentId,
		String merchantDisplayName,
		BigDecimal amount,
		String currency,
		String status,
		LocalDateTime paidAt
) {
	public static PaymentHistoryItemResponse from(PaymentRecord paymentRecord) {
		return new PaymentHistoryItemResponse(
				paymentRecord.paymentId(),
				paymentRecord.merchantDisplayName(),
				paymentRecord.amount(),
				paymentRecord.currency(),
				paymentRecord.status(),
				paymentRecord.createdAt()
		);
	}
}
