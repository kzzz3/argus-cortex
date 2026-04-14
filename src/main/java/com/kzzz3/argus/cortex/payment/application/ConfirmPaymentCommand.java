package com.kzzz3.argus.cortex.payment.application;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record ConfirmPaymentCommand(@Nullable BigDecimal amount, @Nullable String note) {
}
