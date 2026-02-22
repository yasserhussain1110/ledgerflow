package com.yasser.ledgerflow.service;

import java.util.UUID;

import com.yasser.ledgerflow.model.IdempotencyKey;
import com.yasser.ledgerflow.model.Payment;
import com.yasser.ledgerflow.repository.IdempotencyKeyRepository;
import com.yasser.ledgerflow.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import static com.yasser.ledgerflow.model.IdempotencyStatus.COMPLETED;
import static com.yasser.ledgerflow.model.IdempotencyStatus.IN_PROGRESS;
import static com.yasser.ledgerflow.model.PaymentStatus.CANCELED;
import static com.yasser.ledgerflow.model.PaymentStatus.CAPTURED;
import static com.yasser.ledgerflow.model.PaymentStatus.INITIATED;
import static java.time.Instant.now;

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

        if (key.getStatus() == COMPLETED) {
            return paymentRepository.findById(UUID.fromString(key.getResponsePayload()))
                    .orElseThrow(() -> new IllegalStateException("Previously completed payment not found"));
        }

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .amount(amount)
                .currency(currency)
                .status(INITIATED)
                .version(0L)
                .createdAt(now())
                .updatedAt(now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Update idempotency key
        key.setStatus(COMPLETED);
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

        if (key.getStatus() == COMPLETED) {
            return payment; // already completed
        }

        if (payment.getStatus() != INITIATED) {
            throw new IllegalStateException("Payment is not in PENDING state");
        }

        // Update payment status
        payment.setStatus(CAPTURED);
        payment.setUpdatedAt(now());
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
        key.setStatus(COMPLETED);
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

        if (key.getStatus() == COMPLETED) {
            return payment; // already canceled or completed
        }

        if (payment.getStatus() != INITIATED) {
            throw new IllegalStateException("Only PENDING payments can be canceled");
        }

        payment.setStatus(CANCELED);
        payment.setUpdatedAt(now());
        paymentRepository.save(payment);

        key.setStatus(COMPLETED);
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
                    key.setStatus(IN_PROGRESS);
                    key.setCreatedAt(now());
                    key.setRequestHash(keyValue);
                    return idempotencyKeyRepository.save(key);
                });
    }
}
