package com.kzzz3.argus.cortex.conversation.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.conversation.domain.ConversationDetail;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessageAttachment;
import com.kzzz3.argus.cortex.conversation.domain.ConversationMessagePage;
import com.kzzz3.argus.cortex.conversation.domain.ConversationNotFoundException;
import com.kzzz3.argus.cortex.conversation.domain.ConversationStore;
import com.kzzz3.argus.cortex.conversation.domain.ConversationSummary;
import com.kzzz3.argus.cortex.conversation.domain.MessageNotFoundException;
import com.kzzz3.argus.cortex.conversation.infrastructure.entity.ConversationMemberEntity;
import com.kzzz3.argus.cortex.conversation.infrastructure.entity.ConversationMessageEntity;
import com.kzzz3.argus.cortex.conversation.infrastructure.entity.ConversationThreadEntity;
import com.kzzz3.argus.cortex.conversation.infrastructure.mapper.ConversationMemberMapper;
import com.kzzz3.argus.cortex.conversation.infrastructure.mapper.ConversationMessageMapper;
import com.kzzz3.argus.cortex.conversation.infrastructure.mapper.ConversationThreadMapper;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisConversationStore implements ConversationStore {

	private final ConversationThreadMapper threadMapper;
	private final ConversationMemberMapper memberMapper;
	private final ConversationMessageMapper messageMapper;
	private final MediaAttachmentStore mediaAttachmentStore;

	public MybatisConversationStore(ConversationThreadMapper threadMapper, ConversationMemberMapper memberMapper, ConversationMessageMapper messageMapper, MediaAttachmentStore mediaAttachmentStore) {
		this.threadMapper = threadMapper;
		this.memberMapper = memberMapper;
		this.messageMapper = messageMapper;
		this.mediaAttachmentStore = mediaAttachmentStore;
	}

	@Override
	public List<ConversationSummary> listConversations(AccountRecord accountRecord, int recentWindowDays) {
		return threadMapper.selectList(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getOwnerAccountId, accountRecord.accountId())
				.orderByDesc(ConversationThreadEntity::getUpdatedAt))
				.stream()
				.map(this::toSummary)
				.toList();
	}

	@Override
	public ConversationDetail getConversationDetail(AccountRecord accountRecord, String conversationId) {
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId, 7);
		List<String> members = memberMapper.selectList(new LambdaQueryWrapper<ConversationMemberEntity>()
				.eq(ConversationMemberEntity::getOwnerAccountId, accountRecord.accountId())
				.eq(ConversationMemberEntity::getConversationId, conversationId)
				.orderByAsc(ConversationMemberEntity::getId))
				.stream()
				.map(ConversationMemberEntity::getMemberDisplayName)
				.toList();
		if (members.isEmpty()) {
			members = List.of(accountRecord.displayName(), thread.getTitle());
		}
		return new ConversationDetail(
				thread.getConversationId(),
				thread.getTitle(),
				thread.getSubtitle(),
				members.size(),
				members
		);
	}

	@Override
	public ConversationMessagePage listMessages(AccountRecord accountRecord, String conversationId, int recentWindowDays, int limit, String sinceCursor) {
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId, recentWindowDays);
		if (thread.getSyncCursor().equals(sinceCursor)) {
			return new ConversationMessagePage(List.of(), thread.getSyncCursor(), recentWindowDays, limit);
		}

		ConversationSyncCursor currentCursor = ConversationSyncCursor.parse(conversationId, thread.getSyncCursor());
		ConversationSyncCursor requestedCursor = ConversationSyncCursor.parse(conversationId, sinceCursor);

		List<ConversationMessageEntity> entities;
		String nextSyncCursor;
		if (requestedCursor != null && currentCursor != null && currentCursor.sequence() > requestedCursor.sequence()) {
			entities = selectMessagesAfterSequence(accountRecord.accountId(), conversationId, requestedCursor.sequence(), limit);
			nextSyncCursor = entities.isEmpty()
					? thread.getSyncCursor()
					: ConversationSyncCursor.of(conversationId, entities.get(entities.size() - 1).getSequenceNo(), currentCursor.revision()).encoded();
		} else {
			entities = selectRecentMessages(accountRecord.accountId(), conversationId, limit);
			nextSyncCursor = thread.getSyncCursor();
		}

		List<ConversationMessage> messages = entities.stream()
				.map(this::toMessage)
				.toList();
		return new ConversationMessagePage(messages, nextSyncCursor, recentWindowDays, limit);
	}

	@Override
	public ConversationMessage sendMessage(AccountRecord accountRecord, String conversationId, String clientMessageId, String body, @Nullable ConversationMessageAttachment attachment) {
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId, 7);
		if (clientMessageId != null && !clientMessageId.isBlank()) {
			ConversationMessageEntity existing = messageMapper.selectOne(new LambdaQueryWrapper<ConversationMessageEntity>()
					.eq(ConversationMessageEntity::getOwnerAccountId, accountRecord.accountId())
					.eq(ConversationMessageEntity::getConversationId, conversationId)
					.eq(ConversationMessageEntity::getClientMessageId, clientMessageId));
			if (existing != null) {
				return toMessage(existing);
			}
		}
		long nextSequence = nextSequence(accountRecord.accountId(), conversationId);
		ConversationMessageEntity entity = new ConversationMessageEntity();
		entity.setId("msg-" + UUID.randomUUID());
		entity.setClientMessageId(clientMessageId == null || clientMessageId.isBlank() ? entity.getId() : clientMessageId);
		entity.setOwnerAccountId(accountRecord.accountId());
		entity.setConversationId(conversationId);
		entity.setSenderAccountId(accountRecord.accountId());
		entity.setSenderDisplayName(accountRecord.displayName());
		entity.setBody(body.trim());
		entity.setAttachmentId(attachment == null ? null : attachment.attachmentId());
		entity.setTimestampLabel("Now");
		entity.setFromCurrentUser(true);
		entity.setDeliveryStatus("DELIVERED");
		entity.setStatusUpdatedAt("2026-04-10T21:10:00+08:00");
		entity.setSequenceNo(nextSequence);
		entity.setCreatedAt(LocalDateTime.now());
		messageMapper.insert(entity);
		advanceThreadCursor(thread, nextSequence);
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
		advanceThreadCursor(thread, entity.getSequenceNo());
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
		advanceThreadCursor(thread, entity.getSequenceNo());
		return toMessage(entity);
	}

	@Override
	public ConversationSummary markConversationRead(AccountRecord accountRecord, String conversationId) {
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId, 7);
		thread.setUnreadCount(0);
		advanceThreadCursor(thread, latestSequence(accountRecord.accountId(), conversationId));
		return toSummary(thread);
	}

	@Override
	public ConversationSummary ensureDirectConversation(AccountRecord owner, AccountRecord friend) {
		String conversationId = directConversationId(owner.accountId(), friend.accountId());
		ConversationThreadEntity ownerThread = ensureDirectConversationThread(owner.accountId(), friend.displayName(), conversationId);
		ensureDirectConversationThread(friend.accountId(), owner.displayName(), conversationId);
		return toSummary(ownerThread);
	}

	@Override
	public ConversationSummary createConversation(AccountRecord owner, String type, String title) {
		String normalizedType = type == null ? "GROUP" : type.trim().toUpperCase();
		String normalizedTitle = title == null || title.isBlank() ? "Untitled Conversation" : title.trim();
		String conversationId = switch (normalizedType) {
			case "DIRECT" -> "conv-direct-" + owner.accountId() + "-self";
			case "GROUP" -> "conv-group-" + UUID.randomUUID();
			default -> "conv-group-" + UUID.randomUUID();
		};
		ConversationThreadEntity existing = threadMapper.selectOne(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getOwnerAccountId, owner.accountId())
				.eq(ConversationThreadEntity::getConversationId, conversationId));
		if (existing != null) {
			return toSummary(existing);
		}
		insertThread(
				owner.accountId(),
				conversationId,
				normalizedTitle,
				normalizedType.equals("GROUP") ? "Remote group conversation" : "Remote direct conversation",
				0
		);
		ConversationThreadEntity created = threadMapper.selectOne(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getOwnerAccountId, owner.accountId())
				.eq(ConversationThreadEntity::getConversationId, conversationId));
		insertMember(owner.accountId(), conversationId, owner.accountId(), owner.displayName());
		return toSummary(created);
	}

	@Override
	public ConversationDetail addMember(AccountRecord owner, String conversationId, AccountRecord member) {
		ConversationThreadEntity ownerThread = requireThread(owner.accountId(), conversationId, 7);
		ensureMember(owner.accountId(), conversationId, member.accountId(), member.displayName());

		List<ConversationThreadEntity> existingThreads = threadMapper.selectList(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getConversationId, conversationId));
		boolean memberHasThread = existingThreads.stream().anyMatch(thread -> thread.getOwnerAccountId().equals(member.accountId()));
		if (!memberHasThread) {
			insertThread(member.accountId(), conversationId, ownerThread.getTitle(), ownerThread.getSubtitle(), 0);
		}

		existingThreads = threadMapper.selectList(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getConversationId, conversationId));
		for (ConversationThreadEntity thread : existingThreads) {
			ensureMember(thread.getOwnerAccountId(), conversationId, member.accountId(), member.displayName());
		}
		ensureMember(member.accountId(), conversationId, owner.accountId(), owner.displayName());
		return getConversationDetail(owner, conversationId);
	}

	private ConversationThreadEntity requireThread(String ownerAccountId, String conversationId, int recentWindowDays) {
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

	private void insertThread(String ownerAccountId, String conversationId, String title, String subtitle, int unreadCount) {
		ConversationThreadEntity entity = new ConversationThreadEntity();
		entity.setOwnerAccountId(ownerAccountId);
		entity.setConversationId(conversationId);
		entity.setTitle(title);
		entity.setSubtitle(subtitle);
		entity.setUnreadCount(unreadCount);
		entity.setSyncCursor(ConversationSyncCursor.of(conversationId, 0L, 0L).encoded());
		entity.setUpdatedAt(LocalDateTime.now());
		threadMapper.insert(entity);
	}

	private void insertMember(String ownerAccountId, String conversationId, String memberAccountId, String memberDisplayName) {
		ConversationMemberEntity entity = new ConversationMemberEntity();
		entity.setOwnerAccountId(ownerAccountId);
		entity.setConversationId(conversationId);
		entity.setMemberAccountId(memberAccountId);
		entity.setMemberDisplayName(memberDisplayName);
		entity.setJoinedAt(LocalDateTime.now());
		memberMapper.insert(entity);
	}

	private void ensureMember(String ownerAccountId, String conversationId, String memberAccountId, String memberDisplayName) {
		long count = memberMapper.selectCount(new LambdaQueryWrapper<ConversationMemberEntity>()
				.eq(ConversationMemberEntity::getOwnerAccountId, ownerAccountId)
				.eq(ConversationMemberEntity::getConversationId, conversationId)
				.eq(ConversationMemberEntity::getMemberAccountId, memberAccountId));
		if (count == 0L) {
			insertMember(ownerAccountId, conversationId, memberAccountId, memberDisplayName);
		}
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


	private long nextSequence(String ownerAccountId, String conversationId) {
		return latestSequence(ownerAccountId, conversationId) + 1L;
	}

	private long latestSequence(String ownerAccountId, String conversationId) {
		ConversationMessageEntity latest = messageMapper.selectOne(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getOwnerAccountId, ownerAccountId)
				.eq(ConversationMessageEntity::getConversationId, conversationId)
				.orderByDesc(ConversationMessageEntity::getSequenceNo)
				.last("LIMIT 1"));
		return latest == null ? 0L : latest.getSequenceNo();
	}

	private List<ConversationMessageEntity> selectMessagesAfterSequence(String ownerAccountId, String conversationId, long sequenceNo, int limit) {
		return messageMapper.selectList(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getOwnerAccountId, ownerAccountId)
				.eq(ConversationMessageEntity::getConversationId, conversationId)
				.gt(ConversationMessageEntity::getSequenceNo, sequenceNo)
				.orderByAsc(ConversationMessageEntity::getSequenceNo)
				.last("LIMIT " + limit));
	}

	private List<ConversationMessageEntity> selectRecentMessages(String ownerAccountId, String conversationId, int limit) {
		List<ConversationMessageEntity> recentMessages = new ArrayList<>(messageMapper.selectList(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getOwnerAccountId, ownerAccountId)
				.eq(ConversationMessageEntity::getConversationId, conversationId)
				.orderByDesc(ConversationMessageEntity::getSequenceNo)
				.last("LIMIT " + limit)));
		Collections.reverse(recentMessages);
		return recentMessages;
	}

	private void advanceThreadCursor(ConversationThreadEntity thread, long affectedSequenceNo) {
		ConversationSyncCursor currentCursor = ConversationSyncCursor.parse(thread.getConversationId(), thread.getSyncCursor());
		long nextRevision = currentCursor == null ? 1L : currentCursor.nextRevision();
		long nextSequence = currentCursor == null
				? affectedSequenceNo
				: Math.max(currentCursor.sequence(), affectedSequenceNo);
		thread.setSyncCursor(ConversationSyncCursor.of(thread.getConversationId(), nextSequence, nextRevision).encoded());
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
				entity.getStatusUpdatedAt(),
				resolveAttachment(entity.getAttachmentId())
		);
	}

	@Nullable
	private ConversationMessageAttachment resolveAttachment(@Nullable String attachmentId) {
		if (attachmentId == null || attachmentId.isBlank()) {
			return null;
		}
		return mediaAttachmentStore.findByAttachmentId(attachmentId)
				.map(this::toAttachment)
				.orElse(null);
	}

	private ConversationMessageAttachment toAttachment(MediaAttachmentRecord record) {
		return new ConversationMessageAttachment(
				record.attachmentId(),
				record.attachmentType().name(),
				record.fileName(),
				record.contentType(),
				record.contentLength()
		);
	}

	private String directConversationId(String accountIdA, String accountIdB) {
		return accountIdA.compareTo(accountIdB) < 0
				? "conv-direct-" + accountIdA + "-" + accountIdB
				: "conv-direct-" + accountIdB + "-" + accountIdA;
	}
}
