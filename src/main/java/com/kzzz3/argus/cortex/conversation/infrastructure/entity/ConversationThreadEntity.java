package com.kzzz3.argus.cortex.conversation.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("conversation_thread")
public class ConversationThreadEntity {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String ownerAccountId;
	private String conversationId;
	private String title;
	private String subtitle;
	private Integer unreadCount;
	private String syncCursor;
	private LocalDateTime updatedAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getOwnerAccountId() { return ownerAccountId; }
	public void setOwnerAccountId(String ownerAccountId) { this.ownerAccountId = ownerAccountId; }
	public String getConversationId() { return conversationId; }
	public void setConversationId(String conversationId) { this.conversationId = conversationId; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getSubtitle() { return subtitle; }
	public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
	public Integer getUnreadCount() { return unreadCount; }
	public void setUnreadCount(Integer unreadCount) { this.unreadCount = unreadCount; }
	public String getSyncCursor() { return syncCursor; }
	public void setSyncCursor(String syncCursor) { this.syncCursor = syncCursor; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
