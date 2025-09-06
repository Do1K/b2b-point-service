package com.example.b2bpoint.coupon.domain;


import com.example.b2bpoint.common.domain.BaseEntity;
import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
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
    @JoinColumn(name = "coupon_template_id", insertable = false, updatable = false)
    private CouponTemplate couponTemplate;

    @Column(name = "coupon_template_id")
    private Long couponTemplateId;




    @Builder
    public Coupon(Long partnerId, String userId, CouponTemplate couponTemplate) {
        this.code = UUID.randomUUID().toString();
        this.partnerId = partnerId;
        this.userId = userId;
        this.couponTemplate = couponTemplate;
        if (couponTemplate != null) {
            this.couponTemplateId = couponTemplate.getId();
            this.expiredAt = couponTemplate.getValidUntil();
        }
        this.status = CouponStatus.AVAILABLE;
        this.issuedAt = LocalDateTime.now();

    }

    @Builder(builderMethodName = "issueBuilder")
    public Coupon(Long partnerId, String userId, Long couponTemplateId, LocalDateTime validUntil) {
        this.code = UUID.randomUUID().toString();
        this.partnerId = partnerId;
        this.userId = userId;
        this.couponTemplateId = couponTemplateId; // ID를 직접 할당
        this.status = CouponStatus.AVAILABLE;
        this.issuedAt = LocalDateTime.now();
        this.expiredAt = validUntil;
    }

    private Coupon(Long partnerId, String userId, CouponTemplate couponTemplate, Long couponTemplateId, LocalDateTime expiredAt) {
        this.code = UUID.randomUUID().toString();
        this.partnerId = partnerId;
        this.userId = userId;
        this.couponTemplate = couponTemplate;
        this.couponTemplateId = couponTemplateId;
        this.status = CouponStatus.AVAILABLE;
        this.issuedAt = LocalDateTime.now();
        this.expiredAt = expiredAt;
    }

    public static Coupon createFromMessage(Long partnerId, String userId, Long couponTemplateId, LocalDateTime expiredAt) {
        if (couponTemplateId == null || expiredAt == null) {
            throw new IllegalArgumentException("couponTemplateId and expiredAt cannot be null");
        }
        // couponTemplate 객체는 null로 두고, ID와 만료일만 직접 설정
        return new Coupon(partnerId, userId, null, couponTemplateId, expiredAt);
    }


    public void use(Long requestPartnerId, String requestUserId) {
        verifyCanBeUsed(requestPartnerId, requestUserId); // 사용할 수 있는 상태인지 먼저 검증
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    private void verifyCanBeUsed(Long requestPartnerId, String requestUserId) {
        if (!this.partnerId.equals(requestPartnerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }
        if (!this.userId.equals(requestUserId)) {
            throw new CustomException(ErrorCode.COUPON_OWNER_MISMATCH);
        }

        if (this.status != CouponStatus.AVAILABLE) {
            throw new IllegalStateException("이미 사용되었거나 만료된 쿠폰입니다.");
        }
        if (LocalDateTime.now().isAfter(this.expiredAt)) {
            throw new IllegalStateException("유효기간이 만료된 쿠폰입니다.");
        }
    }
}
