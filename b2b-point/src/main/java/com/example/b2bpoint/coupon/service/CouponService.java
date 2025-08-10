package com.example.b2bpoint.coupon.service;

import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.dto.CouponTemplateCreateRequest;
import com.example.b2bpoint.coupon.dto.CouponTemplateResponse;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponTemplateRepository couponTemplateRepository;

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
}
