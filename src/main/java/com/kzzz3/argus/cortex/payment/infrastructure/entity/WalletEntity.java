package com.kzzz3.argus.cortex.payment.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("wallet_account")
public class WalletEntity {

	@TableId(value = "account_id")
	private String accountId;
	private String currency;
	private BigDecimal balance;
	private LocalDateTime updatedAt;

	public String getAccountId() { return accountId; }
	public void setAccountId(String accountId) { this.accountId = accountId; }
	public String getCurrency() { return currency; }
	public void setCurrency(String currency) { this.currency = currency; }
	public BigDecimal getBalance() { return balance; }
	public void setBalance(BigDecimal balance) { this.balance = balance; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
