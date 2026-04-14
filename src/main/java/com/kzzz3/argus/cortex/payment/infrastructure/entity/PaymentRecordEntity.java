package com.kzzz3.argus.cortex.payment.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("payment_record")
public class PaymentRecordEntity {

	@TableId
	private String paymentId;
	private String sessionId;
	private String payerAccountId;
	private String merchantAccountId;
	private String merchantDisplayName;
	private String conversationId;
	private BigDecimal amount;
	private String currency;
	private String note;
	private String status;
	private LocalDateTime createdAt;

	public String getPaymentId() { return paymentId; }
	public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
	public String getSessionId() { return sessionId; }
	public void setSessionId(String sessionId) { this.sessionId = sessionId; }
	public String getPayerAccountId() { return payerAccountId; }
	public void setPayerAccountId(String payerAccountId) { this.payerAccountId = payerAccountId; }
	public String getMerchantAccountId() { return merchantAccountId; }
	public void setMerchantAccountId(String merchantAccountId) { this.merchantAccountId = merchantAccountId; }
	public String getMerchantDisplayName() { return merchantDisplayName; }
	public void setMerchantDisplayName(String merchantDisplayName) { this.merchantDisplayName = merchantDisplayName; }
	public String getConversationId() { return conversationId; }
	public void setConversationId(String conversationId) { this.conversationId = conversationId; }
	public BigDecimal getAmount() { return amount; }
	public void setAmount(BigDecimal amount) { this.amount = amount; }
	public String getCurrency() { return currency; }
	public void setCurrency(String currency) { this.currency = currency; }
	public String getNote() { return note; }
	public void setNote(String note) { this.note = note; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
