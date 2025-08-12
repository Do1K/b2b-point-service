package com.example.b2bpoint.coupon.service;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.dto.CouponIssueRequest;
import com.example.b2bpoint.coupon.dto.CouponResponse;
import com.example.b2bpoint.coupon.dto.CouponTemplateCreateRequest;
import com.example.b2bpoint.coupon.dto.CouponTemplateResponse;
import com.example.b2bpoint.coupon.repository.CouponRepository;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponTemplateRepository couponTemplateRepository;
    private final CouponRepository couponRepository;

    public CouponTemplateResponse createCouponTemplate(Long partnerId, CouponTemplateCreateRequest request){
        if(request.getValidFrom().isAfter(request.getValidUntil())){
            throw new IllegalArgumentException("유효 기간 설정이 잘못되었습니다. ");
        }

        CouponTemplate couponTemplate = CouponTemplate.builder()
                .partnerId(partnerId)
                .name(request.getName())
                .couponType(request.getCouponType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount())
                .totalQuantity(request.getTotalQuantity())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .build();


        CouponTemplate savedTemplate = couponTemplateRepository.save(couponTemplate);

        return CouponTemplateResponse.from(savedTemplate);
    }

    public CouponResponse issueCoupon(Long partnerId, CouponIssueRequest request){

        CouponTemplate couponTemplate=couponTemplateRepository.findByIdWithLock(request.getCouponTemplateId())
                .orElseThrow(()->new CustomException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND));

        validateCouponIssuance(partnerId, couponTemplate, request.getUserId());

        couponTemplate.increaseIssuedQuantity();

        Coupon coupon = Coupon.builder()
                .partnerId(partnerId)
                .userId(request.getUserId())
                .couponTemplate(couponTemplate)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        return CouponResponse.from(savedCoupon);
    }

    private void validateCouponIssuance(Long partnerId, CouponTemplate template, String userId) {
        // [보안 검증] 요청을 보낸 파트너사가 쿠폰 템플릿의 소유주가 맞는지 확인
        if (!template.getPartnerId().equals(partnerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // [기간 검증] 현재 시각이 쿠폰 발급 가능 기간 내에 있는지 확인
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(template.getValidFrom()) || now.isAfter(template.getValidUntil())) {
            throw new CustomException(ErrorCode.COUPON_NOT_IN_ISSUE_PERIOD);
        }

        if (couponRepository.existsByCouponTemplateIdAndUserId(template.getId(), userId)) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }
}
