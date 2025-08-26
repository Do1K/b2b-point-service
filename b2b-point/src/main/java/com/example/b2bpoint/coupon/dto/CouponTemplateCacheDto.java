package com.example.b2bpoint.coupon.dto;

import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.domain.CouponType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CouponTemplateCacheDto {

    private Long id;
    private Long partnerId;
    private String name;
    private CouponType couponType;
    private BigDecimal discountValue;
    private Integer maxDiscountAmount;
    private Integer minOrderAmount;
    private Integer totalQuantity;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    // 엔티티를 DTO로 변환하기 위한 생성자 또는 정적 팩토리 메소드
    public static CouponTemplateCacheDto fromEntity(CouponTemplate entity) {
        CouponTemplateCacheDto dto = new CouponTemplateCacheDto();
        dto.id = entity.getId();
        dto.partnerId = entity.getPartnerId();
        dto.name = entity.getName();
        dto.couponType = entity.getCouponType();
        dto.discountValue = entity.getDiscountValue();
        dto.maxDiscountAmount = entity.getMaxDiscountAmount();
        dto.minOrderAmount = entity.getMinOrderAmount();
        dto.totalQuantity = entity.getTotalQuantity();
        dto.validFrom = entity.getValidFrom();
        dto.validUntil = entity.getValidUntil();
        return dto;
    }

    @Builder
    private CouponTemplateCacheDto(Long id, Long partnerId, String name, CouponType couponType,
                                   BigDecimal discountValue, Integer maxDiscountAmount, Integer minOrderAmount,
                                   Integer totalQuantity, LocalDateTime validFrom, LocalDateTime validUntil) {
        this.id = id;
        this.partnerId = partnerId;
        this.name = name;
        this.couponType = couponType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.totalQuantity = totalQuantity;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }


}