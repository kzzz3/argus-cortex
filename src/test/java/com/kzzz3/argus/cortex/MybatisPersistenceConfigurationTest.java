package com.kzzz3.argus.cortex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.kzzz3.argus.cortex.auth.application.AuthApplicationService;
import com.kzzz3.argus.cortex.auth.application.AuthResult;
import com.kzzz3.argus.cortex.auth.application.LoginCommand;
import com.kzzz3.argus.cortex.auth.application.RegisterCommand;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.auth.domain.RefreshSessionStore;
import com.kzzz3.argus.cortex.auth.infrastructure.MybatisAccountStore;
import com.kzzz3.argus.cortex.auth.infrastructure.MybatisRefreshSessionStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.conversation.infrastructure.MybatisConversationStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"argus.persistence.mode=mysql",
		"argus.auth.jwt.secret=test-jwt-secret-for-mybatis-path-1234567890"
})
class MybatisPersistenceConfigurationTest {

	@Autowired
	private AccountStore accountStore;

	@Autowired
	private RefreshSessionStore refreshSessionStore;

	@Autowired
	private ConversationStore conversationStore;

	@Autowired
	private AuthApplicationService authApplicationService;

	@Test
	void bootsMybatisBackedStores() {
		assertInstanceOf(MybatisAccountStore.class, accountStore);
		assertInstanceOf(MybatisRefreshSessionStore.class, refreshSessionStore);
		assertInstanceOf(MybatisConversationStore.class, conversationStore);
	}

	@Test
	void registerAndLoginWorkAgainstMybatisAccountStore() {
		AuthResult registerResult = authApplicationService.register(new RegisterCommand("Argus Tester", "mybatis-tester", "secret123"));
		AuthResult loginResult = authApplicationService.login(new LoginCommand("mybatis-tester", "secret123"));

		assertEquals("mybatis-tester", registerResult.accountId());
		assertEquals("mybatis-tester", loginResult.accountId());
		assertNotNull(loginResult.accessToken());
		assertNotNull(loginResult.refreshToken());
	}
}
