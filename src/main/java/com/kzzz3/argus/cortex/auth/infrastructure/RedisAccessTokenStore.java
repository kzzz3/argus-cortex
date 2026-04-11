package com.kzzz3.argus.cortex.auth.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import java.time.Duration;
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
	private final ObjectMapper objectMapper;

	public RedisAccessTokenStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public String issue(AccountRecord accountRecord) {
		String accessToken = "argus-" + UUID.randomUUID();
		redisTemplate.opsForValue().set(redisKey(accessToken), serialize(accountRecord), SESSION_TTL);
		return accessToken;
	}

	@Override
	public Optional<AccountRecord> findByToken(String accessToken) {
		String payload = redisTemplate.opsForValue().get(redisKey(accessToken));
		if (payload == null || payload.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(deserialize(payload));
	}

	private String redisKey(String accessToken) {
		return "argus:session:" + accessToken;
	}

	private String serialize(AccountRecord accountRecord) {
		try {
			return objectMapper.writeValueAsString(accountRecord);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize account session.", exception);
		}
	}

	private AccountRecord deserialize(String payload) {
		try {
			return objectMapper.readValue(payload, AccountRecord.class);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to deserialize account session.", exception);
		}
	}
}
