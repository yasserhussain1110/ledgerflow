package com.yasser.ledgerflow.service;

import com.yasser.ledgerflow.domain.Payment;
import com.yasser.ledgerflow.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LedgerService ledgerService;

    @Transactional
    public Payment createPayment(Payment payment) {

        payment.setCreatedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);

        ledgerService.postPaymentEntries(savedPayment);

        return savedPayment;
    }
}
