package com.kzzz3.argus.cortex.auth.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kzzz3.argus.cortex.auth.domain.RefreshSessionRecord;
import com.kzzz3.argus.cortex.auth.domain.RefreshSessionStore;
import com.kzzz3.argus.cortex.auth.infrastructure.entity.AuthRefreshSessionEntity;
import com.kzzz3.argus.cortex.auth.infrastructure.mapper.AuthRefreshSessionMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisRefreshSessionStore implements RefreshSessionStore {
	private final AuthRefreshSessionMapper mapper;

	public MybatisRefreshSessionStore(AuthRefreshSessionMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public void create(RefreshSessionRecord record) {
		mapper.insert(toEntity(record));
	}

	@Override
	public Optional<RefreshSessionRecord> findActiveByTokenHash(String tokenHash, LocalDateTime now) {
		AuthRefreshSessionEntity entity = mapper.selectOne(new LambdaQueryWrapper<AuthRefreshSessionEntity>()
				.eq(AuthRefreshSessionEntity::getRefreshTokenHash, tokenHash)
				.isNull(AuthRefreshSessionEntity::getRevokedAt)
				.gt(AuthRefreshSessionEntity::getExpiresAt, now));
		return Optional.ofNullable(entity).map(this::toRecord);
	}

	@Override
	public RefreshSessionRecord rotate(String sessionId, String nextTokenHash, LocalDateTime nextExpiresAt, LocalDateTime rotatedAt) {
		AuthRefreshSessionEntity entity = mapper.selectById(sessionId);
		entity.setRefreshTokenHash(nextTokenHash);
		entity.setExpiresAt(nextExpiresAt);
		entity.setUpdatedAt(rotatedAt);
		entity.setRevokedAt(null);
		mapper.updateById(entity);
		return toRecord(entity);
	}

	@Override
	public void revoke(String sessionId, LocalDateTime revokedAt) {
		AuthRefreshSessionEntity entity = mapper.selectById(sessionId);
		if (entity == null) return;
		entity.setRevokedAt(revokedAt);
		entity.setUpdatedAt(revokedAt);
		mapper.updateById(entity);
	}

	private AuthRefreshSessionEntity toEntity(RefreshSessionRecord record) {
		AuthRefreshSessionEntity entity = new AuthRefreshSessionEntity();
		entity.setSessionId(record.sessionId());
		entity.setAccountId(record.accountId());
		entity.setDisplayName(record.displayName());
		entity.setRefreshTokenHash(record.refreshTokenHash());
		entity.setExpiresAt(record.expiresAt());
		entity.setCreatedAt(record.createdAt());
		entity.setUpdatedAt(record.updatedAt());
		entity.setRevokedAt(record.revokedAt());
		return entity;
	}

	private RefreshSessionRecord toRecord(AuthRefreshSessionEntity entity) {
		return new RefreshSessionRecord(
				entity.getSessionId(),
				entity.getAccountId(),
				entity.getDisplayName(),
				entity.getRefreshTokenHash(),
				entity.getExpiresAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getRevokedAt()
		);
	}
}
