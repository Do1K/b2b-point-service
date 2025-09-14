package com.example.b2bpoint.point.domain;

import com.example.b2bpoint.common.domain.BaseEntity;
import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_wallets",uniqueConstraints = @UniqueConstraint(columnNames = {"partner_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointWallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private int points;


    @Builder
    private PointWallet(Long partnerId, String userId) {
        this.partnerId = partnerId;
        this.userId = userId;
        this.points = 0;
    }

    public static PointWallet create(Long partnerId, String userId) {
        return new PointWallet(partnerId, userId);
    }

    public void earn(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("적립할 포인트는 0보다 커야 합니다.");
        }
        this.points += amount;
    }

    public void use(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용할 포인트는 0보타 커야 합니다.");
        }
        if(this.points < amount) {
            throw new CustomException(ErrorCode.INSUFFICIENT_POINTS);
        }
        this.points -= amount;
    }
}
