package com.kzzz3.argus.cortex.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

public record PaymentScanSession(
		String sessionId,
		String payerAccountId,
		String merchantAccountId,
		String merchantDisplayName,
		String currency,
		@Nullable BigDecimal suggestedAmount,
		boolean amountEditable,
		String suggestedNote,
		LocalDateTime createdAt
) {
}
