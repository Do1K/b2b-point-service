package com.example.b2bpoint.coupon.dto;

import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CouponUseResponse {

    private final String couponCode;
    private final CouponStatus status;
    private final LocalDateTime usedAt;

    @Builder
    private CouponUseResponse(String couponCode, CouponStatus status, LocalDateTime usedAt){
        this.couponCode = couponCode;
        this.status = status;
        this.usedAt = usedAt;
    }

    public static CouponUseResponse from(Coupon coupon) {
        return CouponUseResponse.builder()
                .couponCode(coupon.getCode())
                .status(coupon.getStatus())
                .usedAt(coupon.getUsedAt())
                .build();
    }
}
