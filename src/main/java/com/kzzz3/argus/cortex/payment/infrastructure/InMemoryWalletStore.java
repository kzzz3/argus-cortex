package com.kzzz3.argus.cortex.payment.infrastructure;

import com.kzzz3.argus.cortex.payment.domain.WalletRecord;
import com.kzzz3.argus.cortex.payment.domain.WalletStore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryWalletStore implements WalletStore {

	private final ConcurrentMap<String, WalletRecord> walletsByAccountId = new ConcurrentHashMap<>();

	@Override
	public WalletRecord save(WalletRecord walletRecord) {
		walletsByAccountId.putIfAbsent(walletRecord.accountId(), walletRecord);
		return walletsByAccountId.get(walletRecord.accountId());
	}

	@Override
	public Optional<WalletRecord> findByAccountId(String accountId) {
		return Optional.ofNullable(walletsByAccountId.get(accountId));
	}

	@Override
	public Optional<WalletRecord> debit(String accountId, BigDecimal amount) {
		synchronized (walletsByAccountId) {
			WalletRecord existing = walletsByAccountId.get(accountId);
			if (existing == null || existing.balance().compareTo(amount) < 0) {
				return Optional.empty();
			}
			WalletRecord updated = new WalletRecord(
					existing.accountId(),
					existing.currency(),
					existing.balance().subtract(amount),
					LocalDateTime.now()
			);
			walletsByAccountId.put(accountId, updated);
			return Optional.of(updated);
		}
	}

	@Override
	public WalletRecord credit(String accountId, BigDecimal amount) {
		WalletRecord updated = walletsByAccountId.compute(accountId, (key, existing) -> {
			if (existing == null) {
				throw new IllegalStateException("Wallet was not initialized for account: " + accountId);
			}
			return new WalletRecord(
					existing.accountId(),
					existing.currency(),
					existing.balance().add(amount),
					LocalDateTime.now()
			);
		});
		return updated;
	}
}
