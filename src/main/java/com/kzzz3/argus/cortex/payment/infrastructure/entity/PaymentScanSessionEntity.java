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
	private String merchantAccountId;
	private String merchantDisplayName;
	private String currency;
	private BigDecimal suggestedAmount;
	private Boolean amountEditable;
	private String suggestedNote;
	private LocalDateTime createdAt;

	public String getSessionId() { return sessionId; }
	public void setSessionId(String sessionId) { this.sessionId = sessionId; }
	public String getPayerAccountId() { return payerAccountId; }
	public void setPayerAccountId(String payerAccountId) { this.payerAccountId = payerAccountId; }
	public String getMerchantAccountId() { return merchantAccountId; }
	public void setMerchantAccountId(String merchantAccountId) { this.merchantAccountId = merchantAccountId; }
	public String getMerchantDisplayName() { return merchantDisplayName; }
	public void setMerchantDisplayName(String merchantDisplayName) { this.merchantDisplayName = merchantDisplayName; }
	public String getCurrency() { return currency; }
	public void setCurrency(String currency) { this.currency = currency; }
	public BigDecimal getSuggestedAmount() { return suggestedAmount; }
	public void setSuggestedAmount(BigDecimal suggestedAmount) { this.suggestedAmount = suggestedAmount; }
	public Boolean getAmountEditable() { return amountEditable; }
	public void setAmountEditable(Boolean amountEditable) { this.amountEditable = amountEditable; }
	public String getSuggestedNote() { return suggestedNote; }
	public void setSuggestedNote(String suggestedNote) { this.suggestedNote = suggestedNote; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
