package com.kzzz3.argus.cortex.media.domain;

import java.util.Optional;

public interface MediaUploadSessionStore {

    MediaUploadSession save(MediaUploadSession session);

    Optional<MediaUploadSession> findBySessionId(String sessionId);
}
