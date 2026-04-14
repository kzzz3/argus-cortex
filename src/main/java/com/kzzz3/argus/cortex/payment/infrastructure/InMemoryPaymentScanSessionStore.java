package com.kzzz3.argus.cortex.payment.infrastructure;

import com.kzzz3.argus.cortex.payment.domain.PaymentScanSession;
import com.kzzz3.argus.cortex.payment.domain.PaymentScanSessionStore;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argus.persistence.mode", havingValue = "memory")
public class InMemoryPaymentScanSessionStore implements PaymentScanSessionStore {

	private final Map<String, PaymentScanSession> sessions = new ConcurrentHashMap<>();

	@Override
	public PaymentScanSession save(PaymentScanSession session) {
		sessions.put(session.sessionId(), session);
		return session;
	}

	@Override
	public Optional<PaymentScanSession> findBySessionId(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(sessions.get(sessionId));
	}
}
