package com.example.b2bpoint.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssueRequest {

    @NotNull(message = "쿠폰 템플릿 ID는 필수입니다.")
    private Long couponTemplateId;

    @NotBlank(message = "사용자 ID는 필수입니다.")
    private String userId;

    @Builder
    public CouponIssueRequest(Long couponTemplateId, String userId) {
        this.couponTemplateId = couponTemplateId;
        this.userId = userId;
    }
}
