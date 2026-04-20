package com.kzzz3.argus.cortex.media.infrastructure;

import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSession;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSessionStore;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class RedisMediaUploadSessionStore implements MediaUploadSessionStore {

    private static final Duration SESSION_TTL = Duration.ofDays(1);
    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_ACCOUNT_ID = "accountId";
    private static final String FIELD_ATTACHMENT_TYPE = "attachmentType";
    private static final String FIELD_OBJECT_KEY = "objectKey";
    private static final String FIELD_UPLOAD_URL = "uploadUrl";
    private static final String FIELD_MAX_PAYLOAD_BYTES = "maxPayloadBytes";
    private static final String FIELD_UPLOAD_TOKEN = "uploadToken";
    private static final String FIELD_INSTRUCTIONS = "instructions";
    private static final String FIELD_UPLOADED = "uploaded";

    private final StringRedisTemplate redisTemplate;

    public RedisMediaUploadSessionStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public MediaUploadSession save(MediaUploadSession session) {
        String key = redisKey(session.sessionId());
        Map<String, String> payload = serialize(session);
        redisTemplate.opsForHash().putAll(key, payload);
        redisTemplate.expire(key, SESSION_TTL);
        return session;
    }

    @Override
    public Optional<MediaUploadSession> findBySessionId(String sessionId) {
        String key = redisKey(sessionId);
        Map<Object, Object> stored = redisTemplate.opsForHash().entries(key);
        if (stored == null || stored.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapToSession(stored));
    }

    @Override
    public MediaUploadSession markUploaded(String sessionId, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key is required when marking upload as complete.");
        }
        String key = redisKey(sessionId);
        Map<String, String> updates = Map.of(
                FIELD_OBJECT_KEY, objectKey,
                FIELD_UPLOADED, Boolean.TRUE.toString()
        );
        redisTemplate.opsForHash().putAll(key, updates);
        redisTemplate.expire(key, SESSION_TTL);
        Map<Object, Object> stored = redisTemplate.opsForHash().entries(key);
        if (stored == null || stored.isEmpty()) {
            throw new IllegalArgumentException("Upload session not found.");
        }
        return mapToSession(stored);
    }

    private MediaUploadSession mapToSession(Map<Object, Object> stored) {
        String sessionId = requiredValue(stored, FIELD_SESSION_ID);
        String accountId = requiredValue(stored, FIELD_ACCOUNT_ID);
        MediaAttachmentType attachmentType = MediaAttachmentType.valueOf(requiredValue(stored, FIELD_ATTACHMENT_TYPE));
        String objectKey = optionalValue(stored, FIELD_OBJECT_KEY);
        String uploadUrl = requiredValue(stored, FIELD_UPLOAD_URL);
        long maxPayloadBytes = Long.parseLong(requiredValue(stored, FIELD_MAX_PAYLOAD_BYTES));
        String uploadToken = requiredValue(stored, FIELD_UPLOAD_TOKEN);
        String instructions = requiredValue(stored, FIELD_INSTRUCTIONS);
        boolean uploaded = Boolean.parseBoolean(optionalValue(stored, FIELD_UPLOADED));
        Map<String, String> uploadHeaders = Map.of(
                "X-Upload-Session", sessionId,
                "X-Attachment-Type", attachmentType.name()
        );
        return new MediaUploadSession(
                sessionId,
                accountId,
                attachmentType,
                objectKey,
                uploadUrl,
                maxPayloadBytes,
                uploadToken,
                uploadHeaders,
                instructions,
                uploaded
        );
    }

    private String redisKey(String sessionId) {
        return "argus:media-upload-session:" + sessionId;
    }

    private Map<String, String> serialize(MediaUploadSession session) {
        Map<String, String> payload = new HashMap<>();
        payload.put(FIELD_SESSION_ID, session.sessionId());
        payload.put(FIELD_ACCOUNT_ID, session.accountId());
        payload.put(FIELD_ATTACHMENT_TYPE, session.attachmentType().name());
        payload.put(FIELD_OBJECT_KEY, session.objectKey());
        payload.put(FIELD_UPLOAD_URL, session.uploadUrl());
        payload.put(FIELD_MAX_PAYLOAD_BYTES, Long.toString(session.maxPayloadBytes()));
        payload.put(FIELD_UPLOAD_TOKEN, session.uploadToken());
        payload.put(FIELD_INSTRUCTIONS, session.instructions());
        payload.put(FIELD_UPLOADED, Boolean.toString(session.uploaded()));
        return payload;
    }

    private String requiredValue(Map<Object, Object> stored, String fieldName) {
        return String.valueOf(stored.get(fieldName));
    }

    private String optionalValue(Map<Object, Object> stored, String fieldName) {
        Object value = stored.get(fieldName);
        return value == null ? null : value.toString();
    }
}
