package com.yasser.ledgerflow.repository;

import com.yasser.ledgerflow.model.LedgerEntry;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("""
    SELECT COALESCE(SUM(e.creditAmount - e.debitAmount), 0)
    FROM LedgerEntry e
    WHERE e.accountId = :accountId
    """)
    Long calculateBalance(@Param("accountId") UUID accountId);
}
