package com.example.b2bpoint.coupon.dto;

import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.domain.CouponType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class CouponTemplateResponse {

    private final Long id;
    private final Long partnerId;
    private final String name;
    private final CouponType couponType;
    private final BigDecimal discountValue;
    private final Integer maxDiscountAmount;
    private final Integer minOrderAmount;
    private final Integer totalQuantity;
    private final Integer issuedQuantity;
    private final LocalDateTime validFrom;
    private final LocalDateTime validUntil;
    private final LocalDateTime createdAt;

    @Builder
    private CouponTemplateResponse(Long id, Long partnerId, String name, CouponType couponType,
                                   BigDecimal discountValue, Integer maxDiscountAmount, Integer minOrderAmount,
                                   Integer totalQuantity, Integer issuedQuantity,
                                   LocalDateTime validFrom, LocalDateTime validUntil, LocalDateTime createdAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.name = name;
        this.couponType = couponType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.createdAt = createdAt;
    }

    public static CouponTemplateResponse from(CouponTemplate couponTemplate) {
        return CouponTemplateResponse.builder()
                .id(couponTemplate.getId())
                .partnerId(couponTemplate.getPartnerId())
                .name(couponTemplate.getName())
                .couponType(couponTemplate.getCouponType())
                .discountValue(couponTemplate.getDiscountValue())
                .maxDiscountAmount(couponTemplate.getMaxDiscountAmount())
                .minOrderAmount(couponTemplate.getMinOrderAmount())
                .totalQuantity(couponTemplate.getTotalQuantity())
                .issuedQuantity(couponTemplate.getIssuedQuantity())
                .validFrom(couponTemplate.getValidFrom())
                .validUntil(couponTemplate.getValidUntil())
                .createdAt(couponTemplate.getCreatedAt())
                .build();
    }
}