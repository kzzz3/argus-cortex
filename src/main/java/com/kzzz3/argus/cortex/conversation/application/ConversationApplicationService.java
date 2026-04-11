package com.kzzz3.argus.cortex.conversation.application;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessagePage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationDetail;
import com.kzzz3.argus.cortex.conversation.domain.ConversationNotFoundException;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationSummary;
import com.kzzz3.argus.cortex.conversation.domain.MessageNotFoundException;
import com.kzzz3.argus.cortex.conversation.web.CreateConversationRequest;
import com.kzzz3.argus.cortex.conversation.web.MessageReceiptRequest;
import com.kzzz3.argus.cortex.conversation.web.SendMessageRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConversationApplicationService {

	private static final int DEFAULT_RECENT_WINDOW_DAYS = 7;
	private static final int DEFAULT_MESSAGE_LIMIT = 50;

	private final AccessTokenStore accessTokenStore;
	private final ConversationStore conversationStore;

	public ConversationApplicationService(AccessTokenStore accessTokenStore, ConversationStore conversationStore) {
		this.accessTokenStore = accessTokenStore;
		this.conversationStore = conversationStore;
	}

	public List<ConversationSummary> listConversations(String accessToken, int recentWindowDays) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		int normalizedWindowDays = normalizeRecentWindowDays(recentWindowDays);
		return conversationStore.listConversations(accountRecord, normalizedWindowDays);
	}

	public ConversationDetail getConversationDetail(String accessToken, String conversationId) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		return conversationStore.getConversationDetail(accountRecord, conversationId);
	}

	public ConversationMessagePage listMessages(
			String accessToken,
			String conversationId,
			int recentWindowDays,
			int limit,
			String sinceCursor
	) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		int normalizedWindowDays = normalizeRecentWindowDays(recentWindowDays);
		int normalizedLimit = normalizeMessageLimit(limit);
		return conversationStore.listMessages(accountRecord, conversationId, normalizedWindowDays, normalizedLimit, sinceCursor);
	}

	public ConversationMessage sendMessage(
			String accessToken,
			String conversationId,
			SendMessageRequest request
	) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		return conversationStore.sendMessage(
				accountRecord,
				conversationId,
				request.clientMessageId().trim(),
				request.body().trim()
		);
	}

	public ConversationMessage applyReceipt(
			String accessToken,
			String conversationId,
			String messageId,
			MessageReceiptRequest request
	) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		return conversationStore.applyReceipt(accountRecord, conversationId, messageId, request.receiptType().trim());
	}

	public ConversationMessage recallMessage(
			String accessToken,
			String conversationId,
			String messageId
	) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		return conversationStore.recallMessage(accountRecord, conversationId, messageId);
	}

	public ConversationSummary markConversationRead(
			String accessToken,
			String conversationId
	) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		return conversationStore.markConversationRead(accountRecord, conversationId);
	}

	public ConversationSummary createConversation(
			String accessToken,
			CreateConversationRequest request
	) {
		AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
				.orElseThrow(InvalidCredentialsException::new);
		return conversationStore.createConversation(accountRecord, request.type().trim(), request.title().trim());
	}

	private int normalizeRecentWindowDays(int requestedWindowDays) {
		return requestedWindowDays <= 0 ? DEFAULT_RECENT_WINDOW_DAYS : Math.min(requestedWindowDays, 30);
	}

	private int normalizeMessageLimit(int requestedLimit) {
		return requestedLimit <= 0 ? DEFAULT_MESSAGE_LIMIT : Math.min(requestedLimit, 200);
	}
}
