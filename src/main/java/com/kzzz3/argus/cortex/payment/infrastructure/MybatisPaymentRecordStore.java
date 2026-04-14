package com.kzzz3.argus.cortex.payment.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kzzz3.argus.cortex.payment.domain.PaymentRecord;
import com.kzzz3.argus.cortex.payment.domain.PaymentRecordStore;
import com.kzzz3.argus.cortex.payment.infrastructure.entity.PaymentRecordEntity;
import com.kzzz3.argus.cortex.payment.infrastructure.mapper.PaymentRecordMapper;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisPaymentRecordStore implements PaymentRecordStore {

	private final PaymentRecordMapper paymentRecordMapper;

	public MybatisPaymentRecordStore(PaymentRecordMapper paymentRecordMapper) {
		this.paymentRecordMapper = paymentRecordMapper;
	}

	@Override
	public PaymentRecord save(PaymentRecord paymentRecord) {
		paymentRecordMapper.insert(toEntity(paymentRecord));
		return paymentRecord;
	}

	@Override
	public Optional<PaymentRecord> findBySessionId(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return Optional.empty();
		}
		PaymentRecordEntity entity = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecordEntity>()
				.eq(PaymentRecordEntity::getSessionId, sessionId));
		return Optional.ofNullable(entity).map(this::toRecord);
	}

	private PaymentRecordEntity toEntity(PaymentRecord paymentRecord) {
		PaymentRecordEntity entity = new PaymentRecordEntity();
		entity.setPaymentId(paymentRecord.paymentId());
		entity.setSessionId(paymentRecord.sessionId());
		entity.setPayerAccountId(paymentRecord.payerAccountId());
		entity.setMerchantAccountId(paymentRecord.merchantAccountId());
		entity.setMerchantDisplayName(paymentRecord.merchantDisplayName());
		entity.setConversationId(paymentRecord.conversationId());
		entity.setAmount(paymentRecord.amount());
		entity.setCurrency(paymentRecord.currency());
		entity.setNote(paymentRecord.note());
		entity.setStatus(paymentRecord.status());
		entity.setCreatedAt(paymentRecord.createdAt());
		return entity;
	}

	private PaymentRecord toRecord(PaymentRecordEntity entity) {
		return new PaymentRecord(
				entity.getPaymentId(),
				entity.getSessionId(),
				entity.getPayerAccountId(),
				entity.getMerchantAccountId(),
				entity.getMerchantDisplayName(),
				entity.getConversationId(),
				entity.getAmount(),
				entity.getCurrency(),
				entity.getNote(),
				entity.getStatus(),
				entity.getCreatedAt()
		);
	}
}
