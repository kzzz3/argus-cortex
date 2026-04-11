package com.kzzz3.argus.cortex.auth.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("auth_session")
public class AuthSessionEntity {

	@TableId(value = "access_token")
	private String accessToken;
	private String accountId;
	private String displayName;
	private LocalDateTime createdAt;

	public String getAccessToken() { return accessToken; }
	public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
	public String getAccountId() { return accountId; }
	public void setAccountId(String accountId) { this.accountId = accountId; }
	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
