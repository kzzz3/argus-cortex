package com.kzzz3.argus.cortex.auth.application;

import com.kzzz3.argus.cortex.auth.config.JwtSecretKeyProvider;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final Duration accessTokenTtl;
	private final Duration refreshTokenTtl;
	private final JwtDecoder refreshTokenDecoder;

	public JwtTokenService(
			JwtEncoder jwtEncoder,
			@org.springframework.beans.factory.annotation.Value("${argus.auth.jwt.ttl:PT2H}") String accessTokenTtl,
			@org.springframework.beans.factory.annotation.Value("${argus.auth.jwt.refresh-ttl:PT168H}") String refreshTokenTtl,
			JwtSecretKeyProvider jwtSecretKeyProvider
	) {
		this.jwtEncoder = jwtEncoder;
		this.accessTokenTtl = Duration.parse(accessTokenTtl);
		this.refreshTokenTtl = Duration.parse(refreshTokenTtl);
		this.refreshTokenDecoder = NimbusJwtDecoder.withSecretKey(jwtSecretKeyProvider.secretKey())
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	public String issueAccessToken(AccountRecord accountRecord) {
		return issue(accountRecord, accessTokenTtl, "access");
	}

	public String issueRefreshToken(AccountRecord accountRecord) {
		return issue(accountRecord, refreshTokenTtl, "refresh");
	}

	public Jwt decodeRefreshToken(String refreshAccessToken) {
		Jwt jwt = refreshTokenDecoder.decode(refreshAccessToken);
		if (!"refresh".equals(jwt.getClaimAsString("token_type"))) {
			throw new JwtException("Refresh token required");
		}
		return jwt;
	}

	private String issue(AccountRecord accountRecord, Duration ttl, String tokenType) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.id(UUID.randomUUID().toString())
				.subject(accountRecord.accountId())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(ttl))
				.claim("displayName", accountRecord.displayName())
				.claim("token_type", tokenType)
				.build();
		return jwtEncoder.encode(
				JwtEncoderParameters.from(
						JwsHeader.with(MacAlgorithm.HS256).build(),
						claims
				)
		).getTokenValue();
	}
}
