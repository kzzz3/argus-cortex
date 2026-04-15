package com.kzzz3.argus.cortex.payment.domain;

public class PaymentRecordNotFoundException extends RuntimeException {

	public PaymentRecordNotFoundException(String paymentId) {
		super("Payment record not found: " + paymentId);
	}
}
