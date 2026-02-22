package com.yasser.ledgerflow.service;

import com.yasser.ledgerflow.domain.Payment;
import com.yasser.ledgerflow.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.yasser.ledgerflow.domain.PaymentStatus.INITIATED;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LedgerService ledgerService;

    public PaymentService(PaymentRepository paymentRepository,
                          LedgerService ledgerService) {
        this.paymentRepository = paymentRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public Payment createPayment(UUID merchantId,
                                 BigDecimal amount,
                                 String currency) {

        Payment payment = new Payment(
                UUID.randomUUID(),
                merchantId,
                amount,
                currency,
                INITIATED,
                Instant.now(),
                Instant.now()
        );

        paymentRepository.save(payment);

        ledgerService.postPaymentEntries(payment.getId(), amount);

        return payment;
    }
}
