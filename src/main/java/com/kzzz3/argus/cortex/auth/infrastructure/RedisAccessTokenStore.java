package com.kzzz3.argus.cortex.auth.infrastructure;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class RedisAccessTokenStore implements AccessTokenStore {

	private static final Duration SESSION_TTL = Duration.ofDays(7);

	private final StringRedisTemplate redisTemplate;

	public RedisAccessTokenStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public String issue(AccountRecord accountRecord) {
		String accessToken = "argus-" + UUID.randomUUID();
		String redisKey = redisKey(accessToken);
		redisTemplate.opsForHash().put(redisKey, "accountId", accountRecord.accountId());
		redisTemplate.opsForHash().put(redisKey, "displayName", accountRecord.displayName());
		redisTemplate.opsForHash().put(redisKey, "password", accountRecord.password());
		redisTemplate.expire(redisKey, SESSION_TTL);
		return accessToken;
	}

	@Override
	public Optional<AccountRecord> findByToken(String accessToken) {
		String redisKey = redisKey(accessToken);
		Object accountId = redisTemplate.opsForHash().get(redisKey, "accountId");
		if (accountId == null) {
			return Optional.empty();
		}

		Object displayName = redisTemplate.opsForHash().get(redisKey, "displayName");
		Object password = redisTemplate.opsForHash().get(redisKey, "password");
		return Optional.of(
				new AccountRecord(
						String.valueOf(accountId),
						String.valueOf(displayName),
						String.valueOf(password)
				)
		);
	}

	private String redisKey(String accessToken) {
		return "argus:session:" + accessToken;
	}
}
