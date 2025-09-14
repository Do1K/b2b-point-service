package com.example.b2bpoint.coupon.dto;


import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CouponResponse {

    private final Long couponId;          // 발급된 쿠폰의 고유 ID
    private final String couponCode;      // 발급된 쿠폰의 고유 코드
    private final String couponName;      // 어떤 종류의 쿠폰인지 (템플릿 이름)
    private final CouponStatus status;
    private final LocalDateTime issuedAt;
    private final LocalDateTime expiredAt;

    @Builder
    private CouponResponse(Long couponId, String couponCode, String couponName, CouponStatus status, LocalDateTime issuedAt, LocalDateTime expiredAt) {
        this.couponId = couponId;
        this.couponCode = couponCode;
        this.couponName = couponName;
        this.status = status;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
    }

    public static CouponResponse from(Coupon coupon) {
        return CouponResponse.builder()
                .couponId(coupon.getId())
                .couponCode(coupon.getCode())
                .couponName(coupon.getCouponTemplate().getName())
                .status(coupon.getStatus())
                .issuedAt(coupon.getIssuedAt())
                .expiredAt(coupon.getExpiredAt())
                .build();
    }
}
