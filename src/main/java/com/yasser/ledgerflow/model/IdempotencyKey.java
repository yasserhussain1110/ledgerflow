package com.yasser.ledgerflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

import static com.yasser.ledgerflow.model.IdempotencyStatus.COMPLETED;
import static com.yasser.ledgerflow.model.IdempotencyStatus.FAILED;

@Entity
@Table(
        name = "idempotency_keys",
        indexes = {
                @Index(name = "ux_idempotency_merchant_key", columnList = "merchant_id,idempotency_key", unique = true),
                @Index(name = "idx_idempotency_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 255)
    private String requestHash;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private IdempotencyStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public void markCompleted(String responsePayload) {
        this.status = COMPLETED;
        this.responsePayload = responsePayload;
    }

    public void markFailed(String responsePayload) {
        this.status = FAILED;
        this.responsePayload = responsePayload;
    }
}
