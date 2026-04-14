package com.kzzz3.argus.cortex.payment.infrastructure;

import com.kzzz3.argus.cortex.payment.domain.PaymentScanSession;
import com.kzzz3.argus.cortex.payment.domain.PaymentScanSessionStore;
import com.kzzz3.argus.cortex.payment.infrastructure.entity.PaymentScanSessionEntity;
import com.kzzz3.argus.cortex.payment.infrastructure.mapper.PaymentScanSessionMapper;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisPaymentScanSessionStore implements PaymentScanSessionStore {

	private final PaymentScanSessionMapper paymentScanSessionMapper;

	public MybatisPaymentScanSessionStore(PaymentScanSessionMapper paymentScanSessionMapper) {
		this.paymentScanSessionMapper = paymentScanSessionMapper;
	}

	@Override
	public PaymentScanSession save(PaymentScanSession session) {
		paymentScanSessionMapper.insert(toEntity(session));
		return session;
	}

	@Override
	public Optional<PaymentScanSession> findBySessionId(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(paymentScanSessionMapper.selectById(sessionId)).map(this::toRecord);
	}

	private PaymentScanSessionEntity toEntity(PaymentScanSession session) {
		PaymentScanSessionEntity entity = new PaymentScanSessionEntity();
		entity.setSessionId(session.sessionId());
		entity.setPayerAccountId(session.payerAccountId());
		entity.setMerchantAccountId(session.merchantAccountId());
		entity.setMerchantDisplayName(session.merchantDisplayName());
		entity.setCurrency(session.currency());
		entity.setSuggestedAmount(session.suggestedAmount());
		entity.setAmountEditable(session.amountEditable());
		entity.setSuggestedNote(session.suggestedNote());
		entity.setCreatedAt(session.createdAt());
		return entity;
	}

	private PaymentScanSession toRecord(PaymentScanSessionEntity entity) {
		return new PaymentScanSession(
				entity.getSessionId(),
				entity.getPayerAccountId(),
				entity.getMerchantAccountId(),
				entity.getMerchantDisplayName(),
				entity.getCurrency(),
				entity.getSuggestedAmount(),
				Boolean.TRUE.equals(entity.getAmountEditable()),
				entity.getSuggestedNote(),
				entity.getCreatedAt()
		);
	}
}
