package com.kzzz3.argus.cortex.payment.web;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record ConfirmPaymentRequest(
		@DecimalMin(value = "0.01", message = "Payment amount must be at least 0.01.")
		@Digits(integer = 10, fraction = 2, message = "Payment amount must use up to two decimal places.")
		BigDecimal amount,
		String note
) {
}
