package com.kzzz3.argus.cortex.conversation.infrastructure;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class ConversationTimeFormatter {

	private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
	private static final DateTimeFormatter MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter STATUS_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	private ConversationTimeFormatter() {
	}

	static LocalDateTime nowLocal() {
		return LocalDateTime.now(DEFAULT_ZONE);
	}

	static OffsetDateTime nowOffset() {
		return OffsetDateTime.now(DEFAULT_ZONE);
	}

	static String formatTimestampLabel(LocalDateTime timestamp) {
		return timestamp.atZone(DEFAULT_ZONE).format(MESSAGE_TIME_FORMATTER);
	}

	static String formatStatusUpdatedAt(OffsetDateTime timestamp) {
		return timestamp.format(STATUS_TIME_FORMATTER);
	}
}
