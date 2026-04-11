package com.kzzz3.argus.cortex.conversation.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessagePage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationNotFoundException;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationSummary;
import com.kzzz3.argus.cortex.conversation.domain.MessageNotFoundException;
import com.kzzz3.argus.cortex.conversation.infrastructure.entity.ConversationMessageEntity;
import com.kzzz3.argus.cortex.conversation.infrastructure.entity.ConversationThreadEntity;
import com.kzzz3.argus.cortex.conversation.infrastructure.mapper.ConversationMessageMapper;
import com.kzzz3.argus.cortex.conversation.infrastructure.mapper.ConversationThreadMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisConversationStore implements ConversationStore {

	private final ConversationThreadMapper threadMapper;
	private final ConversationMessageMapper messageMapper;

	public MybatisConversationStore(ConversationThreadMapper threadMapper, ConversationMessageMapper messageMapper) {
		this.threadMapper = threadMapper;
		this.messageMapper = messageMapper;
	}

	@Override
	public List<ConversationSummary> listConversations(AccountRecord accountRecord, int recentWindowDays) {
		seedConversations(accountRecord, recentWindowDays);
		return threadMapper.selectList(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getOwnerAccountId, accountRecord.accountId())
				.orderByDesc(ConversationThreadEntity::getUpdatedAt))
				.stream()
				.map(this::toSummary)
				.toList();
	}

	@Override
	public ConversationMessagePage listMessages(AccountRecord accountRecord, String conversationId, int recentWindowDays, int limit, String sinceCursor) {
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId, recentWindowDays);
		if (thread.getSyncCursor().equals(sinceCursor)) {
			return new ConversationMessagePage(List.of(), thread.getSyncCursor(), recentWindowDays, limit);
		}
		List<ConversationMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getOwnerAccountId, accountRecord.accountId())
				.eq(ConversationMessageEntity::getConversationId, conversationId)
				.orderByAsc(ConversationMessageEntity::getSequenceNo)
				.last("LIMIT " + limit))
				.stream()
				.map(this::toMessage)
				.toList();
		return new ConversationMessagePage(messages, thread.getSyncCursor(), recentWindowDays, limit);
	}

	@Override
	public ConversationMessage sendMessage(AccountRecord accountRecord, String conversationId, String body) {
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId, 7);
		long nextSequence = nextSequence(accountRecord.accountId(), conversationId);
		ConversationMessageEntity entity = new ConversationMessageEntity();
		entity.setId("msg-" + UUID.randomUUID());
		entity.setOwnerAccountId(accountRecord.accountId());
		entity.setConversationId(conversationId);
		entity.setSenderAccountId(accountRecord.accountId());
		entity.setSenderDisplayName(accountRecord.displayName());
		entity.setBody(body.trim());
		entity.setTimestampLabel("Now");
		entity.setFromCurrentUser(true);
		entity.setDeliveryStatus("DELIVERED");
		entity.setStatusUpdatedAt("2026-04-10T21:10:00+08:00");
		entity.setSequenceNo(nextSequence);
		entity.setCreatedAt(LocalDateTime.now());
		messageMapper.insert(entity);
		bumpThreadCursor(thread, nextSequence);
		return toMessage(entity);
	}

	@Override
	public ConversationMessage applyReceipt(AccountRecord accountRecord, String conversationId, String messageId, String receiptType) {
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId, 7);
		ConversationMessageEntity entity = requireMessage(accountRecord.accountId(), conversationId, messageId);
		String nextStatus = receiptType == null ? "DELIVERED" : receiptType.trim().toUpperCase();
		entity.setDeliveryStatus(nextStatus);
		entity.setStatusUpdatedAt("2026-04-10T22:30:00+08:00");
		messageMapper.updateById(entity);
		bumpThreadCursor(thread, entity.getSequenceNo());
		return toMessage(entity);
	}

	@Override
	public ConversationMessage recallMessage(AccountRecord accountRecord, String conversationId, String messageId) {
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId, 7);
		ConversationMessageEntity entity = requireMessage(accountRecord.accountId(), conversationId, messageId);
		entity.setSenderDisplayName(accountRecord.displayName());
		entity.setBody("You recalled a message");
		entity.setDeliveryStatus("RECALLED");
		entity.setStatusUpdatedAt("2026-04-10T21:11:00+08:00");
		messageMapper.updateById(entity);
		bumpThreadCursor(thread, entity.getSequenceNo());
		return toMessage(entity);
	}

	@Override
	public ConversationSummary markConversationRead(AccountRecord accountRecord, String conversationId) {
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId, 7);
		thread.setUnreadCount(0);
		thread.setUpdatedAt(LocalDateTime.now());
		thread.setSyncCursor(nextCursor(conversationId, nextSequence(accountRecord.accountId(), conversationId)));
		threadMapper.updateById(thread);
		return toSummary(thread);
	}

	@Override
	public ConversationSummary ensureDirectConversation(AccountRecord owner, AccountRecord friend) {
		String conversationId = directConversationId(owner.accountId(), friend.accountId());
		ConversationThreadEntity ownerThread = ensureDirectConversationThread(owner.accountId(), friend.displayName(), conversationId);
		ensureDirectConversationThread(friend.accountId(), owner.displayName(), conversationId);
		return toSummary(ownerThread);
	}

	private ConversationThreadEntity requireThread(String ownerAccountId, String conversationId, int recentWindowDays) {
		seedConversations(new AccountRecord(ownerAccountId, ownerAccountId, ""), recentWindowDays);
		ConversationThreadEntity entity = threadMapper.selectOne(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getOwnerAccountId, ownerAccountId)
				.eq(ConversationThreadEntity::getConversationId, conversationId));
		if (entity == null) throw new ConversationNotFoundException(conversationId);
		return entity;
	}

	private ConversationMessageEntity requireMessage(String ownerAccountId, String conversationId, String messageId) {
		ConversationMessageEntity entity = messageMapper.selectOne(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getOwnerAccountId, ownerAccountId)
				.eq(ConversationMessageEntity::getConversationId, conversationId)
				.eq(ConversationMessageEntity::getId, messageId));
		if (entity == null) throw new MessageNotFoundException(messageId);
		return entity;
	}

	private void seedConversations(AccountRecord accountRecord, int recentWindowDays) {
		long count = threadMapper.selectCount(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getOwnerAccountId, accountRecord.accountId()));
		if (count > 0) return;

		insertThread(accountRecord.accountId(), "conv-zhang-san", "Zhang San", "1:1 direct chat", 2);
		insertThread(accountRecord.accountId(), "conv-project-group", "Project Group", "3 members", 0);
		insertThread(accountRecord.accountId(), "conv-li-si", "Li Si", "Feature review", 1);

		insertMessage(accountRecord.accountId(), "conv-zhang-san", 1L, "zhangsan", "Zhang San", "Remote message sync currently serves a recent " + recentWindowDays + "-day window.", false, "DELIVERED", "2026-04-10T09:24:00+08:00", "09:24");
		insertMessage(accountRecord.accountId(), "conv-zhang-san", 2L, accountRecord.accountId(), accountRecord.displayName(), "This reply comes from the authenticated Android account inside the remote recent window.", true, "DELIVERED", "2026-04-10T09:28:00+08:00", "09:28");
		insertMessage(accountRecord.accountId(), "conv-project-group", 1L, "project-group", "Project Group", "Next step: replace seeded windowed messages with real sync storage.", false, "DELIVERED", "2026-04-09T20:00:00+08:00", "Yesterday");
		insertMessage(accountRecord.accountId(), "conv-li-si", 1L, "lisi", "Li Si", "Cursor-based sync is the next realistic IM step.", false, "SENT", "2026-04-08T10:00:00+08:00", "Mon");
	}

	private void insertThread(String ownerAccountId, String conversationId, String title, String subtitle, int unreadCount) {
		ConversationThreadEntity entity = new ConversationThreadEntity();
		entity.setOwnerAccountId(ownerAccountId);
		entity.setConversationId(conversationId);
		entity.setTitle(title);
		entity.setSubtitle(subtitle);
		entity.setUnreadCount(unreadCount);
		entity.setSyncCursor(nextCursor(conversationId, 0));
		entity.setUpdatedAt(LocalDateTime.now());
		threadMapper.insert(entity);
	}

	private ConversationThreadEntity ensureDirectConversationThread(String ownerAccountId, String title, String conversationId) {
		ConversationThreadEntity existing = threadMapper.selectOne(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getOwnerAccountId, ownerAccountId)
				.eq(ConversationThreadEntity::getConversationId, conversationId));
		if (existing != null) {
			return existing;
		}
		insertThread(ownerAccountId, conversationId, title, "Direct friend conversation", 0);
		return threadMapper.selectOne(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getOwnerAccountId, ownerAccountId)
				.eq(ConversationThreadEntity::getConversationId, conversationId));
	}

	private void insertMessage(String ownerAccountId, String conversationId, long sequenceNo, String senderAccountId, String senderDisplayName, String body, boolean fromCurrentUser, String deliveryStatus, String statusUpdatedAt, String timestampLabel) {
		ConversationMessageEntity entity = new ConversationMessageEntity();
		entity.setId("seed-" + conversationId + "-" + sequenceNo);
		entity.setOwnerAccountId(ownerAccountId);
		entity.setConversationId(conversationId);
		entity.setSenderAccountId(senderAccountId);
		entity.setSenderDisplayName(senderDisplayName);
		entity.setBody(body);
		entity.setTimestampLabel(timestampLabel);
		entity.setFromCurrentUser(fromCurrentUser);
		entity.setDeliveryStatus(deliveryStatus);
		entity.setStatusUpdatedAt(statusUpdatedAt);
		entity.setSequenceNo(sequenceNo);
		entity.setCreatedAt(LocalDateTime.now());
		messageMapper.insert(entity);
		ConversationThreadEntity thread = threadMapper.selectOne(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getOwnerAccountId, ownerAccountId)
				.eq(ConversationThreadEntity::getConversationId, conversationId));
		if (thread != null) {
			bumpThreadCursor(thread, sequenceNo);
		}
	}

	private long nextSequence(String ownerAccountId, String conversationId) {
		ConversationMessageEntity latest = messageMapper.selectOne(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getOwnerAccountId, ownerAccountId)
				.eq(ConversationMessageEntity::getConversationId, conversationId)
				.orderByDesc(ConversationMessageEntity::getSequenceNo)
				.last("LIMIT 1"));
		return latest == null ? 1L : latest.getSequenceNo() + 1L;
	}

	private void bumpThreadCursor(ConversationThreadEntity thread, long sequenceNo) {
		thread.setSyncCursor(nextCursor(thread.getConversationId(), sequenceNo));
		thread.setUpdatedAt(LocalDateTime.now());
		threadMapper.updateById(thread);
	}

	private ConversationSummary toSummary(ConversationThreadEntity entity) {
		ConversationMessageEntity latestMessage = messageMapper.selectOne(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getOwnerAccountId, entity.getOwnerAccountId())
				.eq(ConversationMessageEntity::getConversationId, entity.getConversationId())
				.orderByDesc(ConversationMessageEntity::getSequenceNo)
				.last("LIMIT 1"));
		String preview = latestMessage == null ? "No messages yet" : latestMessage.getBody();
		String timestampLabel = latestMessage == null ? "--:--" : latestMessage.getTimestampLabel();
		return new ConversationSummary(
				entity.getConversationId(),
				entity.getTitle(),
				entity.getSubtitle(),
				preview,
				timestampLabel,
				entity.getUnreadCount(),
				entity.getSyncCursor()
		);
	}

	private ConversationMessage toMessage(ConversationMessageEntity entity) {
		return new ConversationMessage(
				entity.getId(),
				entity.getConversationId(),
				entity.getSenderDisplayName(),
				entity.getBody(),
				entity.getTimestampLabel(),
				Boolean.TRUE.equals(entity.getFromCurrentUser()),
				entity.getDeliveryStatus(),
				entity.getStatusUpdatedAt()
		);
	}

	private String nextCursor(String conversationId, long sequenceNo) {
		return "cursor-" + conversationId + "-" + sequenceNo;
	}

	private String directConversationId(String accountIdA, String accountIdB) {
		return accountIdA.compareTo(accountIdB) < 0
				? "conv-direct-" + accountIdA + "-" + accountIdB
				: "conv-direct-" + accountIdB + "-" + accountIdA;
	}
}
