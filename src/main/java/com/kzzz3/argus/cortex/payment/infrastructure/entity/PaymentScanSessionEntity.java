package com.kzzz3.argus.cortex.payment.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("payment_scan_session")
public class PaymentScanSessionEntity {

	@TableId
	private String sessionId;
	private String payerAccountId;
	private String recipientAccountId;
	private String recipientDisplayName;
	private String currency;
	private BigDecimal requestedAmount;
	private Boolean amountEditable;
	private String requestedNote;
	private LocalDateTime createdAt;

	public String getSessionId() { return sessionId; }
	public void setSessionId(String sessionId) { this.sessionId = sessionId; }
	public String getPayerAccountId() { return payerAccountId; }
	public void setPayerAccountId(String payerAccountId) { this.payerAccountId = payerAccountId; }
	public String getRecipientAccountId() { return recipientAccountId; }
	public void setRecipientAccountId(String recipientAccountId) { this.recipientAccountId = recipientAccountId; }
	public String getRecipientDisplayName() { return recipientDisplayName; }
	public void setRecipientDisplayName(String recipientDisplayName) { this.recipientDisplayName = recipientDisplayName; }
	public String getCurrency() { return currency; }
	public void setCurrency(String currency) { this.currency = currency; }
	public BigDecimal getRequestedAmount() { return requestedAmount; }
	public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
	public Boolean getAmountEditable() { return amountEditable; }
	public void setAmountEditable(Boolean amountEditable) { this.amountEditable = amountEditable; }
	public String getRequestedNote() { return requestedNote; }
	public void setRequestedNote(String requestedNote) { this.requestedNote = requestedNote; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
