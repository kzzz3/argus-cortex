package com.kzzz3.argus.cortex.media.infrastructure;

import com.kzzz3.argus.cortex.media.domain.MediaAttachmentRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentStore;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;
import com.kzzz3.argus.cortex.media.infrastructure.entity.MediaAttachmentEntity;
import com.kzzz3.argus.cortex.media.infrastructure.mapper.MediaAttachmentMapper;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MyBatisMediaAttachmentStore implements MediaAttachmentStore {

    private final MediaAttachmentMapper mediaAttachmentMapper;

    public MyBatisMediaAttachmentStore(MediaAttachmentMapper mediaAttachmentMapper) {
        this.mediaAttachmentMapper = mediaAttachmentMapper;
    }

    @Override
    public MediaAttachmentRecord save(MediaAttachmentRecord record) {
        mediaAttachmentMapper.insert(toEntity(record));
        return record;
    }

    @Override
    public Optional<MediaAttachmentRecord> findByAttachmentId(String attachmentId) {
        if (attachmentId == null || attachmentId.isBlank()) {
            return Optional.empty();
        }
        MediaAttachmentEntity entity = mediaAttachmentMapper.selectById(attachmentId);
        return Optional.ofNullable(entity).map(this::toRecord);
    }

    private MediaAttachmentEntity toEntity(MediaAttachmentRecord record) {
        MediaAttachmentEntity entity = new MediaAttachmentEntity();
        entity.setAttachmentId(record.attachmentId());
        entity.setSessionId(record.sessionId());
        entity.setAccountId(record.accountId());
        entity.setConversationId(record.conversationId());
        entity.setAttachmentType(record.attachmentType().name());
        entity.setFileName(record.fileName());
        entity.setContentType(record.contentType());
        entity.setContentLength(record.contentLength());
        entity.setObjectKey(record.objectKey());
        entity.setUploadUrl(record.uploadUrl());
        entity.setCreatedAt(record.createdAt());
        return entity;
    }

    private MediaAttachmentRecord toRecord(MediaAttachmentEntity entity) {
        return new MediaAttachmentRecord(
                entity.getAttachmentId(),
                entity.getSessionId(),
                entity.getAccountId(),
                entity.getConversationId(),
                MediaAttachmentType.valueOf(entity.getAttachmentType()),
                entity.getFileName(),
                entity.getContentType(),
                entity.getContentLength(),
                entity.getObjectKey(),
                entity.getUploadUrl(),
                entity.getCreatedAt()
        );
    }
}
