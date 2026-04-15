package com.kzzz3.argus.cortex.payment.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kzzz3.argus.cortex.payment.domain.WalletRecord;
import com.kzzz3.argus.cortex.payment.domain.WalletStore;
import com.kzzz3.argus.cortex.payment.infrastructure.entity.WalletEntity;
import com.kzzz3.argus.cortex.payment.infrastructure.mapper.WalletMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisWalletStore implements WalletStore {

	private final WalletMapper walletMapper;

	public MybatisWalletStore(WalletMapper walletMapper) {
		this.walletMapper = walletMapper;
	}

	@Override
	public WalletRecord save(WalletRecord walletRecord) {
		walletMapper.insert(toEntity(walletRecord));
		return walletRecord;
	}

	@Override
	public Optional<WalletRecord> findByAccountId(String accountId) {
		if (accountId == null || accountId.isBlank()) {
			return Optional.empty();
		}
		WalletEntity entity = walletMapper.selectOne(new LambdaQueryWrapper<WalletEntity>()
				.eq(WalletEntity::getAccountId, accountId));
		return Optional.ofNullable(entity).map(this::toRecord);
	}

	@Override
	public Optional<WalletRecord> debit(String accountId, BigDecimal amount) {
		if (accountId == null || accountId.isBlank()) {
			return Optional.empty();
		}
		int updated = walletMapper.update(
				null,
				new LambdaUpdateWrapper<WalletEntity>()
						.eq(WalletEntity::getAccountId, accountId)
						.ge(WalletEntity::getBalance, amount)
						.setSql("balance = balance - " + amount.toPlainString())
						.set(WalletEntity::getUpdatedAt, LocalDateTime.now())
		);
		if (updated == 0) {
			return Optional.empty();
		}
		return findByAccountId(accountId);
	}

	@Override
	public WalletRecord credit(String accountId, BigDecimal amount) {
		int updated = walletMapper.update(
				null,
				new LambdaUpdateWrapper<WalletEntity>()
						.eq(WalletEntity::getAccountId, accountId)
						.setSql("balance = balance + " + amount.toPlainString())
						.set(WalletEntity::getUpdatedAt, LocalDateTime.now())
		);
		if (updated == 0) {
			throw new IllegalStateException("Wallet was not initialized for account: " + accountId);
		}
		return findByAccountId(accountId)
				.orElseThrow(() -> new IllegalStateException("Wallet was not found after credit for account: " + accountId));
	}

	private WalletEntity toEntity(WalletRecord walletRecord) {
		WalletEntity entity = new WalletEntity();
		entity.setAccountId(walletRecord.accountId());
		entity.setCurrency(walletRecord.currency());
		entity.setBalance(walletRecord.balance());
		entity.setUpdatedAt(walletRecord.updatedAt());
		return entity;
	}

	private WalletRecord toRecord(WalletEntity entity) {
		return new WalletRecord(
				entity.getAccountId(),
				entity.getCurrency(),
				entity.getBalance(),
				entity.getUpdatedAt()
		);
	}
}
