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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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

	public MybatisConversationStore(
			ConversationThreadMapper threadMapper,
			ConversationMemberMapper memberMapper,
			ConversationMessageMapper messageMapper,
			MediaAttachmentStore mediaAttachmentStore
	) {
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
				.map(thread -> toSummary(accountRecord.accountId(), thread))
				.toList();
	}

	@Override
	public ConversationDetail getConversationDetail(AccountRecord accountRecord, String conversationId) {
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId);
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
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId);
		if (thread.getSyncCursor().equals(sinceCursor)) {
			return new ConversationMessagePage(List.of(), thread.getSyncCursor(), recentWindowDays, limit);
		}

		ConversationSyncCursor currentCursor = ConversationSyncCursor.parse(conversationId, thread.getSyncCursor());
		ConversationSyncCursor requestedCursor = ConversationSyncCursor.parse(conversationId, sinceCursor);

		List<ConversationMessageEntity> entities;
		String nextSyncCursor;
		if (requestedCursor != null && currentCursor != null && currentCursor.sequence() > requestedCursor.sequence()) {
			entities = selectMessagesAfterSequence(conversationId, requestedCursor.sequence(), limit);
			nextSyncCursor = entities.isEmpty()
					? thread.getSyncCursor()
					: ConversationSyncCursor.of(conversationId, entities.get(entities.size() - 1).getSequenceNo(), currentCursor.revision()).encoded();
		} else {
			entities = selectRecentMessages(conversationId, limit);
			nextSyncCursor = thread.getSyncCursor();
		}

		List<ConversationMessage> messages = entities.stream()
				.map(entity -> toMessage(accountRecord.accountId(), entity))
				.toList();
		return new ConversationMessagePage(messages, nextSyncCursor, recentWindowDays, limit);
	}

	@Override
	public ConversationMessage sendMessage(AccountRecord accountRecord, String conversationId, String clientMessageId, String body, @Nullable ConversationMessageAttachment attachment) {
		requireThread(accountRecord.accountId(), conversationId);
		String normalizedClientMessageId = clientMessageId == null || clientMessageId.isBlank() ? null : clientMessageId.trim();
		if (normalizedClientMessageId != null) {
			ConversationMessageEntity existing = messageMapper.selectOne(new LambdaQueryWrapper<ConversationMessageEntity>()
					.eq(ConversationMessageEntity::getConversationId, conversationId)
					.eq(ConversationMessageEntity::getSenderAccountId, accountRecord.accountId())
					.eq(ConversationMessageEntity::getClientMessageId, normalizedClientMessageId));
			if (existing != null) {
				return toMessage(accountRecord.accountId(), existing);
			}
		}

		LocalDateTime createdAt = ConversationTimeFormatter.nowLocal();
		OffsetDateTime statusUpdatedAt = ConversationTimeFormatter.nowOffset();
		long nextSequence = nextSequence(conversationId);
		ConversationMessageEntity entity = new ConversationMessageEntity();
		entity.setId("msg-" + UUID.randomUUID());
		entity.setClientMessageId(normalizedClientMessageId == null ? entity.getId() : normalizedClientMessageId);
		entity.setConversationId(conversationId);
		entity.setSenderAccountId(accountRecord.accountId());
		entity.setSenderDisplayName(accountRecord.displayName());
		entity.setBody(body.trim());
		entity.setAttachmentId(attachment == null ? null : attachment.attachmentId());
		entity.setTimestampLabel(ConversationTimeFormatter.formatTimestampLabel(createdAt));
		entity.setDeliveryStatus("SENT");
		entity.setStatusUpdatedAt(ConversationTimeFormatter.formatStatusUpdatedAt(statusUpdatedAt));
		entity.setSequenceNo(nextSequence);
		entity.setCreatedAt(createdAt);
		messageMapper.insert(entity);
		advanceAllThreadCursors(conversationId, nextSequence, accountRecord.accountId());
		return toMessage(accountRecord.accountId(), entity);
	}

	@Override
	public ConversationMessage applyReceipt(AccountRecord accountRecord, String conversationId, String messageId, String receiptType) {
		requireThread(accountRecord.accountId(), conversationId);
		ConversationMessageEntity entity = requireMessage(conversationId, messageId);
		String normalizedReceiptType = normalizeReceiptType(receiptType);
		String nextStatus = mergeDeliveryStatus(entity.getDeliveryStatus(), normalizedReceiptType);
		if (nextStatus.equals(entity.getDeliveryStatus()) || accountRecord.accountId().equals(entity.getSenderAccountId())) {
			return toMessage(accountRecord.accountId(), entity);
		}
		entity.setDeliveryStatus(nextStatus);
		entity.setStatusUpdatedAt(ConversationTimeFormatter.formatStatusUpdatedAt(ConversationTimeFormatter.nowOffset()));
		messageMapper.updateById(entity);
		advanceAllThreadCursors(conversationId, entity.getSequenceNo(), null);
		return toMessage(accountRecord.accountId(), entity);
	}

	@Override
	public ConversationMessage recallMessage(AccountRecord accountRecord, String conversationId, String messageId) {
		requireThread(accountRecord.accountId(), conversationId);
		ConversationMessageEntity entity = requireMessage(conversationId, messageId);
		if (!accountRecord.accountId().equals(entity.getSenderAccountId())) {
			throw new IllegalArgumentException("Only the sender can recall this message.");
		}
		entity.setBody("Message recalled");
		entity.setDeliveryStatus("RECALLED");
		entity.setStatusUpdatedAt(ConversationTimeFormatter.formatStatusUpdatedAt(ConversationTimeFormatter.nowOffset()));
		messageMapper.updateById(entity);
		advanceAllThreadCursors(conversationId, entity.getSequenceNo(), null);
		return toMessage(accountRecord.accountId(), entity);
	}

	@Override
	public ConversationSummary markConversationRead(AccountRecord accountRecord, String conversationId) {
		ConversationThreadEntity thread = requireThread(accountRecord.accountId(), conversationId);
		thread.setUnreadCount(0);
		advanceThreadCursor(thread, latestSequence(conversationId));
		return toSummary(accountRecord.accountId(), thread);
	}

	@Override
	public ConversationSummary ensureDirectConversation(AccountRecord owner, AccountRecord friend) {
		String conversationId = directConversationId(owner.accountId(), friend.accountId());
		ConversationThreadEntity ownerThread = ensureDirectConversationThread(owner.accountId(), friend.displayName(), conversationId);
		ensureDirectConversationThread(friend.accountId(), owner.displayName(), conversationId);
		ensureMember(owner.accountId(), conversationId, owner.accountId(), owner.displayName());
		ensureMember(owner.accountId(), conversationId, friend.accountId(), friend.displayName());
		ensureMember(friend.accountId(), conversationId, owner.accountId(), owner.displayName());
		ensureMember(friend.accountId(), conversationId, friend.accountId(), friend.displayName());
		return toSummary(owner.accountId(), ownerThread);
	}

	private ConversationThreadEntity requireThread(String ownerAccountId, String conversationId) {
		ConversationThreadEntity entity = threadMapper.selectOne(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getOwnerAccountId, ownerAccountId)
				.eq(ConversationThreadEntity::getConversationId, conversationId));
		if (entity == null) {
			throw new ConversationNotFoundException(conversationId);
		}
		return entity;
	}

	private ConversationMessageEntity requireMessage(String conversationId, String messageId) {
		ConversationMessageEntity entity = messageMapper.selectOne(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getConversationId, conversationId)
				.eq(ConversationMessageEntity::getId, messageId));
		if (entity == null) {
			throw new MessageNotFoundException(messageId);
		}
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
		entity.setUpdatedAt(ConversationTimeFormatter.nowLocal());
		threadMapper.insert(entity);
	}

	private void insertMember(String ownerAccountId, String conversationId, String memberAccountId, String memberDisplayName) {
		ConversationMemberEntity entity = new ConversationMemberEntity();
		entity.setOwnerAccountId(ownerAccountId);
		entity.setConversationId(conversationId);
		entity.setMemberAccountId(memberAccountId);
		entity.setMemberDisplayName(memberDisplayName);
		entity.setJoinedAt(ConversationTimeFormatter.nowLocal());
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

	private long nextSequence(String conversationId) {
		return latestSequence(conversationId) + 1L;
	}

	private long latestSequence(String conversationId) {
		ConversationMessageEntity latest = messageMapper.selectOne(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getConversationId, conversationId)
				.orderByDesc(ConversationMessageEntity::getSequenceNo)
				.last("LIMIT 1"));
		return latest == null ? 0L : latest.getSequenceNo();
	}

	private List<ConversationMessageEntity> selectMessagesAfterSequence(String conversationId, long sequenceNo, int limit) {
		return messageMapper.selectList(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getConversationId, conversationId)
				.gt(ConversationMessageEntity::getSequenceNo, sequenceNo)
				.orderByAsc(ConversationMessageEntity::getSequenceNo)
				.last("LIMIT " + limit));
	}

	private List<ConversationMessageEntity> selectRecentMessages(String conversationId, int limit) {
		List<ConversationMessageEntity> recentMessages = new ArrayList<>(messageMapper.selectList(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getConversationId, conversationId)
				.orderByDesc(ConversationMessageEntity::getSequenceNo)
				.last("LIMIT " + limit)));
		Collections.reverse(recentMessages);
		return recentMessages;
	}

	private void advanceAllThreadCursors(String conversationId, long affectedSequenceNo, @Nullable String senderAccountId) {
		List<ConversationThreadEntity> threads = threadMapper.selectList(new LambdaQueryWrapper<ConversationThreadEntity>()
				.eq(ConversationThreadEntity::getConversationId, conversationId));
		for (ConversationThreadEntity thread : threads) {
			if (senderAccountId != null && !senderAccountId.equals(thread.getOwnerAccountId())) {
				thread.setUnreadCount((thread.getUnreadCount() == null ? 0 : thread.getUnreadCount()) + 1);
			}
			advanceThreadCursor(thread, affectedSequenceNo);
		}
	}

	private void advanceThreadCursor(ConversationThreadEntity thread, long affectedSequenceNo) {
		ConversationSyncCursor currentCursor = ConversationSyncCursor.parse(thread.getConversationId(), thread.getSyncCursor());
		long nextRevision = currentCursor == null ? 1L : currentCursor.nextRevision();
		long nextSequence = currentCursor == null
				? affectedSequenceNo
				: Math.max(currentCursor.sequence(), affectedSequenceNo);
		thread.setSyncCursor(ConversationSyncCursor.of(thread.getConversationId(), nextSequence, nextRevision).encoded());
		thread.setUpdatedAt(ConversationTimeFormatter.nowLocal());
		threadMapper.updateById(thread);
	}

	private ConversationSummary toSummary(String viewerAccountId, ConversationThreadEntity entity) {
		ConversationMessageEntity latestMessage = messageMapper.selectOne(new LambdaQueryWrapper<ConversationMessageEntity>()
				.eq(ConversationMessageEntity::getConversationId, entity.getConversationId())
				.orderByDesc(ConversationMessageEntity::getSequenceNo)
				.last("LIMIT 1"));
		String preview = latestMessage == null ? "No messages yet" : renderBody(viewerAccountId, latestMessage);
		String timestampLabel = latestMessage == null ? "--:--" : latestMessage.getTimestampLabel();
		return new ConversationSummary(
				entity.getConversationId(),
				entity.getTitle(),
				entity.getSubtitle(),
				preview,
				timestampLabel,
				entity.getUnreadCount() == null ? 0 : entity.getUnreadCount(),
				entity.getSyncCursor()
		);
	}

	private ConversationMessage toMessage(String viewerAccountId, ConversationMessageEntity entity) {
		return new ConversationMessage(
				entity.getId(),
				entity.getConversationId(),
				entity.getSenderDisplayName(),
				renderBody(viewerAccountId, entity),
				entity.getTimestampLabel(),
				viewerAccountId.equals(entity.getSenderAccountId()),
				entity.getDeliveryStatus(),
				entity.getStatusUpdatedAt(),
				resolveAttachment(entity.getAttachmentId())
		);
	}

	private String renderBody(String viewerAccountId, ConversationMessageEntity entity) {
		if ("RECALLED".equals(entity.getDeliveryStatus())) {
			return viewerAccountId.equals(entity.getSenderAccountId())
					? "You recalled a message"
					: "Message recalled";
		}
		return entity.getBody();
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

	private String normalizeReceiptType(String receiptType) {
		return receiptType == null ? "DELIVERED" : receiptType.trim().toUpperCase(Locale.ROOT);
	}

	private String mergeDeliveryStatus(String currentStatus, String nextStatus) {
		if ("RECALLED".equals(currentStatus)) {
			return currentStatus;
		}
		return deliveryRank(nextStatus) > deliveryRank(currentStatus) ? nextStatus : currentStatus;
	}

	private int deliveryRank(@Nullable String status) {
		if (status == null) {
			return 0;
		}
		return switch (status.toUpperCase(Locale.ROOT)) {
			case "SENT" -> 1;
			case "DELIVERED" -> 2;
			case "READ" -> 3;
			case "RECALLED" -> 4;
			default -> 0;
		};
	}

	private String directConversationId(String accountIdA, String accountIdB) {
		return accountIdA.compareTo(accountIdB) < 0
				? "conv-direct-" + accountIdA + "-" + accountIdB
				: "conv-direct-" + accountIdB + "-" + accountIdA;
	}
}
