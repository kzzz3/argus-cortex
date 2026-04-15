package com.kzzz3.argus.cortex.payment.infrastructure;

import com.kzzz3.argus.cortex.payment.domain.PaymentRecord;
import com.kzzz3.argus.cortex.payment.domain.PaymentRecordStore;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryPaymentRecordStore implements PaymentRecordStore {

	private final Map<String, PaymentRecord> recordsBySessionId = new ConcurrentHashMap<>();
	private final Map<String, PaymentRecord> recordsByPaymentId = new ConcurrentHashMap<>();

	@Override
	public PaymentRecord save(PaymentRecord paymentRecord) {
		recordsBySessionId.put(paymentRecord.sessionId(), paymentRecord);
		recordsByPaymentId.put(paymentRecord.paymentId(), paymentRecord);
		return paymentRecord;
	}

	@Override
	public Optional<PaymentRecord> findBySessionId(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(recordsBySessionId.get(sessionId));
	}

	@Override
	public List<PaymentRecord> listByPayerAccountId(String payerAccountId) {
		return recordsByPaymentId.values().stream()
				.filter(record -> record.payerAccountId().equals(payerAccountId))
				.sorted(Comparator.comparing(PaymentRecord::createdAt).reversed())
				.toList();
	}

	@Override
	public Optional<PaymentRecord> findByPaymentIdAndPayerAccountId(String paymentId, String payerAccountId) {
		PaymentRecord record = recordsByPaymentId.get(paymentId);
		if (record == null || !record.payerAccountId().equals(payerAccountId)) {
			return Optional.empty();
		}
		return Optional.of(record);
	}
}
