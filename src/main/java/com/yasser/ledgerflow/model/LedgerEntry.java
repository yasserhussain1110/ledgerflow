package com.yasser.ledgerflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LedgerEntry {
    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "debit_amount")
    private Long debitAmount;

    @Column(name = "credit_amount")
    private Long creditAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
