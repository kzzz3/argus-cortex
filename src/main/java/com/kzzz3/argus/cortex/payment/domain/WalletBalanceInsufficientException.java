package com.kzzz3.argus.cortex.payment.domain;

public class WalletBalanceInsufficientException extends RuntimeException {

	public WalletBalanceInsufficientException(String accountId) {
		super("Wallet balance is insufficient for account: " + accountId);
	}
}
