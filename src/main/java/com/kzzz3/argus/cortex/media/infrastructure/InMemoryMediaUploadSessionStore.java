package com.kzzz3.argus.cortex.media.infrastructure;

import com.kzzz3.argus.cortex.media.domain.MediaUploadSession;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSessionStore;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryMediaUploadSessionStore implements MediaUploadSessionStore {

	private final Map<String, MediaUploadSession> sessions = new ConcurrentHashMap<>();

	@Override
	public MediaUploadSession save(MediaUploadSession session) {
		sessions.put(session.sessionId(), session);
		return session;
	}

	@Override
	public Optional<MediaUploadSession> findBySessionId(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(sessions.get(sessionId));
	}

	@Override
	public MediaUploadSession markUploaded(String sessionId, String objectKey) {
		if (objectKey == null || objectKey.isBlank()) {
			throw new IllegalArgumentException("Object key is required when marking upload as complete.");
		}
		MediaUploadSession existing = sessions.get(sessionId);
		if (existing == null) {
			throw new IllegalArgumentException("Upload session not found.");
		}
		MediaUploadSession updated = new MediaUploadSession(
				existing.sessionId(),
				existing.accountId(),
				existing.attachmentType(),
				objectKey,
				existing.uploadUrl(),
				existing.maxPayloadBytes(),
				existing.uploadToken(),
				existing.uploadHeaders(),
				existing.instructions(),
				true
		);
		sessions.put(sessionId, updated);
		return updated;
	}
}
