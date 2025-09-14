package com.example.b2bpoint.coupon.dto;

import com.example.b2bpoint.common.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CouponIssueResponse {
    private final String message;

    @Builder
    private CouponIssueResponse(String message) {
        this.message = message;
    }


}