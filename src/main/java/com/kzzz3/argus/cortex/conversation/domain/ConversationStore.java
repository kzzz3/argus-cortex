package com.kzzz3.argus.cortex.conversation.domain;

import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface ConversationStore {

	List<ConversationSummary> listConversations(AccountRecord accountRecord, int recentWindowDays);

	ConversationDetail getConversationDetail(AccountRecord accountRecord, String conversationId);

	ConversationMessagePage listMessages(
			AccountRecord accountRecord,
			String conversationId,
			int recentWindowDays,
			int limit,
			String sinceCursor
	);

	ConversationMessage sendMessage(
			AccountRecord accountRecord,
			String conversationId,
			String clientMessageId,
			String body,
			@Nullable ConversationMessageAttachment attachment
	);

	ConversationMessage applyReceipt(AccountRecord accountRecord, String conversationId, String messageId, String receiptType);

	ConversationMessage recallMessage(AccountRecord accountRecord, String conversationId, String messageId);

	ConversationSummary markConversationRead(AccountRecord accountRecord, String conversationId);

	ConversationSummary ensureDirectConversation(AccountRecord owner, AccountRecord friend);

	ConversationSummary createConversation(AccountRecord owner, String type, String title);

	ConversationDetail addMember(AccountRecord owner, String conversationId, AccountRecord member);
}
