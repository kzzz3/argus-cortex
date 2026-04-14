package com.kzzz3.argus.cortex.media.application;

import org.springframework.lang.Nullable;

public record FinalizeMediaUploadCommand(
		String fileName,
		String contentType,
		long contentLength,
		String objectKey,
		@Nullable String conversationId
) {
}
