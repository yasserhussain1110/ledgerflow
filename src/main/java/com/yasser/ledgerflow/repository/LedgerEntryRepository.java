package com.yasser.ledgerflow.repository;

import com.yasser.ledgerflow.model.LedgerEntry;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("""
        SELECT COALESCE(
            SUM(
                CASE WHEN e.type = 'DEBIT' 
                     THEN e.amount 
                     ELSE -e.amount 
                END
            ), 0
        )
        FROM LedgerEntry e
        WHERE e.account.id = :accountId
    """)
    Long calculateBalance(@Param("accountId") UUID accountId);
}
