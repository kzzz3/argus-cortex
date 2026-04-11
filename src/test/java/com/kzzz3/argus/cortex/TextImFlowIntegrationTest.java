package com.kzzz3.argus.cortex;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
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

		mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", conversationId)
						.header("Authorization", bearer(accessToken))
						.param("recentWindowDays", "7")
						.param("limit", "50"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.messages[*].id").isArray())
				.andExpect(jsonPath("$.nextSyncCursor").isNotEmpty());

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

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}
}
