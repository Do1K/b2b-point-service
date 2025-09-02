package com.example.b2bpoint.coupon.dto;

import com.example.b2bpoint.common.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CouponIssueResponse {
    private final boolean isSuccess;
    private final String message;
    private final ErrorCode errorCode;


    private CouponIssueResponse(boolean isSuccess, String message, ErrorCode errorCode) {
        this.isSuccess = isSuccess;
        this.message = message;
        this.errorCode = errorCode;
    }

    public static CouponIssueResponse success(String message) {
        return new CouponIssueResponse(true, message, null);
    }

    public static CouponIssueResponse fail(ErrorCode errorCode) {
        return new CouponIssueResponse(false, null, errorCode);
    }
}