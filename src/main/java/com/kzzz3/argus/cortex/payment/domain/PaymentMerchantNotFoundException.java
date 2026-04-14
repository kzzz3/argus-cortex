package com.kzzz3.argus.cortex.payment.domain;

public class PaymentMerchantNotFoundException extends RuntimeException {

	public PaymentMerchantNotFoundException(String merchantAccountId) {
		super("Merchant account not found: " + merchantAccountId);
	}
}
