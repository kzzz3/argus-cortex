package com.kzzz3.argus.cortex.media.web;

import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateMediaUploadSessionRequest(
        @NotNull MediaAttachmentType attachmentType,
        @NotBlank String fileName,
        @PositiveOrZero long estimatedBytes
) {
}
