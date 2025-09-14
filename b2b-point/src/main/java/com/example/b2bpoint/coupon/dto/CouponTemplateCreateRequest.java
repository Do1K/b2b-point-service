package com.example.b2bpoint.coupon.dto;

import com.example.b2bpoint.coupon.domain.CouponType;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CouponTemplateCreateRequest {

    @NotBlank
    private String name;

    @NotNull
    private CouponType couponType;
    @NotNull @Positive private BigDecimal discountValue;
    @Positive private Integer maxDiscountAmount;
    @NotNull @PositiveOrZero private Integer minOrderAmount;
    @Positive private Integer totalQuantity;
    @NotNull @Future private LocalDateTime validFrom;
    @NotNull @Future private LocalDateTime validUntil;

    @Builder
    public CouponTemplateCreateRequest(String name, CouponType couponType,  BigDecimal discountValue, Integer maxDiscountAmount,
                                       Integer minOrderAmount, Integer totalQuantity, LocalDateTime validFrom, LocalDateTime validUntil) {
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
