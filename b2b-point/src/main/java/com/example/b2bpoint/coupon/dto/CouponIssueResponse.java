package com.example.b2bpoint.coupon.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CouponIssueResponse {
    private final String message;

    @Builder
    public CouponIssueResponse(String message) {
        this.message = message;
    }
}