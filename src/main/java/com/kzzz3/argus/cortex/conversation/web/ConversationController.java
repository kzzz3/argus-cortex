package com.kzzz3.argus.cortex.conversation.web;

import com.kzzz3.argus.cortex.conversation.application.ConversationApplicationService;
import com.kzzz3.argus.cortex.conversation.application.ApplyMessageReceiptCommand;
import com.kzzz3.argus.cortex.conversation.application.SendMessageCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

	private final ConversationApplicationService conversationApplicationService;

	public ConversationController(ConversationApplicationService conversationApplicationService) {
		this.conversationApplicationService = conversationApplicationService;
	}

	@GetMapping
	public List<ConversationSummaryResponse> listConversations(
			@RequestParam(name = "recentWindowDays", defaultValue = "7") int recentWindowDays
	) {
		return conversationApplicationService.listConversations(recentWindowDays)
				.stream()
				.map(ConversationSummaryResponse::from)
				.toList();
	}

	@GetMapping("/{conversationId}")
	public ConversationDetailResponse getConversationDetail(
			@PathVariable String conversationId
	) {
		return ConversationDetailResponse.from(conversationApplicationService.getConversationDetail(conversationId));
	}

	@GetMapping("/{conversationId}/messages")
	public ConversationMessagePageResponse listMessages(
			@PathVariable String conversationId,
			@RequestParam(name = "recentWindowDays", defaultValue = "7") int recentWindowDays,
			@RequestParam(name = "limit", defaultValue = "50") int limit,
			@RequestParam(name = "sinceCursor", required = false) String sinceCursor
	) {
		var page = conversationApplicationService.listMessages(
				conversationId,
				recentWindowDays,
				limit,
				sinceCursor
		);

		return new ConversationMessagePageResponse(
				page.messages().stream().map(ConversationMessageResponse::from).toList(),
				page.nextSyncCursor(),
				page.recentWindowDays(),
				page.limit()
		);
	}

	@PostMapping("/{conversationId}/messages")
	public ConversationMessageResponse sendMessage(
			@PathVariable String conversationId,
			@Valid @RequestBody SendMessageRequest request
	) {
		return ConversationMessageResponse.from(
				conversationApplicationService.sendMessage(
						conversationId,
						new SendMessageCommand(
								request.clientMessageId(),
								request.body(),
								request.attachment() == null ? null : request.attachment().attachmentId()
						)
				)
		);
	}

	@PostMapping("/{conversationId}/messages/{messageId}/receipt")
	public ConversationMessageResponse applyReceipt(
			@PathVariable String conversationId,
			@PathVariable String messageId,
			@Valid @RequestBody MessageReceiptRequest request
	) {
		return ConversationMessageResponse.from(
				conversationApplicationService.applyReceipt(
						conversationId,
						messageId,
						new ApplyMessageReceiptCommand(request.receiptType())
				)
		);
	}

	@PostMapping("/{conversationId}/messages/{messageId}/recall")
	public ConversationMessageResponse recallMessage(
			@PathVariable String conversationId,
			@PathVariable String messageId,
			@RequestBody(required = false) RecallMessageRequest request
	) {
		return ConversationMessageResponse.from(
				conversationApplicationService.recallMessage(
						conversationId,
						messageId
				)
		);
	}

	@PostMapping("/{conversationId}/read")
	public ConversationSummaryResponse markConversationRead(
			@PathVariable String conversationId,
			@RequestBody(required = false) MarkConversationReadRequest request
	) {
		return ConversationSummaryResponse.from(conversationApplicationService.markConversationRead(conversationId));
	}
}
