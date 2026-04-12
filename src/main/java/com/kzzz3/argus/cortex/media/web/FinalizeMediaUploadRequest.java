package com.kzzz3.argus.cortex.media.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record FinalizeMediaUploadRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @PositiveOrZero long contentLength,
        @NotBlank String objectKey,
        String conversationId
) {
}
