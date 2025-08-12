package com.example.b2bpoint.coupon.domain;


import com.example.b2bpoint.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupon_user_partner", columnList = "user_id, partner_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "partner_id", nullable = false, updatable = false)
    private Long partnerId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime expiredAt;

    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_template_id", nullable = false, updatable = false)
    private CouponTemplate couponTemplate;


    @Builder
    public Coupon(Long partnerId, String userId, CouponTemplate couponTemplate) {
        this.code = UUID.randomUUID().toString();
        this.partnerId = partnerId;
        this.userId = userId;
        this.couponTemplate = couponTemplate;
        this.status = CouponStatus.AVAILABLE;
        this.issuedAt = LocalDateTime.now();
        this.expiredAt = couponTemplate.getValidUntil();
    }

    public void use() {
        verifyCanBeUsed(); // 사용할 수 있는 상태인지 먼저 검증
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    private void verifyCanBeUsed() {
        if (this.status != CouponStatus.AVAILABLE) {
            throw new IllegalStateException("이미 사용되었거나 만료된 쿠폰입니다.");
        }
        if (LocalDateTime.now().isAfter(this.expiredAt)) {
            throw new IllegalStateException("유효기간이 만료된 쿠폰입니다.");
        }
    }
}
