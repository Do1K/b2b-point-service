package com.example.b2bpoint.coupon.controller;

import com.example.b2bpoint.common.dto.ApiResponse;
import com.example.b2bpoint.common.dto.ErrorResponse;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.dto.*;
import com.example.b2bpoint.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/issue-async")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CouponIssueResponse> issueCouponAsync(
            @RequestAttribute Long partnerId,
            @RequestBody @Valid CouponIssueRequest request) {

        CouponIssueResponse response=couponService.issueCouponAsync(partnerId, request);

        return ApiResponse.success(response);
    }

    @GetMapping("/{userId}")
    public ApiResponse<List<CouponResponse>> getCoupons(
            @RequestAttribute Long partnerId,
            @PathVariable String userId) {

        List<CouponResponse> coupons=couponService.getCoupons(partnerId,userId);
        return ApiResponse.success(coupons);
    }
}
