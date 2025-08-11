package com.example.b2bpoint.coupon.controller;

import com.example.b2bpoint.common.dto.ApiResponse;
import com.example.b2bpoint.coupon.dto.CouponTemplateCreateRequest;
import com.example.b2bpoint.coupon.dto.CouponTemplateResponse;
import com.example.b2bpoint.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupon")
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
}
