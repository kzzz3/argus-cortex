package com.kzzz3.argus.cortex.conversation.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MessageReceiptRequest(
		@NotBlank
		@Pattern(regexp = "(?i)DELIVERED|READ", message = "receiptType must be DELIVERED or READ")
		String receiptType
) {
}
