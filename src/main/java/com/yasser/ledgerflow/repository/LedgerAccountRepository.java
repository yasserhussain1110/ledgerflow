package com.yasser.ledgerflow.repository;

import com.yasser.ledgerflow.domain.LedgerAccount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {
    Optional<LedgerAccount> findByName(String name);
}
