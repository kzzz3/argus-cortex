package com.kzzz3.argus.cortex.conversation.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("conversation_message")
public class ConversationMessageEntity {

	@TableId
	private String id;
	private String clientMessageId;
	private String conversationId;
	private String senderAccountId;
	private String senderDisplayName;
	private String body;
	private String attachmentId;
	private String timestampLabel;
	private String deliveryStatus;
	private String statusUpdatedAt;
	private Long sequenceNo;
	private LocalDateTime createdAt;

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public String getClientMessageId() { return clientMessageId; }
	public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; }
	public String getConversationId() { return conversationId; }
	public void setConversationId(String conversationId) { this.conversationId = conversationId; }
	public String getSenderAccountId() { return senderAccountId; }
	public void setSenderAccountId(String senderAccountId) { this.senderAccountId = senderAccountId; }
	public String getSenderDisplayName() { return senderDisplayName; }
	public void setSenderDisplayName(String senderDisplayName) { this.senderDisplayName = senderDisplayName; }
	public String getBody() { return body; }
	public void setBody(String body) { this.body = body; }
	public String getAttachmentId() { return attachmentId; }
	public void setAttachmentId(String attachmentId) { this.attachmentId = attachmentId; }
	public String getTimestampLabel() { return timestampLabel; }
	public void setTimestampLabel(String timestampLabel) { this.timestampLabel = timestampLabel; }
	public String getDeliveryStatus() { return deliveryStatus; }
	public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
	public String getStatusUpdatedAt() { return statusUpdatedAt; }
	public void setStatusUpdatedAt(String statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }
	public Long getSequenceNo() { return sequenceNo; }
	public void setSequenceNo(Long sequenceNo) { this.sequenceNo = sequenceNo; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
