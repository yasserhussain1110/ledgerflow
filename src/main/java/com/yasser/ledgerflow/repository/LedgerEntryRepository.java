package com.yasser.ledgerflow.repository;

import com.yasser.ledgerflow.domain.LedgerEntry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
}
