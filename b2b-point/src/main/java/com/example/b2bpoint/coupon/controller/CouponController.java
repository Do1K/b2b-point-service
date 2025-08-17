package com.example.b2bpoint.coupon.controller;

import com.example.b2bpoint.common.dto.ApiResponse;
import com.example.b2bpoint.coupon.dto.CouponIssueRequest;
import com.example.b2bpoint.coupon.dto.CouponResponse;
import com.example.b2bpoint.coupon.dto.CouponTemplateCreateRequest;
import com.example.b2bpoint.coupon.dto.CouponTemplateResponse;
import com.example.b2bpoint.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/template")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CouponTemplateResponse> createCouponTemplate(
            @RequestAttribute Long partnerId,
            @RequestBody @Valid CouponTemplateCreateRequest request
            ){

        CouponTemplateResponse response=couponService.createCouponTemplate(partnerId,request);

        return ApiResponse.success(response);
    }

    @PostMapping("/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CouponResponse> issueCoupon(
            @RequestAttribute Long partnerId,
            @RequestBody @Valid CouponIssueRequest request) {

        CouponResponse response = couponService.issueCoupon(partnerId, request);

        return ApiResponse.success(response);
    }

    @PostMapping("/issue-async") // 경로를 분리하거나 기존 경로를 대체
    @ResponseStatus(HttpStatus.OK) // 201 Created가 아닌, '요청 접수' 의미로 200 OK 또는 202 Accepted가 더 적합
    public ApiResponse<String> issueCouponAsync(
            @RequestAttribute Long partnerId,
            @RequestBody @Valid CouponIssueRequest request) {

        couponService.issueCouponAsync(partnerId, request);

        // 사용자에게는 즉시 '성공'처럼 보이는 낙관적인 응답을 보냄
        return ApiResponse.success("쿠폰 발급 요청이 성공적으로 접수되었습니다.");
    }
}
