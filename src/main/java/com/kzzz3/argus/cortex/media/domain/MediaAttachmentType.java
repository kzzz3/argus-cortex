package com.kzzz3.argus.cortex.media.domain;

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
}
