package com.kzzz3.argus.cortex.conversation.infrastructure;

final class ConversationSyncCursor {

    private final String conversationId;
    private final long sequence;
    private final long revision;

    private ConversationSyncCursor(String conversationId, long sequence, long revision) {
        this.conversationId = conversationId;
        this.sequence = Math.max(sequence, 0L);
        this.revision = Math.max(revision, 0L);
    }

    static ConversationSyncCursor of(String conversationId, long sequence, long revision) {
        return new ConversationSyncCursor(conversationId, sequence, revision);
    }

    static ConversationSyncCursor parse(String expectedConversationId, String rawCursor) {
        if (rawCursor == null || rawCursor.isBlank()) {
            return null;
        }

        String trimmed = rawCursor.trim();
        String prefix = "cursor:" + expectedConversationId + ":";
        if (trimmed.startsWith(prefix)) {
            String[] parts = trimmed.substring(prefix.length()).split(":", 2);
            if (parts.length == 2) {
                Long sequence = parseLong(parts[0]);
                Long revision = parseLong(parts[1]);
                if (sequence != null && revision != null) {
                    return new ConversationSyncCursor(expectedConversationId, sequence, revision);
                }
            }
        }

        String legacyPrefix = "cursor-" + expectedConversationId + "-";
        if (trimmed.startsWith(legacyPrefix)) {
            Long legacySequence = parseLong(trimmed.substring(legacyPrefix.length()));
            if (legacySequence != null) {
                return new ConversationSyncCursor(expectedConversationId, legacySequence, legacySequence);
            }
        }

        return null;
    }

    String encoded() {
        return "cursor:" + conversationId + ":" + sequence + ":" + revision;
    }

    long sequence() {
        return sequence;
    }

    long revision() {
        return revision;
    }

    long nextRevision() {
        return revision + 1L;
    }

    private static Long parseLong(String rawValue) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
