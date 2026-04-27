package com.kzzz3.argus.cortex.media.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MediaAttachmentType {

    IMAGE(5L * 1024 * 1024),
    VIDEO(50L * 1024 * 1024),
    VOICE(2L * 1024 * 1024);

    private final long maxPayloadBytes;

    MediaAttachmentType(long maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public long defaultMaxPayloadBytes() {
        return maxPayloadBytes;
    }

    @JsonCreator
    public static MediaAttachmentType fromJson(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Media attachment type is required.");
        }
        return MediaAttachmentType.valueOf(value.trim().toUpperCase());
    }
}
