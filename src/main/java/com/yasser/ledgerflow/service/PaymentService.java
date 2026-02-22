package com.yasser.ledgerflow.service;

import java.time.Instant;
import java.util.UUID;

import com.yasser.ledgerflow.model.IdempotencyKey;
import com.yasser.ledgerflow.model.IdempotencyStatus;
import com.yasser.ledgerflow.model.Payment;
import com.yasser.ledgerflow.model.PaymentStatus;
import com.yasser.ledgerflow.repository.IdempotencyKeyRepository;
import com.yasser.ledgerflow.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final LedgerService ledgerService;

    public PaymentService(PaymentRepository paymentRepository,
                          IdempotencyKeyRepository idempotencyKeyRepository,
                          LedgerService ledgerService) {
        this.paymentRepository = paymentRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.ledgerService = ledgerService;
    }

    // --------------------
    // CREATE PAYMENT
    // --------------------
    @Transactional
    public Payment createPayment(UUID merchantId, Long amount, String currency, String idempotencyKeyValue) {
        IdempotencyKey key = getOrCreateIdempotencyKey(merchantId, idempotencyKeyValue);

        if (key.getStatus() == IdempotencyStatus.COMPLETED) {
            return paymentRepository.findById(UUID.fromString(key.getResponsePayload()))
                    .orElseThrow(() -> new IllegalStateException("Previously completed payment not found"));
        }

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.INITIATED)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Update idempotency key
        key.setStatus(IdempotencyStatus.COMPLETED);
        key.setResponsePayload(savedPayment.getId().toString());
        idempotencyKeyRepository.save(key);

        return savedPayment;
    }

    // --------------------
    // COMPLETE PAYMENT
    // --------------------
    @Transactional
    public Payment completePayment(UUID paymentId, String idempotencyKeyValue) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment not found"));

        IdempotencyKey key = getOrCreateIdempotencyKey(payment.getMerchantId(), idempotencyKeyValue);

        if (key.getStatus() == IdempotencyStatus.COMPLETED) {
            return payment; // already completed
        }

        if (payment.getStatus() != PaymentStatus.INITIATED) {
            throw new IllegalStateException("Payment is not in PENDING state");
        }

        // Update payment status
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);

        // Add Ledger entry here
        UUID platformCashAccountId = ledgerService.getAccountIdByName("PLATFORM_CASH");
        UUID merchantPayableAccountId = ledgerService.getAccountIdByName("MERCHANT_PAYABLE");

        ledgerService.postTransaction(
                payment.getId(),
                platformCashAccountId,    // debit
                merchantPayableAccountId, // credit
                payment.getAmount()
        );

        // Mark idempotency key as completed
        key.setStatus(IdempotencyStatus.COMPLETED);
        key.setResponsePayload(payment.getId().toString());
        idempotencyKeyRepository.save(key);

        return payment;
    }

    // --------------------
    // CANCEL PAYMENT
    // --------------------
    @Transactional
    public Payment cancelPayment(UUID paymentId, String idempotencyKeyValue) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment not found"));

        IdempotencyKey key = getOrCreateIdempotencyKey(payment.getMerchantId(), idempotencyKeyValue);

        if (key.getStatus() == IdempotencyStatus.COMPLETED) {
            return payment; // already canceled or completed
        }

        if (payment.getStatus() != PaymentStatus.INITIATED) {
            throw new IllegalStateException("Only PENDING payments can be canceled");
        }

        payment.setStatus(PaymentStatus.CANCELED);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);

        key.setStatus(IdempotencyStatus.COMPLETED);
        key.setResponsePayload(payment.getId().toString());
        idempotencyKeyRepository.save(key);

        return payment;
    }

    // --------------------
    // Helper
    // --------------------
    private IdempotencyKey getOrCreateIdempotencyKey(UUID merchantId, String keyValue) {
        return idempotencyKeyRepository
                .findByMerchantIdAndIdempotencyKey(merchantId, keyValue)
                .orElseGet(() -> {
                    IdempotencyKey key = new IdempotencyKey();
                    key.setId(UUID.randomUUID());
                    key.setMerchantId(merchantId);
                    key.setIdempotencyKey(keyValue);
                    key.setStatus(IdempotencyStatus.IN_PROGRESS);
                    key.setCreatedAt(Instant.now());
                    return idempotencyKeyRepository.save(key);
                });
    }
}
