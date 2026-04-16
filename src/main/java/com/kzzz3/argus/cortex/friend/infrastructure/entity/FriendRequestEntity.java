package com.kzzz3.argus.cortex.friend.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("friend_request")
public class FriendRequestEntity {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String requestId;
	private String requesterAccountId;
	private String requesterDisplayName;
	private String targetAccountId;
	private String targetDisplayName;
	private String note;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime respondedAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getRequestId() { return requestId; }
	public void setRequestId(String requestId) { this.requestId = requestId; }
	public String getRequesterAccountId() { return requesterAccountId; }
	public void setRequesterAccountId(String requesterAccountId) { this.requesterAccountId = requesterAccountId; }
	public String getRequesterDisplayName() { return requesterDisplayName; }
	public void setRequesterDisplayName(String requesterDisplayName) { this.requesterDisplayName = requesterDisplayName; }
	public String getTargetAccountId() { return targetAccountId; }
	public void setTargetAccountId(String targetAccountId) { this.targetAccountId = targetAccountId; }
	public String getTargetDisplayName() { return targetDisplayName; }
	public void setTargetDisplayName(String targetDisplayName) { this.targetDisplayName = targetDisplayName; }
	public String getNote() { return note; }
	public void setNote(String note) { this.note = note; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getRespondedAt() { return respondedAt; }
	public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
