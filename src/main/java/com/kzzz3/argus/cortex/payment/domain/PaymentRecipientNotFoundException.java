package com.kzzz3.argus.cortex.payment.domain;

public class PaymentRecipientNotFoundException extends RuntimeException {

	public PaymentRecipientNotFoundException(String recipientAccountId) {
		super("Recipient account was not found: " + recipientAccountId);
	}
}
