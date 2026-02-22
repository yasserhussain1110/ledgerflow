package com.yasser.ledgerflow.service;

import java.time.Instant;
import java.util.UUID;

import com.yasser.ledgerflow.model.IdempotencyKey;
import com.yasser.ledgerflow.model.Payment;
import com.yasser.ledgerflow.model.IdempotencyStatus;
import com.yasser.ledgerflow.repository.IdempotencyKeyRepository;
import com.yasser.ledgerflow.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import static com.yasser.ledgerflow.model.PaymentStatus.INITIATED;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          IdempotencyKeyRepository idempotencyKeyRepository) {
        this.paymentRepository = paymentRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    /**
     * Create a payment with idempotency support
     */
    @Transactional
    public Payment createPayment(UUID merchantId, Long amount, String currency, String idempotencyKeyValue) {

        // Check if this idempotency key already exists
        IdempotencyKey existingKey = idempotencyKeyRepository
                .findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKeyValue)
                .orElse(null);

        if (existingKey != null) {
            if (existingKey.getStatus() == IdempotencyStatus.COMPLETED) {
                // Return previously created payment
                return paymentRepository.findById(UUID.fromString(existingKey.getResponsePayload()))
                        .orElseThrow(() -> new IllegalStateException("Previously completed payment not found"));
            } else if (existingKey.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                throw new IllegalStateException("Payment creation already in progress for this key");
            } else {
                // FAILED: we can retry
                existingKey.setStatus(IdempotencyStatus.IN_PROGRESS);
                idempotencyKeyRepository.save(existingKey);
            }
        } else {
            // Create new idempotency key
            existingKey = new IdempotencyKey();
            existingKey.setId(UUID.randomUUID());
            existingKey.setMerchantId(merchantId);
            existingKey.setIdempotencyKey(idempotencyKeyValue);
            existingKey.setStatus(IdempotencyStatus.IN_PROGRESS);
            existingKey.setCreatedAt(Instant.now());
            idempotencyKeyRepository.save(existingKey);
        }

        // Create the payment
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .amount(amount)
                .currency(currency)
                .status(INITIATED)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Update idempotency key with completed status and response payload
        existingKey.setStatus(IdempotencyStatus.COMPLETED);
        existingKey.setResponsePayload(savedPayment.getId().toString());
        idempotencyKeyRepository.save(existingKey);

        return savedPayment;
    }
}
