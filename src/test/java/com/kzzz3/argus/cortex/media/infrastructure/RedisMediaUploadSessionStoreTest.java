package com.kzzz3.argus.cortex.media.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSession;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisMediaUploadSessionStoreTest {

    @Test
    void findBySessionId_restoresStoredSessionFields() {
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("argus:media-upload-session:session-1")).thenReturn(Map.of(
                "sessionId", "session-1",
                "accountId", "tester",
                "attachmentType", "IMAGE",
                "objectKey", "media/object-key",
                "uploadUrl", "https://upload.example.com",
                "maxPayloadBytes", "4096",
                "uploadToken", "upload-token",
                "instructions", "Upload the file body as-is.",
                "uploaded", "false"
        ));

        RedisMediaUploadSessionStore store = new RedisMediaUploadSessionStore(redisTemplate);

        MediaUploadSession session = store.findBySessionId("session-1").orElseThrow();

        assertEquals("session-1", session.sessionId());
        assertEquals("tester", session.accountId());
        assertEquals(MediaAttachmentType.IMAGE, session.attachmentType());
        assertEquals("media/object-key", session.objectKey());
        assertEquals("https://upload.example.com", session.uploadUrl());
        assertEquals(4096L, session.maxPayloadBytes());
        assertEquals("upload-token", session.uploadToken());
        assertEquals("Upload the file body as-is.", session.instructions());
        assertEquals(false, session.uploaded());
        assertEquals("session-1", session.uploadHeaders().get("X-Upload-Session"));
    }

    @Test
    void markUploaded_marksStoredSessionAsUploaded() {
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("argus:media-upload-session:session-1")).thenReturn(Map.of(
                "sessionId", "session-1",
                "accountId", "tester",
                "attachmentType", "VOICE",
                "objectKey", "media/uploaded-object",
                "uploadUrl", "https://upload.example.com",
                "maxPayloadBytes", "2048",
                "uploadToken", "upload-token",
                "instructions", "Upload the file body as-is.",
                "uploaded", "true"
        ));

        RedisMediaUploadSessionStore store = new RedisMediaUploadSessionStore(redisTemplate);

        MediaUploadSession session = store.markUploaded("session-1", "media/uploaded-object");

        assertEquals("session-1", session.sessionId());
        assertEquals("media/uploaded-object", session.objectKey());
        assertTrue(session.uploaded());
    }
}
