package com.example.b2bpoint.coupon.controller;

import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.domain.CouponType;
import com.example.b2bpoint.coupon.dto.CouponIssueRequest;
import com.example.b2bpoint.coupon.dto.CouponTemplateCreateRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private Partner testPartner;
    private String validApiKey;
    private String COUPON_TEMPLATE="/api/v1/coupons/template";

    @BeforeEach
    void setUp() {
        pointHistoryRepository.deleteAllInBatch();      // 1. 포인트 내역 (point_wallets 참조)
        pointWalletRepository.deleteAllInBatch();       // 2. 포인트 지갑 (partners 참조)
        couponRepository.deleteAllInBatch();            // 3. 발급된 쿠폰 (coupon_templates, partners 참조)
        couponTemplateRepository.deleteAllInBatch();  // 4. 쿠폰 템플릿 (partners 참조)
        partnerRepository.deleteAllInBatch();

        testPartner = Partner.builder()
                .name("테스트 파트너")
                .contactEmail("test@partner.com")
                .businessNumber("111-22-33333")
                .build();

        testPartner.approve();
        testPartner.issueApiKey();
        validApiKey = testPartner.getApiKey();
        partnerRepository.save(testPartner);
    }

    @DisplayName("성공: 쿠폰 템플릿을 생성한다.")
    @Test
    @Transactional
    void createCouponTemplate_success() throws Exception {
        //given
        String name="테스트 쿠폰";
        CouponType couponType=CouponType.FIXED_AMOUNT;
        BigDecimal discountValue=BigDecimal.TEN;
        Integer maxDiscountAmount=20;
        Integer minOrderAmount=10;
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

        String requestJson=objectMapper.writeValueAsString(request);

        //when&then
        mockMvc.perform(post(COUPON_TEMPLATE)
                .header("X-API-KEY", validApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.partnerId").value(testPartner.getId()))
                .andExpect(jsonPath("$.data.name").value("테스트 쿠폰"))
                .andDo(print());
    }

    @DisplayName("실패: 쿠폰 템플릿 유효기간 오류")
    @Test
    @Transactional
    void createCouponTemplate_fail() throws Exception {
        //given
        String name="테스트 쿠폰";
        CouponType couponType=CouponType.FIXED_AMOUNT;
        BigDecimal discountValue=BigDecimal.TEN;
        Integer maxDiscountAmount=20;
        Integer minOrderAmount=10;
        LocalDateTime validFrom=LocalDateTime.now().plusDays(-1);
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

        String requestJson=objectMapper.writeValueAsString(request);

        //when&then
        mockMvc.perform(post(COUPON_TEMPLATE)
                        .header("X-API-KEY", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C001"))
                .andDo(print());
    }

    @DisplayName("성공: 쿠폰 발급에 성공한다.")
    @Test
    @Transactional
    void issueCoupon_success() throws Exception {
        // given
        CouponTemplate template = createCouponTemplate(100);
        CouponIssueRequest request = new CouponIssueRequest(template.getId(), "user-1");
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/v1/coupons/issue-async")
                        .header("X-API-KEY", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.couponId").exists())
                .andExpect(jsonPath("$.data.couponName").value("선착순 테스트 쿠폰"))
                .andDo(print());
    }

    @DisplayName("실패: 소진된 쿠폰 발급 요청 시 실패한다.")
    @Test
    @Transactional
    void issueCoupon_fail_quantityExceeded() throws Exception {
        // given
        CouponTemplate template = createCouponTemplate(1);
        template.increaseIssuedQuantity();
        couponTemplateRepository.save(template);

        CouponIssueRequest request = new CouponIssueRequest(template.getId(), "user-2");
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/v1/coupons/issue")
                        .header("X-API-KEY", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CP002")) // COUPON_ISSUE_QUANTITY_EXCEEDED
                .andDo(print());
    }

    @DisplayName("동시성 테스트: 100명이 동시에 10개 한정 쿠폰 발급 요청 시, 정확히 10명만 성공한다.")
    @Test
    void issueCoupon_concurrencyTest() throws Exception {
        // given
        CouponTemplate template = createCouponTemplate(10000);
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            final String userId = "user-" + i;
            executorService.submit(() -> {
                try {
                    CouponIssueRequest request = new CouponIssueRequest(template.getId(), userId);
                    String requestBody = objectMapper.writeValueAsString(request);

                    mockMvc.perform(post("/api/v1/coupons/issue-async")
                                    .header("X-API-KEY", validApiKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                            )
                            .andDo(result -> {
                                if (result.getResponse().getStatus() == HttpStatus.CREATED.value()) {
                                    successCount.incrementAndGet();
                                }
                            });
                } catch (Exception e) {
                    // 예외 출력 (디버깅용)
                    // e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        System.out.println("Total Success Count: " + successCount.get());
        assertThat(successCount.get()).isEqualTo(10);

        // DB 최종 상태 검증
        CouponTemplate finalTemplate = couponTemplateRepository.findById(template.getId()).get();
        assertThat(finalTemplate.getIssuedQuantity()).isEqualTo(10);
        assertThat(couponRepository.count()).isEqualTo(10);
    }


    private CouponTemplate createCouponTemplate(Integer totalQuantity) {
        CouponTemplate template = CouponTemplate.builder()
                .partnerId(testPartner.getId())
                .name("선착순 테스트 쿠폰")
                .couponType(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(1000))
                .totalQuantity(totalQuantity)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .minOrderAmount(0)
                .build();
        return couponTemplateRepository.save(template);
    }


}