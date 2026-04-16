package com.kzzz3.argus.cortex.payment.web;

import com.kzzz3.argus.cortex.payment.application.ConfirmPaymentCommand;
import com.kzzz3.argus.cortex.payment.application.PaymentApplicationService;
import com.kzzz3.argus.cortex.payment.application.ResolvePaymentScanCommand;
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

	@GetMapping("/wallet")
	public WalletSummaryResponse getWalletSummary() {
		return paymentApplicationService.getWalletSummary();
	}

	@GetMapping
	public List<PaymentHistoryItemResponse> listPayments() {
		return paymentApplicationService.listPayments().stream()
				.map(PaymentHistoryItemResponse::from)
				.toList();
	}

	@GetMapping("/{paymentId}")
	public ConfirmPaymentResponse getPaymentReceipt(
			@PathVariable String paymentId
	) {
		return ConfirmPaymentResponse.from(paymentApplicationService.getPaymentReceipt(paymentId));
	}

	@PostMapping("/scan-sessions/resolve")
	public ResolvePaymentScanResponse resolveScanSession(
			@Valid @RequestBody ResolvePaymentScanRequest request
	) {
		return ResolvePaymentScanResponse.from(paymentApplicationService.resolveScan(new ResolvePaymentScanCommand(request.scanPayload())));
	}

	@PostMapping("/scan-sessions/{sessionId}/confirm")
	public ConfirmPaymentResponse confirmPayment(
			@PathVariable String sessionId,
			@Valid @RequestBody ConfirmPaymentRequest request
	) {
		return ConfirmPaymentResponse.from(paymentApplicationService.confirmPayment(sessionId, new ConfirmPaymentCommand(request.amount(), request.note())));
	}
}
