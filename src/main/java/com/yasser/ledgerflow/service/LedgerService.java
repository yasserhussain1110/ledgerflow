package com.yasser.ledgerflow.service;

import com.yasser.ledgerflow.model.LedgerAccount;
import com.yasser.ledgerflow.model.LedgerEntry;
import com.yasser.ledgerflow.repository.LedgerAccountRepository;
import com.yasser.ledgerflow.repository.LedgerEntryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;

    public LedgerService(LedgerAccountRepository accountRepository,
                         LedgerEntryRepository entryRepository) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
    }

    /**
     * Posts a transaction: debit from source account, credit to destination account
     */
    @Transactional
    public List<LedgerEntry> postTransaction(UUID transactionId, UUID debitAccountId, UUID creditAccountId, Long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");

        Instant now = Instant.now();

        LedgerEntry debitEntry = new LedgerEntry();
        debitEntry.setId(UUID.randomUUID());
        debitEntry.setTransactionId(transactionId);
        debitEntry.setAccountId(debitAccountId);
        debitEntry.setDebitAmount(amount);
        debitEntry.setCreditAmount(0L);
        debitEntry.setCreatedAt(now);

        LedgerEntry creditEntry = new LedgerEntry();
        creditEntry.setId(UUID.randomUUID());
        creditEntry.setTransactionId(transactionId);
        creditEntry.setAccountId(creditAccountId);
        creditEntry.setDebitAmount(0L);
        creditEntry.setCreditAmount(amount);
        creditEntry.setCreatedAt(now);

        entryRepository.saveAll(Arrays.asList(debitEntry, creditEntry));

        return Arrays.asList(debitEntry, creditEntry);
    }

    /**
     * Convenience method for platform → merchant payment
     */
    @Transactional
    public List<LedgerEntry> postPayment(UUID transactionId, Long amount) {
        LedgerAccount platformCash = accountRepository.findByName("PLATFORM_CASH")
                .orElseThrow(() -> new IllegalStateException("Platform account missing"));

        LedgerAccount merchantPayable = accountRepository.findByName("MERCHANT_PAYABLE")
                .orElseThrow(() -> new IllegalStateException("Merchant payable account missing"));

        return postTransaction(transactionId, platformCash.getId(), merchantPayable.getId(), amount);
    }
}
