package com.example.b2bpoint.point.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_wallet_id", nullable = false)
    private PointWallet pointWallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false)
    private int amount;

    private String description;

    @Column(name = "partner_order_id")
    private String partnerOrderId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public PointHistory(PointWallet pointWallet, TransactionType transactionType, int amount, String description, String partnerOrderId) {
        this.pointWallet = pointWallet;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.partnerOrderId = partnerOrderId;
    }
}
