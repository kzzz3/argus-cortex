package com.kzzz3.argus.cortex.conversation.application;

public record ApplyMessageReceiptCommand(
		String receiptType
) {
	public ApplyMessageReceiptCommand {
		if (receiptType == null || receiptType.isBlank()) {
			throw new IllegalArgumentException("Receipt type is required.");
		}

		String normalizedReceiptType = receiptType.trim().toUpperCase();
		if (!normalizedReceiptType.equals("DELIVERED") && !normalizedReceiptType.equals("READ")) {
			throw new IllegalArgumentException("Receipt type must be DELIVERED or READ.");
		}

		receiptType = normalizedReceiptType;
	}
}
