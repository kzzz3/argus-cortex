package com.kzzz3.argus.cortex.media.infrastructure;

import com.kzzz3.argus.cortex.media.domain.MediaAttachmentRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentStore;
import com.kzzz3.argus.cortex.media.infrastructure.entity.MediaAttachmentEntity;
import com.kzzz3.argus.cortex.media.infrastructure.mapper.MediaAttachmentMapper;
import org.springframework.stereotype.Component;

@Component
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
}
