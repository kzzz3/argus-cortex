package com.kzzz3.argus.cortex.payment.web;

import jakarta.validation.constraints.NotBlank;

public record ResolvePaymentScanRequest(
		@NotBlank(message = "Scan payload is required.") String scanPayload
) {
}
