package com.yasser.ledgerflow.repository;

import com.yasser.ledgerflow.domain.LedgerAccount;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {
    Optional<LedgerAccount> findByName(String name);
}
