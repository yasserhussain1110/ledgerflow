package com.yasser.ledgerflow.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.yasser.ledgerflow.model.LedgerAccount;
import com.yasser.ledgerflow.model.LedgerEntry;
import com.yasser.ledgerflow.repository.LedgerAccountRepository;
import com.yasser.ledgerflow.repository.LedgerEntryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class LedgerService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;

    public LedgerService(LedgerAccountRepository accountRepository,
                         LedgerEntryRepository entryRepository) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional
    public void postTransaction(UUID transactionId, Long amount) {

        LedgerAccount platformCash = accountRepository.findByName("PLATFORM_CASH")
                .orElseThrow(() -> new IllegalStateException("Platform account missing"));

        LedgerAccount merchantPayable = accountRepository.findByName("MERCHANT_PAYABLE")
                .orElseThrow(() -> new IllegalStateException("Merchant payable account missing"));

        // Debit entry
        LedgerEntry debitEntry = new LedgerEntry();
        debitEntry.setId(UUID.randomUUID());
        debitEntry.setTransactionId(transactionId);
        debitEntry.setAccountId(platformCash.getId());
        debitEntry.setDebitAmount(amount);
        debitEntry.setCreditAmount(0L);
        debitEntry.setCreatedAt(Instant.now());

        // Credit entry
        LedgerEntry creditEntry = new LedgerEntry();
        creditEntry.setId(UUID.randomUUID());
        creditEntry.setTransactionId(transactionId);
        creditEntry.setAccountId(merchantPayable.getId());
        creditEntry.setDebitAmount(0L);
        creditEntry.setCreditAmount(amount);
        creditEntry.setCreatedAt(Instant.now());

        entryRepository.save(debitEntry);
        entryRepository.save(creditEntry);
    }
}
