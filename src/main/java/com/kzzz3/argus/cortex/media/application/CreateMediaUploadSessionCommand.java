package com.kzzz3.argus.cortex.media.application;

import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;

public record CreateMediaUploadSessionCommand(
		MediaAttachmentType attachmentType,
		String fileName,
		long estimatedBytes
) {
}
