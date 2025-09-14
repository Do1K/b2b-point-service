package com.example.b2bpoint.coupon.domain;

import com.example.b2bpoint.common.domain.BaseEntity;
import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(length = 255, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType couponType;

    @Column(nullable = false, precision = 19, scale = 2) // 금액/비율을 위한 정밀도 설정
    private BigDecimal discountValue;

    private Integer maxDiscountAmount;

    @Column(nullable = false)
    private Integer minOrderAmount;

    private Integer totalQuantity; // NULL 가능 (무제한 쿠폰)

    @Column(nullable = false)
    private Integer issuedQuantity;


    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;


    // === 생성자 ===
    @Builder
    public CouponTemplate(Long partnerId, String name, CouponType couponType,
                          BigDecimal discountValue, Integer maxDiscountAmount, Integer minOrderAmount,
                          Integer totalQuantity, LocalDateTime validFrom, LocalDateTime validUntil) {
        this.partnerId = partnerId;
        this.name = name;
        this.couponType = couponType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0; // 최초 생성 시 발급 수량은 0
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }


    public void increaseIssuedQuantity() {
        if (this.totalQuantity != null) {
            if (this.issuedQuantity >= this.totalQuantity) {
                throw new CustomException(ErrorCode.COUPON_ISSUE_QUANTITY_EXCEEDED);
            }
        }
        this.issuedQuantity++;
    }
}