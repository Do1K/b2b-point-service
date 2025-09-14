package com.example.b2bpoint.coupon.service;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.coupon.application.CouponIssueProducer;
import com.example.b2bpoint.coupon.application.CouponReader;
import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponStatus;
import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.domain.CouponType;
import com.example.b2bpoint.coupon.dto.*;
import com.example.b2bpoint.coupon.repository.CouponRepository;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks private CouponService couponService;

    @Mock private CouponRepository couponRepository;
    @Mock private CouponTemplateRepository couponTemplateRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private CouponIssueProducer couponIssueProducer;
    @Mock private CouponReader couponReader;

    @Spy private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private SetOperations<String, String> setOperations;

    @BeforeEach
    void setUp() {

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    Long partnerId = 1L;

    @Nested
    @DisplayName("쿠폰 템플릿 생성 (createCouponTemplate)")
    class CreateCouponTemplate {

        @Test
        @DisplayName("성공 케이스")
        void success() throws JsonProcessingException {
            // given
            Long partnerId = 1L;
            CouponTemplateCreateRequest request = CouponTemplateCreateRequest.builder()
                    .name("신규 템플릿").couponType(CouponType.FIXED_AMOUNT).discountValue(BigDecimal.TEN)
                    .validFrom(LocalDateTime.now().plusDays(1)).validUntil(LocalDateTime.now().plusDays(10))
                    .minOrderAmount(0).build();

            CouponTemplate filledTemplate = CouponTemplate.builder()
                    .partnerId(partnerId).name(request.getName()).couponType(request.getCouponType())
                    .discountValue(request.getDiscountValue()).minOrderAmount(request.getMinOrderAmount())
                    .validFrom(request.getValidFrom()).validUntil(request.getValidUntil())
                    .build();

            given(couponTemplateRepository.save(any(CouponTemplate.class))).willReturn(filledTemplate);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            CouponTemplateResponse response = couponService.createCouponTemplate(partnerId, request);

            // then
            assertThat(response).isNotNull();
            verify(valueOperations).set(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("실패 케이스 - 유효 기간 오류")
        void fail_invalidDate() {
            // given
            CouponTemplateCreateRequest request = CouponTemplateCreateRequest.builder()
                    .validFrom(LocalDateTime.now().plusDays(3)).validUntil(LocalDateTime.now().plusDays(2))
                    .build();

            // when & then
            assertThrows(IllegalArgumentException.class, () -> couponService.createCouponTemplate(partnerId, request));
        }
    }


    @Nested
    @DisplayName("비동기 쿠폰 발급 (issueCouponAsync)")
    class IssueCouponAsync {

        private Long partnerId = 1L;
        private final Long templateId = 1L;
        private String userId = "user-123";
        private CouponIssueRequest request = new CouponIssueRequest(templateId, userId);
        private CouponTemplateCacheDto cacheDto = CouponTemplateCacheDto.builder()
                .id(templateId).partnerId(partnerId).totalQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(1)).validUntil(LocalDateTime.now().plusDays(1))
                .build();



        @Test
        @DisplayName("성공: 캐시 미스(Cache Miss) 시 DB 조회 후 쿠폰 발급 메시지를 전송한다.")
        void success_whenCacheMiss() {
            // given
            Long templateId = 1L;
            String userId = "user-123";
            CouponIssueRequest request = new CouponIssueRequest(templateId, userId);
            CouponTemplateCacheDto dbDto = CouponTemplateCacheDto.builder()
                    .totalQuantity(100).partnerId(partnerId)
                    .validFrom(LocalDateTime.now().minusDays(1)).validUntil(LocalDateTime.now().plusDays(1))
                    .build();

            given(couponReader.findTemplateFromCache(templateId)).willReturn(null);

            given(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
                    .willReturn(true);

            given(couponReader.findTemplateFromDbAndCache(templateId)).willReturn(dbDto);

            given(redisTemplate.opsForSet().add(anyString(), anyString())).willReturn(1L);
            given(redisTemplate.opsForValue().increment(anyString())).willReturn(10L);

            // when
            couponService.issueCouponAsync(partnerId, request);

            // then
            verify(couponReader).findTemplateFromDbAndCache(templateId);
            verify(couponIssueProducer).send(any(CouponIssueMessage.class));
        }



        @Test
        @DisplayName("실패: 이미 발급받은 사용자가 요청 시 예외가 발생한다.")
        void fail_whenCouponAlreadyIssued() {
            // given
            given(couponReader.findTemplateFromCache(templateId)).willReturn(cacheDto);
            given(setOperations.add(anyString(), anyString())).willReturn(0L); // 중복 시 0L 반환

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    couponService.issueCouponAsync(partnerId, request)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
            verify(couponIssueProducer, never()).send(any());
        }


        @Test
        @DisplayName("성공: 캐시 히트(Cache Hit) 시 쿠폰 발급 메시지를 전송한다.")
        void success_whenCacheHit() {
            // given
            given(couponReader.findTemplateFromCache(templateId)).willReturn(cacheDto);
            given(setOperations.add(anyString(), anyString())).willReturn(1L);
            given(valueOperations.increment(anyString())).willReturn(10L);

            // when
            CouponIssueResponse response = couponService.issueCouponAsync(partnerId, request);

            // then
            assertThat(response.getMessage()).isEqualTo("쿠폰이 성공적으로 발급되었습니다.");
            verify(couponIssueProducer).send(any(CouponIssueMessage.class));
        }

        @Test
        @DisplayName("실패: 쿠폰 수량이 모두 소진되었을 때 요청 시 예외가 발생한다.")
        void fail_whenQuantityExceeded() {
            // given
            cacheDto = CouponTemplateCacheDto.builder().totalQuantity(10).partnerId(partnerId)
                    .validFrom(LocalDateTime.now().minusDays(1)).validUntil(LocalDateTime.now().plusDays(1)).build();

            given(couponReader.findTemplateFromCache(templateId)).willReturn(cacheDto);
            given(setOperations.add(anyString(), anyString())).willReturn(1L);
            given(valueOperations.increment(anyString())).willReturn(11L);

            // when & then
            assertThrows(CustomException.class, () ->
                    couponService.issueCouponAsync(partnerId, request)
            );

            verify(setOperations).remove(anyString(), anyString());
            verify(couponIssueProducer, never()).send(any());
        }


        @Test
        @DisplayName("실패: 메시지 큐 전송 실패 시 예외가 발생하고 보정 로직이 실행된다.")
        void fail_whenMessageQueueFails() {
            // given
            given(couponReader.findTemplateFromCache(templateId)).willReturn(cacheDto);
            given(redisTemplate.opsForSet().add(anyString(), anyString())).willReturn(1L);
            given(redisTemplate.opsForValue().increment(anyString())).willReturn(10L);

            doThrow(new RuntimeException("MQ Connection Error")).when(couponIssueProducer).send(any());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                couponService.issueCouponAsync(partnerId, request);
            });

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MESSAGING_SYSTEM_ERROR);

            verify(redisTemplate.opsForValue()).decrement(anyString());
            verify(redisTemplate.opsForSet()).remove(anyString(), anyString());
        }

    }

    @Nested
    @DisplayName("쿠폰 조회 (getCoupons)")
    class GetCouponsTest {

        @DisplayName("성공: 특정 사용자의 쿠폰 목록을 반환한다")
        @Test
        void getCoupons_Success() {
            // given
            Long partnerId = 1L;
            String userId = "test-user-123";

            CouponTemplate mockTemplate1 = CouponTemplate.builder().name("Welcome Coupon").build();
            CouponTemplate mockTemplate2 = CouponTemplate.builder().name("Summer Sale Coupon").build();

            List<Coupon> mockCoupons = List.of(
                    Coupon.builder()
                            .partnerId(partnerId)
                            .userId(userId)
                            .couponTemplate(mockTemplate1)
                            .build(),
                    Coupon.builder()
                            .partnerId(partnerId)
                            .userId(userId)
                            .couponTemplate(mockTemplate2)
                            .build()
            );

            given(couponRepository.findByPartnerIdAndUserId(partnerId, userId)).willReturn(mockCoupons);

            // when
            List<CouponResponse> responses = couponService.getCoupons(partnerId, userId);

            // then
            verify(couponRepository, times(1)).findByPartnerIdAndUserId(partnerId, userId);

            assertThat(responses).hasSize(2);

            assertThat(responses.get(0).getCouponName()).isEqualTo("Welcome Coupon");
            assertThat(responses.get(1).getCouponName()).isEqualTo("Summer Sale Coupon");
        }

        @DisplayName("성공: 사용자에게 발급된 쿠폰이 없으면 빈 리스트를 반환한다")
        @Test
        void getCoupons_WhenNoCoupons_ReturnsEmptyList() {
            // given
            Long partnerId = 1L;
            String userId = "new-user-456";

            given(couponRepository.findByPartnerIdAndUserId(partnerId, userId)).willReturn(Collections.emptyList());

            // when
            List<CouponResponse> responses = couponService.getCoupons(partnerId, userId);

            // then
            assertThat(responses).isNotNull();
            assertThat(responses).isEmpty();
            verify(couponRepository, times(1)).findByPartnerIdAndUserId(partnerId, userId);
        }
    }

    @Nested
    @DisplayName("쿠폰 사용(useCoupon)")
    class UseCouponTest {
        @Spy // Coupon 객체의 실제 메소드(use)를 호출하고 검증하기 위해 @Spy 사용
        private Coupon availableCoupon;

        @DisplayName("성공: 유효한 쿠폰을 성공적으로 '사용됨' 처리한다")
        @Test
        void useCoupon_Success() {
            // given
            Long partnerId = 1L;
            String userId = "test-user";
            String couponCode = "VALID-COUPON-CODE";

            availableCoupon = Coupon.issueBuilder()
                            .partnerId(partnerId)
                                    .userId(userId)
                                            .validUntil(LocalDateTime.now().plusDays(1))
                                                    .build();

            given(couponRepository.findByCodeWithLock(couponCode))
                    .willReturn(Optional.of(availableCoupon));

            // when
            CouponUseResponse response = couponService.useCoupon(partnerId, userId, couponCode);

            // then
            verify(couponRepository, times(1)).findByCodeWithLock(couponCode);

            assertThat(response.getStatus()).isEqualTo(CouponStatus.USED);
            assertThat(response.getUsedAt()).isNotNull();
        }

        @DisplayName("실패: 존재하지 않는 쿠폰 코드로 요청 시 예외가 발생한다")
        @Test
        void useCoupon_Fail_WhenCouponNotFound() {
            // given
            String nonExistentCode = "INVALID-CODE";
            given(couponRepository.findByCodeWithLock(nonExistentCode))
                    .willReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                couponService.useCoupon(1L, "test-user", nonExistentCode);
            });

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_NOT_FOUND);
        }
    }



}