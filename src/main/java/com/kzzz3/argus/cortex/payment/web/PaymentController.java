package com.kzzz3.argus.cortex.payment.web;

import com.kzzz3.argus.cortex.payment.application.ConfirmPaymentCommand;
import com.kzzz3.argus.cortex.payment.application.PaymentApplicationService;
import com.kzzz3.argus.cortex.payment.application.ResolvePaymentScanCommand;
import com.kzzz3.argus.cortex.shared.web.BearerTokenExtractor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

	private final PaymentApplicationService paymentApplicationService;

	public PaymentController(PaymentApplicationService paymentApplicationService) {
		this.paymentApplicationService = paymentApplicationService;
	}

	@GetMapping
	public List<PaymentHistoryItemResponse> listPayments(
			@RequestHeader("Authorization") String authorizationHeader
	) {
		return paymentApplicationService.listPayments(BearerTokenExtractor.extract(authorizationHeader)).stream()
				.map(PaymentHistoryItemResponse::from)
				.toList();
	}

	@GetMapping("/{paymentId}")
	public ConfirmPaymentResponse getPaymentReceipt(
			@PathVariable String paymentId,
			@RequestHeader("Authorization") String authorizationHeader
	) {
		return ConfirmPaymentResponse.from(paymentApplicationService.getPaymentReceipt(
				BearerTokenExtractor.extract(authorizationHeader),
				paymentId
		));
	}

	@PostMapping("/scan-sessions/resolve")
	public ResolvePaymentScanResponse resolveScanSession(
			@RequestHeader("Authorization") String authorizationHeader,
			@Valid @RequestBody ResolvePaymentScanRequest request
	) {
		return ResolvePaymentScanResponse.from(paymentApplicationService.resolveScan(
				BearerTokenExtractor.extract(authorizationHeader),
				new ResolvePaymentScanCommand(request.scanPayload())
		));
	}

	@PostMapping("/scan-sessions/{sessionId}/confirm")
	public ConfirmPaymentResponse confirmPayment(
			@PathVariable String sessionId,
			@RequestHeader("Authorization") String authorizationHeader,
			@Valid @RequestBody ConfirmPaymentRequest request
	) {
		return ConfirmPaymentResponse.from(paymentApplicationService.confirmPayment(
				BearerTokenExtractor.extract(authorizationHeader),
				sessionId,
				new ConfirmPaymentCommand(request.amount(), request.note())
		));
	}
}
