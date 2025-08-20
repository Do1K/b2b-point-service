package com.example.b2bpoint.coupon.service;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponStatus;
import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.domain.CouponType;
import com.example.b2bpoint.coupon.dto.CouponIssueRequest;
import com.example.b2bpoint.coupon.dto.CouponResponse;
import com.example.b2bpoint.coupon.dto.CouponTemplateCreateRequest;
import com.example.b2bpoint.coupon.dto.CouponTemplateResponse;
import com.example.b2bpoint.coupon.repository.CouponRepository;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import com.example.b2bpoint.point.domain.PointWallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponTemplateRepository couponTemplateRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CouponService couponService;

    private final Long partnerId = 1L;
    private final String name="테스트 쿠폰";
    private final CouponType couponType=CouponType.FIXED_AMOUNT;
    private final BigDecimal discountValue=BigDecimal.TEN;
    private final Integer maxDiscountAmount=20;
    private final  Integer minOrderAmount=10;

    private CouponTemplate availableTemplate;

    @BeforeEach
    void setUp() {

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        //when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 테스트에서 사용할 공통 쿠폰 템플릿 객체를 미리 생성
        availableTemplate = CouponTemplate.builder()
                .partnerId(partnerId)
                .name("사용 가능한 쿠폰")
                .couponType(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(1000))
                .totalQuantity(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(10))
                .minOrderAmount(0)
                .build();
    }

    @DisplayName("성공: 쿠폰 템플릿 생성")
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

        given(couponTemplateRepository.save(any(CouponTemplate.class)))
                .willReturn(couponTemplate);

        //when
        CouponTemplateResponse response=couponService.createCouponTemplate(partnerId,request);

        //then
        verify(couponTemplateRepository).save(any(CouponTemplate.class));
        assertThat(response.getName()).isEqualTo(name);
        assertThat(response.getCouponType()).isEqualTo(couponType);

    }

    @DisplayName("실패: 쿠폰 템플릿 생성 실패(기간 설정 오류)")
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

        //when&then
        assertThrows(IllegalArgumentException.class, () -> couponService.createCouponTemplate(partnerId,request));
    }

    @DisplayName("성공: 쿠폰 발급에 성공한다.")
    @Test
    void issueCoupon_success() {
        // given
        CouponIssueRequest request = new CouponIssueRequest(1L, "user-123");

        given(couponTemplateRepository.findByIdWithLock(request.getCouponTemplateId()))
                .willReturn(Optional.of(availableTemplate));

        given(couponRepository.save(any(Coupon.class)))
                .willAnswer(invocation -> invocation.getArgument(0));


        // when
        CouponResponse response = couponService.issueCoupon(partnerId, request);


        // then
        assertThat(response.getCouponName()).isEqualTo("사용 가능한 쿠폰");
        assertThat(response.getStatus()).isEqualTo(CouponStatus.AVAILABLE);

        assertThat(availableTemplate.getIssuedQuantity()).isEqualTo(1);

        verify(couponRepository).save(any(Coupon.class));
    }

    @DisplayName("실패: 존재하지 않는 쿠폰 템플릿 ID로 요청 시 예외가 발생한다.")
    @Test
    void issueCoupon_fail_templateNotFound() {
        // given
        CouponIssueRequest request = new CouponIssueRequest(999L, "user-123");

        given(couponTemplateRepository.findByIdWithLock(request.getCouponTemplateId()))
                .willReturn(Optional.empty());

        // when & then
        assertThrows(CustomException.class, () -> {
            couponService.issueCoupon(partnerId, request);
        });

        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @DisplayName("실패: 다른 파트너사의 쿠폰 템플릿으로 발급 요청 시 예외가 발생한다.")
    @Test
    void issueCoupon_fail_partnerMismatch() {
        // given
        Long anotherPartnerId = 2L;
        CouponIssueRequest request = new CouponIssueRequest(1L, "user-123");

        given(couponTemplateRepository.findByIdWithLock(request.getCouponTemplateId()))
                .willReturn(Optional.of(availableTemplate));

        // when & then
        assertThrows(CustomException.class, () -> {
            couponService.issueCoupon(anotherPartnerId, request);
        });
    }

    @DisplayName("실패: 쿠폰 발급 기간이 아닌 경우 예외가 발생한다.")
    @Test
    void issueCoupon_fail_notInPeriod() {
        // given
        CouponTemplate expiredTemplate = CouponTemplate.builder()
                .partnerId(partnerId)
                .validFrom(LocalDateTime.now().minusDays(10))
                .validUntil(LocalDateTime.now().minusDays(1)) // 이미 만료됨
                .build();

        CouponIssueRequest request = new CouponIssueRequest(1L, "user-123");

        given(couponTemplateRepository.findByIdWithLock(request.getCouponTemplateId()))
                .willReturn(Optional.of(expiredTemplate));

        // when & then
        assertThrows(CustomException.class, () -> {
            couponService.issueCoupon(partnerId, request);
        });
    }

    @DisplayName("실패: 쿠폰 수량이 모두 소진된 경우 예외가 발생한다.")
    @Test
    void issueCoupon_fail_quantityExceeded() {
        // given

        availableTemplate.increaseIssuedQuantity();
        assertThat(availableTemplate.getIssuedQuantity()).isEqualTo(1);

        CouponIssueRequest request = new CouponIssueRequest(1L, "user-123");

        given(couponTemplateRepository.findByIdWithLock(request.getCouponTemplateId()))
                .willReturn(Optional.of(availableTemplate));


        // when & then
        assertThrows(CustomException.class, () -> {
            couponService.issueCoupon(partnerId, request);
        });
    }

}