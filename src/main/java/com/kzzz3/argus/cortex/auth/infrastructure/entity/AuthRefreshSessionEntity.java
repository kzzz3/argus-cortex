package com.kzzz3.argus.cortex.auth.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("auth_refresh_session")
public class AuthRefreshSessionEntity {
	@TableId(value = "session_id")
	private String sessionId;
	private String accountId;
	private String displayName;
	private String refreshTokenHash;
	private LocalDateTime expiresAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime revokedAt;

	public String getSessionId() { return sessionId; }
	public void setSessionId(String sessionId) { this.sessionId = sessionId; }
	public String getAccountId() { return accountId; }
	public void setAccountId(String accountId) { this.accountId = accountId; }
	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }
	public String getRefreshTokenHash() { return refreshTokenHash; }
	public void setRefreshTokenHash(String refreshTokenHash) { this.refreshTokenHash = refreshTokenHash; }
	public LocalDateTime getExpiresAt() { return expiresAt; }
	public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
	public LocalDateTime getRevokedAt() { return revokedAt; }
	public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}
