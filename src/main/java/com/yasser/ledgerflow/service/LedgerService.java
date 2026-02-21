package com.yasser.ledgerflow.service;

import com.yasser.ledgerflow.domain.EntryType;
import com.yasser.ledgerflow.domain.LedgerAccount;
import com.yasser.ledgerflow.domain.LedgerEntry;
import com.yasser.ledgerflow.domain.Payment;
import com.yasser.ledgerflow.repository.LedgerAccountRepository;
import com.yasser.ledgerflow.repository.LedgerEntryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;

    @Transactional
    public void postPaymentEntries(Payment payment) {

        LedgerAccount platformAccount =
                accountRepository.findByName("PLATFORM_CASH")
                        .orElseThrow(() -> new IllegalStateException("PLATFORM_CASH account missing"));

        LedgerAccount merchantAccount =
                accountRepository.findByName("MERCHANT_BALANCE")
                        .orElseThrow(() -> new IllegalStateException("MERCHANT_BALANCE account missing"));

        BigDecimal amount = payment.getAmount();

        LedgerEntry debit = LedgerEntry.builder()
                .account(platformAccount)
                .payment(payment)
                .type(EntryType.DEBIT)
                .amount(amount)
                .createdAt(Instant.now())
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .account(merchantAccount)
                .payment(payment)
                .type(EntryType.CREDIT)
                .amount(amount)
                .createdAt(Instant.now())
                .build();

        entryRepository.save(debit);
        entryRepository.save(credit);
    }
}
