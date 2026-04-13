package com.kzzz3.argus.cortex;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class TextImFlowIntegrationTest {

	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private DataSource dataSource;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS media_attachment (
				    attachment_id VARCHAR(128) PRIMARY KEY,
				    session_id VARCHAR(128) NOT NULL,
				    account_id VARCHAR(64) NOT NULL,
				    conversation_id VARCHAR(64),
				    attachment_type VARCHAR(32) NOT NULL,
				    file_name VARCHAR(255) NOT NULL,
				    content_type VARCHAR(128) NOT NULL,
				    content_length BIGINT NOT NULL,
				    object_key VARCHAR(255) NOT NULL,
				    upload_url VARCHAR(1024) NOT NULL,
				    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
				    CONSTRAINT uk_media_attachment_session UNIQUE (session_id)
				)
				""");
	}

	@Test
	void textImHappyPathWorksEndToEnd() throws Exception {
		register("Zhang San", "zhangsan", "secret123");
		register("Li Si", "lisi", "secret123");
		String accessToken = registerAndReadAccessToken("Argus Tester", "tester", "secret123");

		mockMvc.perform(post("/api/v1/friends")
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"friendAccountId":"lisi"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountId").value("lisi"));

		mockMvc.perform(get("/api/v1/friends")
						.header("Authorization", bearer(accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		MvcResult conversationsResult = mockMvc.perform(get("/api/v1/conversations")
						.header("Authorization", bearer(accessToken))
						.param("recentWindowDays", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id=='conv-direct-lisi-tester')]").exists())
				.andReturn();

		String conversationId = "conv-direct-lisi-tester";
		JsonNode conversationsBody = objectMapper.readTree(conversationsResult.getResponse().getContentAsString());
		String initialSyncCursor = findConversationSyncCursor(conversationsBody, conversationId);

		MvcResult sendFirstResult = mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientMessageId":"client-msg-1","body":"Hello Zhang San"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.conversationId").value(conversationId))
				.andExpect(jsonPath("$.body").value("Hello Zhang San"))
				.andReturn();

		JsonNode sendFirstBody = objectMapper.readTree(sendFirstResult.getResponse().getContentAsString());
		String firstMessageId = sendFirstBody.get("id").asText();

		mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientMessageId":"client-msg-1","body":"Hello Zhang San"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(firstMessageId));

		mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientMessageId":"client-msg-2","body":"Follow-up 1"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body").value("Follow-up 1"));

		mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientMessageId":"client-msg-3","body":"Follow-up 2"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body").value("Follow-up 2"));

		MvcResult continuationPageOne = mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.param("recentWindowDays", "7")
						.param("limit", "2")
						.param("sinceCursor", initialSyncCursor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.messages", hasSize(2)))
				.andExpect(jsonPath("$.messages[0].body").value("Hello Zhang San"))
				.andExpect(jsonPath("$.messages[1].body").value("Follow-up 1"))
				.andReturn();

		String continuationCursor = objectMapper.readTree(continuationPageOne.getResponse().getContentAsString())
				.get("nextSyncCursor")
				.asText();

		MvcResult continuationPageTwo = mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.param("recentWindowDays", "7")
						.param("limit", "2")
						.param("sinceCursor", continuationCursor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.messages", hasSize(1)))
				.andExpect(jsonPath("$.messages[0].body").value("Follow-up 2"))
				.andReturn();

		String latestContinuationCursor = objectMapper.readTree(continuationPageTwo.getResponse().getContentAsString())
				.get("nextSyncCursor")
				.asText();

		mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.param("recentWindowDays", "7")
						.param("limit", "2")
						.param("sinceCursor", latestContinuationCursor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.messages", hasSize(0)));

		mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.param("recentWindowDays", "7")
						.param("limit", "50"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.messages[*].id").isArray())
				.andExpect(jsonPath("$.nextSyncCursor").isNotEmpty());

		MvcResult uploadSessionResult = mockMvc.perform(post("/api/v1/media/upload-sessions")
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"attachmentType":"IMAGE","fileName":"design-spec.png","estimatedBytes":12}
								""".formatted(conversationId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").isNotEmpty())
				.andReturn();

		JsonNode uploadSessionBody = objectMapper.readTree(uploadSessionResult.getResponse().getContentAsString());
		String uploadSessionId = uploadSessionBody.get("sessionId").asText();
		String uploadObjectKey = uploadSessionBody.get("objectKey").asText();

		mockMvc.perform(put("/api/v1/media/upload-sessions/{sessionId}/content", uploadSessionId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_OCTET_STREAM)
						.content("hello-image".getBytes()))
				.andExpect(status().isOk());

		MvcResult finalizeAttachmentResult = mockMvc.perform(post("/api/v1/media/upload-sessions/{sessionId}/finalize", uploadSessionId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fileName":"design-spec.png","contentType":"image/png","contentLength":11,"objectKey":"%s","conversationId":"%s"}
								""".formatted(uploadObjectKey, conversationId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.attachmentId").isNotEmpty())
				.andReturn();

		String attachmentId = objectMapper.readTree(finalizeAttachmentResult.getResponse().getContentAsString()).get("attachmentId").asText();

		MvcResult attachmentMessageResult = mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientMessageId":"client-msg-attachment","attachment":{"attachmentId":"%s"}}
								""".formatted(attachmentId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.attachment.attachmentId").value(attachmentId))
				.andExpect(jsonPath("$.attachment.fileName").value("design-spec.png"))
				.andReturn();

		String attachmentMessageId = objectMapper.readTree(attachmentMessageResult.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientMessageId":"client-msg-attachment","attachment":{"attachmentId":"%s"}}
								""".formatted(attachmentId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(attachmentMessageId));

		MvcResult attachmentMessagesResult = mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.param("recentWindowDays", "7")
						.param("limit", "50"))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode attachmentMessagesPage = objectMapper.readTree(attachmentMessagesResult.getResponse().getContentAsString());
		String attachmentSyncCursor = attachmentMessagesPage.get("nextSyncCursor").asText();
		JsonNode attachmentMessagesBody = attachmentMessagesPage.get("messages");
		JsonNode attachmentMessageNode = null;
		for (JsonNode messageNode : attachmentMessagesBody) {
			if (attachmentMessageId.equals(messageNode.get("id").asText())) {
				attachmentMessageNode = messageNode;
				break;
			}
		}
		org.junit.jupiter.api.Assertions.assertNotNull(attachmentMessageNode);
		org.junit.jupiter.api.Assertions.assertEquals(attachmentId, attachmentMessageNode.get("attachment").get("attachmentId").asText());

		mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages/{messageId}/receipt", conversationId, firstMessageId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"receiptType":"DELIVERED"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deliveryStatus").value("DELIVERED"));

		mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages/{messageId}/receipt", conversationId, firstMessageId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"receiptType":"READ"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deliveryStatus").value("READ"));

		mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.param("recentWindowDays", "7")
						.param("limit", "50")
						.param("sinceCursor", attachmentSyncCursor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.messages", hasSize(0)))
				.andExpect(jsonPath("$.nextSyncCursor").value(org.hamcrest.Matchers.startsWith("cursor:conv-direct-lisi-tester:4:")));

		mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages/{messageId}/recall", conversationId, firstMessageId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deliveryStatus").value("RECALLED"))
				.andExpect(jsonPath("$.body").value("You recalled a message"));

		mockMvc.perform(post("/api/v1/conversations/{conversationId}/read", conversationId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unreadCount").value(0));

		MvcResult groupCreateResult = mockMvc.perform(post("/api/v1/conversations")
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type":"GROUP","title":"Backend Group"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Backend Group"))
				.andExpect(jsonPath("$.subtitle").value("Remote group conversation"))
				.andReturn();

		String groupConversationId = objectMapper.readTree(groupCreateResult.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(post("/api/v1/conversations/{conversationId}/members", groupConversationId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"memberAccountId":"zhangsan"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberCount").value(2));

		mockMvc.perform(post("/api/v1/conversations/{conversationId}/members", groupConversationId)
						.header("Authorization", bearer(accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"memberAccountId":"lisi"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberCount").value(3));

		mockMvc.perform(get("/api/v1/conversations/{conversationId}", groupConversationId)
						.header("Authorization", bearer(accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Backend Group"))
				.andExpect(jsonPath("$.memberCount").value(3))
				.andExpect(jsonPath("$.memberDisplayNames", hasSize(3)));
	}

	private void register(String displayName, String accountId, String password) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"%s","account":"%s","password":"%s"}
								""".formatted(displayName, accountId, password)))
				.andExpect(status().isOk());
	}

	private String registerAndReadAccessToken(String displayName, String accountId, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"%s","account":"%s","password":"%s"}
								""".formatted(displayName, accountId, password)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
	}

	private String findConversationSyncCursor(JsonNode conversationsBody, String conversationId) {
		for (JsonNode conversation : conversationsBody) {
			if (conversationId.equals(conversation.get("id").asText())) {
				return conversation.get("syncCursor").asText();
			}
		}
		throw new IllegalStateException("Conversation not found: " + conversationId);
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}
}
