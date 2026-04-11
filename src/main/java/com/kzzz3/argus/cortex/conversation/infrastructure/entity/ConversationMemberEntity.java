package com.kzzz3.argus.cortex.conversation.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("conversation_member")
public class ConversationMemberEntity {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String ownerAccountId;
	private String conversationId;
	private String memberAccountId;
	private String memberDisplayName;
	private LocalDateTime joinedAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getOwnerAccountId() { return ownerAccountId; }
	public void setOwnerAccountId(String ownerAccountId) { this.ownerAccountId = ownerAccountId; }
	public String getConversationId() { return conversationId; }
	public void setConversationId(String conversationId) { this.conversationId = conversationId; }
	public String getMemberAccountId() { return memberAccountId; }
	public void setMemberAccountId(String memberAccountId) { this.memberAccountId = memberAccountId; }
	public String getMemberDisplayName() { return memberDisplayName; }
	public void setMemberDisplayName(String memberDisplayName) { this.memberDisplayName = memberDisplayName; }
	public LocalDateTime getJoinedAt() { return joinedAt; }
	public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
