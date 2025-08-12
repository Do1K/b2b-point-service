package com.example.b2bpoint.coupon.service;

import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.domain.CouponType;
import com.example.b2bpoint.coupon.dto.CouponTemplateCreateRequest;
import com.example.b2bpoint.coupon.dto.CouponTemplateResponse;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import com.example.b2bpoint.point.domain.PointWallet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponTemplateRepository couponTemplateRepository;

    @InjectMocks
    private CouponService couponService;

    private final Long partnerId = 1L;
    private final String name="테스트 쿠폰";
    private final CouponType couponType=CouponType.FIXED_AMOUNT;
    private final BigDecimal discountValue=BigDecimal.TEN;
    private final Integer maxDiscountAmount=20;
    private final  Integer minOrderAmount=10;


    @DisplayName("쿠폰 템플릿 생성")
    @Test
    void createCouponTemplate() {
        //given

        LocalDateTime validFrom=LocalDateTime.now().plusDays(1);
        LocalDateTime validUntil=LocalDateTime.now().plusDays(10);

        CouponTemplateCreateRequest request=CouponTemplateCreateRequest.builder()
                .name(name)
                .couponType(couponType)
                .discountValue(discountValue)
                .maxDiscountAmount(maxDiscountAmount)
                .minOrderAmount(minOrderAmount)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .build();

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

        BDDMockito.given(couponTemplateRepository.save(BDDMockito.any(CouponTemplate.class)))
                .willReturn(couponTemplate);

        //when
        CouponTemplateResponse response=couponService.createCouponTemplate(partnerId,request);

        //then
        verify(couponTemplateRepository).save(BDDMockito.any(CouponTemplate.class));
        Assertions.assertThat(response.getName()).isEqualTo(name);
        Assertions.assertThat(response.getCouponType()).isEqualTo(couponType);

    }

    @DisplayName("쿠폰 템플릿 생성 실패(기간 설정 오류)")
    @Test
    void createCouponTemplate_fail(){
        //given

        LocalDateTime validFrom=LocalDateTime.now().plusDays(3);
        LocalDateTime validUntil=LocalDateTime.now().plusDays(2);

        CouponTemplateCreateRequest request=CouponTemplateCreateRequest.builder()
                .name(name)
                .couponType(couponType)
                .discountValue(discountValue)
                .maxDiscountAmount(maxDiscountAmount)
                .minOrderAmount(minOrderAmount)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .build();

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

        //when&then
        assertThrows(IllegalArgumentException.class, () -> couponService.createCouponTemplate(partnerId,request));
    }

}