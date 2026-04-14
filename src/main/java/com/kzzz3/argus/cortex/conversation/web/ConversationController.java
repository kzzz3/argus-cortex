package com.kzzz3.argus.cortex.conversation.web;

import com.kzzz3.argus.cortex.conversation.application.ConversationApplicationService;
import com.kzzz3.argus.cortex.conversation.application.AddConversationMemberCommand;
import com.kzzz3.argus.cortex.conversation.application.ApplyMessageReceiptCommand;
import com.kzzz3.argus.cortex.conversation.application.CreateConversationCommand;
import com.kzzz3.argus.cortex.conversation.application.SendMessageCommand;
import com.kzzz3.argus.cortex.shared.web.BearerTokenExtractor;
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
			@RequestParam(name = "recentWindowDays", defaultValue = "7") int recentWindowDays,
			@RequestHeader("Authorization") String authorizationHeader
	) {
		return conversationApplicationService.listConversations(
				BearerTokenExtractor.extract(authorizationHeader),
				recentWindowDays
		)
				.stream()
				.map(ConversationSummaryResponse::from)
				.toList();
	}

	@PostMapping
	public ConversationSummaryResponse createConversation(
			@RequestHeader("Authorization") String authorizationHeader,
			@Valid @RequestBody CreateConversationRequest request
	) {
		return ConversationSummaryResponse.from(
				conversationApplicationService.createConversation(
						BearerTokenExtractor.extract(authorizationHeader),
						new CreateConversationCommand(request.type(), request.title())
				)
		);
	}

	@GetMapping("/{conversationId}")
	public ConversationDetailResponse getConversationDetail(
			@PathVariable String conversationId,
			@RequestHeader("Authorization") String authorizationHeader
	) {
		return ConversationDetailResponse.from(
				conversationApplicationService.getConversationDetail(
						BearerTokenExtractor.extract(authorizationHeader),
						conversationId
				)
		);
	}

	@PostMapping("/{conversationId}/members")
	public ConversationDetailResponse addMember(
			@PathVariable String conversationId,
			@RequestHeader("Authorization") String authorizationHeader,
			@Valid @RequestBody AddConversationMemberRequest request
	) {
		return ConversationDetailResponse.from(
				conversationApplicationService.addMember(
						BearerTokenExtractor.extract(authorizationHeader),
						conversationId,
						new AddConversationMemberCommand(request.memberAccountId())
				)
		);
	}

	@GetMapping("/{conversationId}/messages")
	public ConversationMessagePageResponse listMessages(
			@PathVariable String conversationId,
			@RequestParam(name = "recentWindowDays", defaultValue = "7") int recentWindowDays,
			@RequestParam(name = "limit", defaultValue = "50") int limit,
			@RequestParam(name = "sinceCursor", required = false) String sinceCursor,
			@RequestHeader("Authorization") String authorizationHeader
	) {
		var page = conversationApplicationService.listMessages(
				BearerTokenExtractor.extract(authorizationHeader),
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
			@RequestHeader("Authorization") String authorizationHeader,
			@Valid @RequestBody SendMessageRequest request
	) {
		return ConversationMessageResponse.from(
				conversationApplicationService.sendMessage(
						BearerTokenExtractor.extract(authorizationHeader),
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
			@RequestHeader("Authorization") String authorizationHeader,
			@Valid @RequestBody MessageReceiptRequest request
	) {
		return ConversationMessageResponse.from(
				conversationApplicationService.applyReceipt(
						BearerTokenExtractor.extract(authorizationHeader),
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
			@RequestHeader("Authorization") String authorizationHeader,
			@RequestBody(required = false) RecallMessageRequest request
	) {
		return ConversationMessageResponse.from(
				conversationApplicationService.recallMessage(
						BearerTokenExtractor.extract(authorizationHeader),
						conversationId,
						messageId
				)
		);
	}

	@PostMapping("/{conversationId}/read")
	public ConversationSummaryResponse markConversationRead(
			@PathVariable String conversationId,
			@RequestHeader("Authorization") String authorizationHeader,
			@RequestBody(required = false) MarkConversationReadRequest request
	) {
		return ConversationSummaryResponse.from(
				conversationApplicationService.markConversationRead(
						BearerTokenExtractor.extract(authorizationHeader),
						conversationId
				)
		);
	}
}
