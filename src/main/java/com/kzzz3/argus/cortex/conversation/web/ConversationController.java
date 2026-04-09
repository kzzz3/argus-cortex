package com.kzzz3.argus.cortex.conversation.web;

import com.kzzz3.argus.cortex.conversation.application.ConversationApplicationService;
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
				extractBearerToken(authorizationHeader),
				recentWindowDays
		)
				.stream()
				.map(ConversationSummaryResponse::from)
				.toList();
	}

	@GetMapping("/{conversationId}/messages")
	public List<ConversationMessageResponse> listMessages(
			@PathVariable String conversationId,
			@RequestParam(name = "recentWindowDays", defaultValue = "7") int recentWindowDays,
			@RequestParam(name = "limit", defaultValue = "50") int limit,
			@RequestHeader("Authorization") String authorizationHeader
	) {
		return conversationApplicationService.listMessages(
				extractBearerToken(authorizationHeader),
				conversationId,
				recentWindowDays,
				limit
		)
				.stream()
				.map(ConversationMessageResponse::from)
				.toList();
	}

	@PostMapping("/{conversationId}/messages")
	public ConversationMessageResponse sendMessage(
			@PathVariable String conversationId,
			@RequestHeader("Authorization") String authorizationHeader,
			@Valid @RequestBody SendMessageRequest request
	) {
		return ConversationMessageResponse.from(
				conversationApplicationService.sendMessage(
						extractBearerToken(authorizationHeader),
						conversationId,
						request
				)
		);
	}

	private String extractBearerToken(String authorizationHeader) {
		if (authorizationHeader == null) {
			throw new IllegalArgumentException("Missing Authorization header.");
		}

		String prefix = "Bearer ";
		if (!authorizationHeader.startsWith(prefix)) {
			throw new IllegalArgumentException("Authorization header must use Bearer token.");
		}

		return authorizationHeader.substring(prefix.length()).trim();
	}
}
