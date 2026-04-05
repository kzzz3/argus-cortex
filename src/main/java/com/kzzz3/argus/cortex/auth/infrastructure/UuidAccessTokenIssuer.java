package com.kzzz3.argus.cortex.auth.infrastructure;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenIssuer;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidAccessTokenIssuer implements AccessTokenIssuer {

	@Override
	public String issue() {
		return "argus-" + UUID.randomUUID();
	}
}
