package com.kzzz3.argus.cortex.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletRecord(
		String accountId,
		String currency,
		BigDecimal balance,
		LocalDateTime updatedAt
) {
}
