package com.kzzz3.argus.cortex.payment.domain;

public class PaymentScanSessionNotFoundException extends RuntimeException {

	public PaymentScanSessionNotFoundException(String sessionId) {
		super("Payment scan session not found: " + sessionId);
	}
}
