package com.kzzz3.argus.cortex.auth.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("account")
public class AccountEntity {

	@TableId(value = "account_id")
	private String accountId;
	private String displayName;
	private String password;
	private LocalDateTime createdAt;

	public String getAccountId() { return accountId; }
	public void setAccountId(String accountId) { this.accountId = accountId; }
	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }
	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
