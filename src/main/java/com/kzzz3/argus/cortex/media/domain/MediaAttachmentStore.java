package com.kzzz3.argus.cortex.media.domain;

import java.util.Optional;

public interface MediaAttachmentStore {

    MediaAttachmentRecord save(MediaAttachmentRecord record);

    Optional<MediaAttachmentRecord> findByAttachmentId(String attachmentId);
}
