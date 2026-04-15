package com.kzzz3.argus.cortex.payment.web;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.payment.domain.WalletRecord;
import java.math.BigDecimal;

public record WalletSummaryResponse(
		String accountId,
		String displayName,
		BigDecimal balance,
		String currency
) {
	public static WalletSummaryResponse from(AccountRecord accountRecord, WalletRecord walletRecord) {
		return new WalletSummaryResponse(
				accountRecord.accountId(),
				accountRecord.displayName(),
				walletRecord.balance(),
				walletRecord.currency()
		);
	}
}
