package com.kzzz3.argus.cortex.auth.application;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenIssuer;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.AccountStore;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.auth.domain.RegistrationConflictException;
import com.kzzz3.argus.cortex.auth.web.LoginRequest;
import com.kzzz3.argus.cortex.auth.web.RegisterRequest;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService {

	private final AccountStore accountStore;
	private final AccessTokenIssuer accessTokenIssuer;

	public AuthApplicationService(AccountStore accountStore, AccessTokenIssuer accessTokenIssuer) {
		this.accountStore = accountStore;
		this.accessTokenIssuer = accessTokenIssuer;
	}

	public AuthResult register(RegisterRequest request) {
		String normalizedAccount = normalizeAccount(request.account());
		if (accountStore.exists(normalizedAccount)) {
			throw new RegistrationConflictException(normalizedAccount);
		}

		AccountRecord registeredAccount = accountStore.save(
				new AccountRecord(
						normalizedAccount,
						request.displayName().trim(),
						request.password()
				)
		);

		return new AuthResult(
				registeredAccount.accountId(),
				registeredAccount.displayName(),
				accessTokenIssuer.issue(),
				"Registration succeeded. Stage-1 server session issued."
		);
	}

	public AuthResult login(LoginRequest request) {
		String normalizedAccount = normalizeAccount(request.account());
		AccountRecord accountRecord = accountStore.findByAccountId(normalizedAccount)
				.orElseThrow(InvalidCredentialsException::new);

		if (!accountRecord.password().equals(request.password())) {
			throw new InvalidCredentialsException();
		}

		return new AuthResult(
				accountRecord.accountId(),
				accountRecord.displayName(),
				accessTokenIssuer.issue(),
				"Login succeeded. Stage-1 server session issued."
		);
	}

	private String normalizeAccount(String account) {
		return account.trim().toLowerCase();
	}
}
