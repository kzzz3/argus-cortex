package com.kzzz3.argus.cortex.media.infrastructure;

import com.kzzz3.argus.cortex.media.domain.MediaAttachmentRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentStore;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryMediaAttachmentStore implements MediaAttachmentStore {

	private final Map<String, MediaAttachmentRecord> records = new ConcurrentHashMap<>();

	@Override
	public MediaAttachmentRecord save(MediaAttachmentRecord record) {
		records.put(record.attachmentId(), record);
		return record;
	}

	@Override
	public Optional<MediaAttachmentRecord> findByAttachmentId(String attachmentId) {
		if (attachmentId == null || attachmentId.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(records.get(attachmentId));
	}
}
