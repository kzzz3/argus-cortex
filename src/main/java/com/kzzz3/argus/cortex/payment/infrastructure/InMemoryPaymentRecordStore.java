package com.kzzz3.argus.cortex.payment.infrastructure;

import com.kzzz3.argus.cortex.payment.domain.PaymentRecord;
import com.kzzz3.argus.cortex.payment.domain.PaymentRecordStore;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryPaymentRecordStore implements PaymentRecordStore {

	private final Map<String, PaymentRecord> recordsBySessionId = new ConcurrentHashMap<>();

	@Override
	public PaymentRecord save(PaymentRecord paymentRecord) {
		recordsBySessionId.put(paymentRecord.sessionId(), paymentRecord);
		return paymentRecord;
	}

	@Override
	public Optional<PaymentRecord> findBySessionId(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(recordsBySessionId.get(sessionId));
	}
}
