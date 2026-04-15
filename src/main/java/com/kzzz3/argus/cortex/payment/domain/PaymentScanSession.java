package com.kzzz3.argus.cortex.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

public record PaymentScanSession(
		String sessionId,
		String payerAccountId,
		String recipientAccountId,
		String recipientDisplayName,
		String currency,
		@Nullable BigDecimal requestedAmount,
		boolean amountEditable,
		String requestedNote,
		LocalDateTime createdAt
) {
}
