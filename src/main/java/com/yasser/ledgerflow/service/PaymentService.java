package com.yasser.ledgerflow.service;

import com.yasser.ledgerflow.model.IdempotencyKey;
import com.yasser.ledgerflow.model.Payment;
import com.yasser.ledgerflow.model.PaymentStatus;
import com.yasser.ledgerflow.repository.IdempotencyKeyRepository;
import com.yasser.ledgerflow.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LedgerService ledgerService;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          LedgerService ledgerService,
                          IdempotencyKeyRepository idempotencyKeyRepository) {
        this.paymentRepository = paymentRepository;
        this.ledgerService = ledgerService;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional
    public Payment createPayment(UUID merchantId,
                                 Long amount,
                                 String currency,
                                 String idempotencyKeyValue) {

        // 1. Check if idempotency key already exists
        IdempotencyKey existingKey = idempotencyKeyRepository
                .findByIdempotencyKey(idempotencyKeyValue)
                .orElse(null);

        if (existingKey != null) {
            // In real system you'd deserialize stored response
            // For now, just return existing payment
            return paymentRepository.findById(UUID.fromString(existingKey.getResponsePayload()))
                    .orElseThrow(() -> new IllegalStateException("Stored payment not found"));
        }

        // 2. Create payment
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setMerchantId(merchantId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.INITIATED);

        Payment savedPayment = paymentRepository.save(payment);

        // 3. Store idempotency record
//        IdempotencyKey idempotencyKey = new IdempotencyKey();
//        idempotencyKey.setId(UUID.randomUUID());
//        idempotencyKey.setIdempotencyKey(idempotencyKeyValue);
//        idempotencyKey.setRequestHash("simple-hash"); // placeholder
//        idempotencyKey.setResponseBody(savedPayment.getId().toString());
//        idempotencyKey.setCreatedAt(java.time.Instant.now());
//
//        idempotencyKeyRepository.save(idempotencyKey);

        return savedPayment;
    }

    @Transactional
    public Payment capturePayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED &&
                payment.getStatus() != PaymentStatus.INITIATED) {
            throw new IllegalStateException("Invalid state transition");
        }

        ledgerService.postTransaction(payment.getId(), payment.getAmount());

        payment.setStatus(PaymentStatus.CAPTURED);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment failPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        payment.setStatus(PaymentStatus.FAILED);

        return paymentRepository.save(payment);
    }
}
