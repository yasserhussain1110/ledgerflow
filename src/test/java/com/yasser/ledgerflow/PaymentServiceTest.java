package com.yasser.ledgerflow;

import com.yasser.ledgerflow.model.Payment;
import com.yasser.ledgerflow.model.PaymentStatus;
import com.yasser.ledgerflow.model.IdempotencyKey;
import com.yasser.ledgerflow.model.IdempotencyStatus;
import com.yasser.ledgerflow.repository.IdempotencyKeyRepository;
import com.yasser.ledgerflow.repository.PaymentRepository;
import com.yasser.ledgerflow.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private UUID merchantId;

    @BeforeEach
    void setUp() {
        // use a fixed merchant for testing
        merchantId = UUID.randomUUID();
    }

    @Test
    void testCreatePayment_withIdempotency() {
        String idempotencyKey = "key-123";

        Payment payment1 = paymentService.createPayment(merchantId, 1000L, "USD", idempotencyKey);
        Payment payment2 = paymentService.createPayment(merchantId, 1000L, "USD", idempotencyKey);

        // should return the same payment for repeated key
        assertThat(payment2.getId()).isEqualTo(payment1.getId());

        // idempotency key should be marked completed
        IdempotencyKey key = idempotencyKeyRepository
                .findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey)
                .orElseThrow();
        assertThat(key.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(key.getResponsePayload()).isEqualTo(payment1.getId().toString());
    }

    @Test
    void testCompletePayment() {
        Payment payment = paymentService.createPayment(merchantId, 500L, "USD", "key-complete");

        Payment completed = paymentService.completePayment(payment.getId(), "key-complete-2");

        assertThat(completed.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    void testCancelPayment() {
        Payment payment = paymentService.createPayment(merchantId, 500L, "USD", "key-cancel");

        Payment canceled = paymentService.cancelPayment(payment.getId(), "key-cancel-2");

        assertThat(canceled.getStatus()).isEqualTo(PaymentStatus.CANCELED);

        // trying to complete canceled payment should throw
        assertThrows(IllegalStateException.class, () ->
                paymentService.completePayment(canceled.getId(), "key-cancel-3"));
    }
}
