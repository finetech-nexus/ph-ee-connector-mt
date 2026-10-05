package org.mifos.connector.mt.api;

import jakarta.validation.Valid;
import org.mifos.connector.mt.model.MtAuthorizationRequest;
import org.mifos.connector.mt.model.MtCaptureRequest;
import org.mifos.connector.mt.model.MtPaymentStatus;
import org.mifos.connector.mt.model.MtRefundRequest;
import org.mifos.connector.mt.model.MtVoidRequest;
import org.mifos.connector.mt.service.MtPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mt")
public class MtApiController {
    private final MtPaymentService service;
    public MtApiController(MtPaymentService service) { this.service = service; }

    @PostMapping("/authorizations")
    public ResponseEntity<MtPaymentStatus> authorize(@Valid @RequestBody MtAuthorizationRequest request) {
        return ResponseEntity.accepted().body(service.authorize(request));
    }

    @PostMapping("/captures")
    public ResponseEntity<MtPaymentStatus> capture(@Valid @RequestBody MtCaptureRequest request) {
        return ResponseEntity.accepted().body(service.capture(request));
    }

    @PostMapping("/refunds")
    public ResponseEntity<MtPaymentStatus> refund(@Valid @RequestBody MtRefundRequest request) {
        return ResponseEntity.accepted().body(service.refund(request));
    }

    @PostMapping("/voids")
    public ResponseEntity<MtPaymentStatus> voidTransaction(@Valid @RequestBody MtVoidRequest request) {
        return ResponseEntity.accepted().body(service.voidTransaction(request));
    }

    @GetMapping("/transactions/{id}")
    public MtPaymentStatus status(@PathVariable String id) { return service.status(id); }

    @PostMapping("/callbacks")
    public MtPaymentStatus callback(@RequestBody MtPaymentStatus body) { return service.acceptCallback(body); }
}
