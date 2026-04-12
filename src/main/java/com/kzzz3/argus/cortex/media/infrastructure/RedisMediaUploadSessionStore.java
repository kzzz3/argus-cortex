package com.kzzz3.argus.cortex.media.infrastructure;

import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSession;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSessionStore;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisMediaUploadSessionStore implements MediaUploadSessionStore {

    private static final Duration SESSION_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    public RedisMediaUploadSessionStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public MediaUploadSession save(MediaUploadSession session) {
        String key = redisKey(session.sessionId());
        Map<String, String> payload = new HashMap<>();
        payload.put("sessionId", session.sessionId());
        payload.put("accountId", session.accountId());
        payload.put("attachmentType", session.attachmentType().name());
        payload.put("objectKey", session.objectKey());
        payload.put("uploadUrl", session.uploadUrl());
        payload.put("maxPayloadBytes", Long.toString(session.maxPayloadBytes()));
        payload.put("uploadToken", session.uploadToken());
        payload.put("instructions", session.instructions());
        payload.put("uploaded", Boolean.toString(session.uploaded()));
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
                "objectKey", objectKey,
                "uploaded", "true"
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
        String sessionId = String.valueOf(stored.get("sessionId"));
        String accountId = String.valueOf(stored.get("accountId"));
        MediaAttachmentType attachmentType = MediaAttachmentType.valueOf(String.valueOf(stored.get("attachmentType")));
        String objectKey = null;
        Object savedObjectKey = stored.get("objectKey");
        if (savedObjectKey != null) {
            objectKey = savedObjectKey.toString();
        }
        String uploadUrl = String.valueOf(stored.get("uploadUrl"));
        long maxPayloadBytes = Long.parseLong(String.valueOf(stored.get("maxPayloadBytes")));
        String uploadToken = String.valueOf(stored.get("uploadToken"));
        String instructions = String.valueOf(stored.get("instructions"));
        boolean uploaded = false;
        Object uploadedField = stored.get("uploaded");
        if (uploadedField != null) {
            uploaded = Boolean.parseBoolean(uploadedField.toString());
        }
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
}
