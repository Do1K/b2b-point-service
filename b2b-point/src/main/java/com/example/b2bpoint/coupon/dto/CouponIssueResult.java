package com.example.b2bpoint.coupon.dto;

import com.example.b2bpoint.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class CouponIssueResult {

    private final boolean isSuccess;
    private final CouponIssueResponse data; // 성공 시 데이터
    private final ErrorCode errorCode;      // 실패 시 에러 코드

    private CouponIssueResult(boolean isSuccess, CouponIssueResponse data, ErrorCode errorCode) {
        this.isSuccess = isSuccess;
        this.data = data;
        this.errorCode = errorCode;
    }

    public static CouponIssueResult success(CouponIssueResponse data) {
        return new CouponIssueResult(true, data, null);
    }

    public static CouponIssueResult fail(ErrorCode errorCode) {
        return new CouponIssueResult(false, null, errorCode);
    }
}