package com.kzzz3.argus.cortex.conversation.web;

import jakarta.validation.constraints.NotBlank;

public record MessageReceiptRequest(
		@NotBlank String receiptType
) {
}
