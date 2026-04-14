package com.kzzz3.argus.cortex.payment.domain;

import java.util.Optional;

public interface PaymentRecordStore {

	PaymentRecord save(PaymentRecord paymentRecord);

	Optional<PaymentRecord> findBySessionId(String sessionId);
}
