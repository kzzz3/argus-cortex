package com.kzzz3.argus.cortex.payment.domain;

import java.util.Optional;

public interface PaymentScanSessionStore {

	PaymentScanSession save(PaymentScanSession session);

	Optional<PaymentScanSession> findBySessionId(String sessionId);
}
