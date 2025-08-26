package com.example.b2bpoint.coupon.service;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.coupon.application.CouponIssueProducer;
import com.example.b2bpoint.coupon.application.CouponReader;
import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.domain.CouponType;
import com.example.b2bpoint.coupon.dto.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    // [수정] @InjectMocks 대신, 테스트 대상 객체를 직접 생성
    private CouponService couponService;

    // --- Mock 객체 선언 (의존성) ---
    @Mock private CouponTemplateRepository couponTemplateRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private CouponIssueProducer couponIssueProducer;
    @Mock private CouponReader couponReader;

    // [수정] ObjectMapper는 실제 객체를 사용. @Spy 대신 직접 생성
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // RedisTemplate의 내부 동작을 Mocking 하기 위한 객체들
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private SetOperations<String, String> setOperations;

    @BeforeEach
    void setUp() {
        // [수정] @InjectMocks를 사용하지 않으므로, 수동으로 테스트 대상 객체 생성 및 의존성 주입
        couponService = new CouponService(
                couponTemplateRepository,
                null, // CouponRepository는 CouponService에서 직접 사용하지 않으므로 null
                redisTemplate,
                couponIssueProducer,
                objectMapper,
                couponReader
        );

        // [핵심] 모든 테스트에서 redisTemplate.opsFor...()가 null을 반환하지 않도록 lenient하게 설정
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

            // [최종 수정]
            // save 메서드에 전달될 객체와 거의 동일한, 필드가 채워진 객체를 준비
            CouponTemplate filledTemplate = CouponTemplate.builder()
                    .partnerId(partnerId).name(request.getName()).couponType(request.getCouponType())
                    .discountValue(request.getDiscountValue()).minOrderAmount(request.getMinOrderAmount())
                    .validFrom(request.getValidFrom()).validUntil(request.getValidUntil())
                    .build();

            // save가 호출되면, 필드가 모두 채워진 이 객체를 반환하도록 설정
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


    @Nested // issueCouponAsync 관련 테스트 그룹
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

            // 1. 처음에는 캐시 미스
            given(couponReader.findTemplateFromCache(templateId)).willReturn(null);

            // [핵심 수정] 분산 락 획득에 성공했다고 가정 (true 반환)
            given(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
                    .willReturn(true);

            // 2. DB 조회는 성공
            given(couponReader.findTemplateFromDbAndCache(templateId)).willReturn(dbDto);

            // 3. Redis 중복/수량 체크 통과
            given(redisTemplate.opsForSet().add(anyString(), anyString())).willReturn(1L);
            given(redisTemplate.opsForValue().increment(anyString())).willReturn(10L);

            // when
            couponService.issueCouponAsync(partnerId, request);

            // then
            // DB 조회 메서드가 정확히 1번 호출되었는지 검증
            verify(couponReader).findTemplateFromDbAndCache(templateId);
            // Producer가 호출되었는지 검증
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

        // ... 나머지 테스트 케이스들도 이 패턴을 따르면 정상 동작 ...

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

            // [핵심] Producer의 send 메서드가 호출되면 Exception을 던지도록 설정
            doThrow(new RuntimeException("MQ Connection Error")).when(couponIssueProducer).send(any());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                couponService.issueCouponAsync(partnerId, request);
            });

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MESSAGING_SYSTEM_ERROR);

            // [보정 로직 검증] 큐 전송 실패 시, Redis 카운트를 되돌리고(decrement) Set 멤버를 제거하는 로직이 모두 호출되었는지 확인
            verify(redisTemplate.opsForValue()).decrement(anyString());
            verify(redisTemplate.opsForSet()).remove(anyString(), anyString());
        }
    }

}