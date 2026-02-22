package com.yasser.ledgerflow.service;

import com.yasser.ledgerflow.domain.LedgerAccount;
import com.yasser.ledgerflow.domain.LedgerEntry;
import com.yasser.ledgerflow.repository.LedgerAccountRepository;
import com.yasser.ledgerflow.repository.LedgerEntryRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerAccountRepository ledgerAccountRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository,
                         LedgerAccountRepository ledgerAccountRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
    }

    @Transactional
    public void postPaymentEntries(UUID paymentId,
                                   BigDecimal amount) {

        LedgerAccount platformCash =
                ledgerAccountRepository.findByName("PLATFORM_CASH")
                        .orElseThrow(() -> new IllegalStateException("Platform account missing"));

        LedgerAccount merchantBalance =
                ledgerAccountRepository.findByName("MERCHANT_BALANCE")
                        .orElseThrow(() -> new IllegalStateException("Merchant account missing"));

        LedgerEntry debitEntry = new LedgerEntry(
                UUID.randomUUID(),
                paymentId,
                platformCash,
                "DEBIT",
                amount,
                Instant.now()
        );

        LedgerEntry creditEntry = new LedgerEntry(
                UUID.randomUUID(),
                paymentId,
                merchantBalance,
                "CREDIT",
                amount,
                Instant.now()
        );

        validateBalanced(List.of(debitEntry, creditEntry));

        ledgerEntryRepository.saveAll(List.of(debitEntry, creditEntry));
    }

    private void validateBalanced(List<LedgerEntry> entries) {

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            if ("DEBIT".equals(entry.getType())) {
                totalDebits = totalDebits.add(entry.getAmount());
            } else if ("CREDIT".equals(entry.getType())) {
                totalCredits = totalCredits.add(entry.getAmount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalStateException("Ledger entries are not balanced");
        }
    }

    public BigDecimal getAccountBalance(UUID accountId) {
        return ledgerEntryRepository.calculateBalance(accountId);
    }
}
