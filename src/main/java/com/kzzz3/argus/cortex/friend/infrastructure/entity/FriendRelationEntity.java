package com.kzzz3.argus.cortex.friend.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("friend_relation")
public class FriendRelationEntity {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String ownerAccountId;
	private String friendAccountId;
	private String note;
	private LocalDateTime createdAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getOwnerAccountId() { return ownerAccountId; }
	public void setOwnerAccountId(String ownerAccountId) { this.ownerAccountId = ownerAccountId; }
	public String getFriendAccountId() { return friendAccountId; }
	public void setFriendAccountId(String friendAccountId) { this.friendAccountId = friendAccountId; }
	public String getNote() { return note; }
	public void setNote(String note) { this.note = note; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
