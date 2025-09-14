package com.example.b2bpoint.coupon.controller;

import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponStatus;
import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.domain.CouponType;
import com.example.b2bpoint.coupon.dto.CouponIssueRequest;
import com.example.b2bpoint.coupon.dto.CouponResponse;
import com.example.b2bpoint.coupon.dto.CouponTemplateCreateRequest;
import com.example.b2bpoint.coupon.dto.CouponUseRequest;
import com.example.b2bpoint.coupon.repository.CouponRepository;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import com.example.b2bpoint.coupon.service.CouponService;
import com.example.b2bpoint.partner.domain.Partner;
import com.example.b2bpoint.partner.repository.PartnerRepository;
import com.example.b2bpoint.point.repository.PointHistoryRepository;
import com.example.b2bpoint.point.repository.PointWalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PartnerRepository partnerRepository;
    @Autowired
    private CouponTemplateRepository couponTemplateRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private PointWalletRepository pointWalletRepository;
    @Autowired
    private PointHistoryRepository pointHistoryRepository;
    @Autowired
    private StringRedisTemplate redisTemplate; // Redis 상태 확인을 위해 주입

    private Partner testPartner;
    private String validApiKey;
    private Coupon availableCoupon;

    private static final String COUPON_TEMPLATE_URL = "/api/v1/coupons/template";
    private static final String COUPON_ISSUE_ASYNC_URL = "/api/v1/coupons/issue-async";

    @BeforeEach
    void setUp() {
        // Redis 데이터 정리
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // DB 데이터 정리 (자식 -> 부모 순서)
        pointHistoryRepository.deleteAllInBatch();
        pointWalletRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();
        couponTemplateRepository.deleteAllInBatch();
        partnerRepository.deleteAllInBatch();

        // 테스트용 파트너 생성
        testPartner = Partner.builder().name("테스트 파트너").contactEmail("test@partner.com").businessNumber("111-22-33333").build();
        testPartner.approve();
        testPartner.issueApiKey();
        validApiKey = testPartner.getApiKey();
        partnerRepository.save(testPartner);

//        CouponTemplate template1 = couponTemplateRepository.save(
//                CouponTemplate.builder()
//                        .name("테스트쿠폰템플릿")
//                        .couponType(CouponType.FIXED_AMOUNT)
//                        .discountValue(BigDecimal.TEN)
//                        .minOrderAmount(10)
//                        .validFrom(LocalDateTime.now())
//                        .validUntil(LocalDateTime.now().plusDays(10))
//                        .partnerId(testPartner.getId())
//                        .build()
//        );

//        availableCoupon = couponRepository.save(
//                Coupon.createFromMessage(testPartner.getId(), "test-user-123", template.getId(),template1.getValidUntil())
//        );
    }

    // --- CouponTemplate 생성 테스트 (기존과 거의 동일, URL 경로만 수정) ---
    @Test
    @Transactional
    @DisplayName("성공: 쿠폰 템플릿을 생성한다.")
    void createCouponTemplate_success() throws Exception {
        // ... 기존 코드와 거의 동일, URL 변수만 변경 ...
        CouponTemplateCreateRequest request = CouponTemplateCreateRequest.builder()
                .name("테스트 쿠폰").couponType(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.TEN).minOrderAmount(10)
                .validFrom(LocalDateTime.now().plusDays(1)).validUntil(LocalDateTime.now().plusDays(10))
                .build();
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post(COUPON_TEMPLATE_URL)
                        .header("X-API-KEY", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("테스트 쿠폰"))
                .andDo(print());
    }

    // ... createCouponTemplate_fail 테스트도 동일하게 유지 ...

    // --- Coupon 발급 테스트 (비동기 로직에 맞게 수정) ---

    @Test
    @Transactional
    @DisplayName("성공: 비동기 쿠폰 발급 요청에 성공한다.")
    void issueCouponAsync_success() throws Exception {
        // given
        CouponTemplate template = createCouponTemplate(100);
        CouponIssueRequest request = new CouponIssueRequest(template.getId(), "user-1");
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post(COUPON_ISSUE_ASYNC_URL)
                        .header("X-API-KEY", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isOk()) // 201 Created -> 200 OK 로 변경됨
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("쿠폰이 성공적으로 발급되었습니다."))
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("실패: 소진된 쿠폰 발급 요청 시 실패한다 (Redis 컷오프).")
    void issueCouponAsync_fail_quantityExceeded() throws Exception {
        // given
        CouponTemplate template = createCouponTemplate(1); // 1개 한정

        // [수정] DB를 직접 조작하는 대신, Redis의 상태를 조작하여 "소진된" 상황을 만듦
        String countKey = String.format("coupon:template:%d:count", template.getId());
        redisTemplate.opsForValue().set(countKey, "1"); // 발급 카운트를 이미 1로 설정

        CouponIssueRequest request = new CouponIssueRequest(template.getId(), "user-2");
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post(COUPON_ISSUE_ASYNC_URL)
                        .header("X-API-KEY", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CP002")) // COUPON_ISSUE_QUANTITY_EXCEEDED
                .andDo(print());
    }

    @Test
    @DisplayName("동시성 테스트: 100명이 10개 한정 쿠폰 요청 시, Redis에서 10개만 통과시키고 Consumer가 모두 처리한다.")
    void issueCouponAsync_concurrencyTest() throws Exception {
        // given
        CouponTemplate template = createCouponTemplate(10); // 10개 한정
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            final String userId = "user-" + i;
            executorService.submit(() -> {
                try {
                    CouponIssueRequest request = new CouponIssueRequest(template.getId(), userId);
                    String requestBody = objectMapper.writeValueAsString(request);

                    mockMvc.perform(post(COUPON_ISSUE_ASYNC_URL)
                                    .header("X-API-KEY", validApiKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                            )
                            .andDo(result -> {
                                if (result.getResponse().getStatus() == 200) {
                                    successCount.incrementAndGet(); // API 응답 성공 카운트
                                } else {
                                    failCount.incrementAndGet(); // API 응답 실패 카운트
                                }
                            });
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // [수정] Consumer가 메시지를 모두 처리할 시간을 줌 (매우 중요)
        TimeUnit.SECONDS.sleep(5);

        // then
        System.out.println("API Success Count: " + successCount.get());
        System.out.println("API Fail Count: " + failCount.get());

        // 1. API 응답 검증: 10명은 성공 응답, 90명은 실패 응답을 받았어야 함
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(90);

        // 2. DB 최종 상태 검증: Consumer가 10개의 쿠폰을 모두 DB에 저장했어야 함
        TimeUnit.SECONDS.sleep(10);

        long couponInDb = couponRepository.count();
        System.out.println("Coupons in DB: " + couponInDb);
        assertThat(couponInDb).isEqualTo(10);
    }

    // 헬퍼 메서드 (기존과 동일)
    private CouponTemplate createCouponTemplate(Integer totalQuantity) {
        CouponTemplate template = CouponTemplate.builder()
                .partnerId(testPartner.getId())
                .name("선착순 테스트 쿠폰").couponType(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(1000)).totalQuantity(totalQuantity)
                .validFrom(LocalDateTime.now().minusDays(1)).validUntil(LocalDateTime.now().plusDays(30))
                .minOrderAmount(0).build();
        return couponTemplateRepository.save(template);
    }

    @DisplayName("성공: 특정 사용자의 쿠폰 목록을 조회한다")
    @Transactional
    @Test
    void getCoupons_Success() throws Exception {
        // given
        Long partnerId = testPartner.getId();
        String userId = "test-user-123";

        CouponTemplate template1 = createCouponTemplate(1);
        CouponTemplate template2 = createCouponTemplate(2);


        Coupon coupon1 = Coupon.builder()
                .partnerId(partnerId)
                .userId(userId)
                .couponTemplate(template1)
                .build();

        Coupon coupon2 = Coupon.builder()
                .partnerId(partnerId)
                .userId(userId)
                .couponTemplate(template2)
                .build();



        couponRepository.save(coupon1);
        couponRepository.save(coupon2);


        // when & then
        mockMvc.perform(
                        get("/api/v1/coupons/{userId}", userId)
                                .header("X-API-KEY", validApiKey)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].couponName").value("선착순 테스트 쿠폰"));
    }

    @DisplayName("성공: 유효한 쿠폰을 성공적으로 '사용됨' 처리한다")
    @Test
    void useCoupon_Success() throws Exception {
        // given
        CouponTemplate couponTemplate=createCouponTemplate(1);

        availableCoupon = couponRepository.save(
                Coupon.createFromMessage(testPartner.getId(), "test-user-123", couponTemplate.getId(), couponTemplate.getValidUntil())
        );
        CouponUseRequest request = CouponUseRequest.builder()
                .userId(availableCoupon.getUserId())
                .orderId(999L)
                .orderAmount(10000)
                .build();
        String requestJson = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/coupons/{couponCode}/use", availableCoupon.getCode())
                                .header("X-API-KEY", validApiKey)
                                .requestAttr("partnerId", testPartner.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("USED"))
                .andExpect(jsonPath("$.data.couponCode").value(availableCoupon.getCode()));

        // DB 상태 직접 검증 (선택 사항이지만, 가장 확실한 방법)
        Coupon usedCoupon = couponRepository.findById(availableCoupon.getId()).get();
        assertThat(usedCoupon.getStatus()).isEqualTo(CouponStatus.USED);
        assertThat(usedCoupon.getUsedAt()).isNotNull();
    }

    @DisplayName("실패: 이미 사용된 쿠폰을 다시 사용하려고 하면 예외가 발생한다")
    @Test
    void useCoupon_Fail_WhenAlreadyUsed() throws Exception {
        // given
        // 쿠폰을 미리 '사용됨' 상태로 변경
        CouponTemplate couponTemplate=createCouponTemplate(1);

        availableCoupon = couponRepository.save(
                Coupon.createFromMessage(testPartner.getId(), "test-user-123", couponTemplate.getId(), couponTemplate.getValidUntil())
        );
        availableCoupon.use(testPartner.getId(), availableCoupon.getUserId());
        couponRepository.saveAndFlush(availableCoupon); // DB에 즉시 반영

        CouponUseRequest request = CouponUseRequest.builder()
                .userId(availableCoupon.getUserId())
                .orderId(1001L)
                .orderAmount(20000)
                .build();
        String requestJson = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/coupons/{couponCode}/use", availableCoupon.getCode())
                                .requestAttr("partnerId", testPartner.getId())
                                .header("X-API-KEY", validApiKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andDo(print())
                .andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CP008")); // COUPON_ALREADY_USED_OR_EXPIRED
    }

    @DisplayName("실패: 다른 사용자가 남의 쿠폰을 사용하려고 하면 예외가 발생한다")
    @Test
    void useCoupon_Fail_WhenOwnerMismatch() throws Exception {
        // given
        CouponTemplate couponTemplate=createCouponTemplate(1);

        availableCoupon = couponRepository.save(
                Coupon.createFromMessage(testPartner.getId(), "test-user-123", couponTemplate.getId(), couponTemplate.getValidUntil())
        );
        String anotherUser = "another-user-789";
        CouponUseRequest request = CouponUseRequest.builder()
                .userId(anotherUser) // 쿠폰 소유주가 아닌 다른 사용자로 요청
                .orderId(1002L)
                .orderAmount(30000)
                .build();
        String requestJson = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/coupons/{couponCode}/use", availableCoupon.getCode())
                                .requestAttr("partnerId", testPartner.getId())
                                .header("X-API-KEY", validApiKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andDo(print())
                .andExpect(status().isForbidden()) // 403 Forbidden 예상
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CP007"));
    }



}