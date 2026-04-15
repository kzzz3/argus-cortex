package com.kzzz3.argus.cortex.payment.domain;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletStore {

	WalletRecord save(WalletRecord walletRecord);

	Optional<WalletRecord> findByAccountId(String accountId);

	Optional<WalletRecord> debit(String accountId, BigDecimal amount);

	WalletRecord credit(String accountId, BigDecimal amount);
}
