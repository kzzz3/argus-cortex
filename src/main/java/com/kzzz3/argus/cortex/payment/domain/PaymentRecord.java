package com.kzzz3.argus.cortex.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRecord(
		String paymentId,
		String sessionId,
		String payerAccountId,
		String merchantAccountId,
		String merchantDisplayName,
		String conversationId,
		BigDecimal amount,
		String currency,
		String note,
		String status,
		LocalDateTime createdAt
) {
}
