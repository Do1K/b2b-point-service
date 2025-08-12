package com.example.b2bpoint.coupon.controller;

import com.example.b2bpoint.coupon.domain.CouponType;
import com.example.b2bpoint.coupon.dto.CouponTemplateCreateRequest;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import com.example.b2bpoint.coupon.service.CouponService;
import com.example.b2bpoint.partner.domain.Partner;
import com.example.b2bpoint.partner.repository.PartnerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PartnerRepository partnerRepository;

    private Partner testPartner;
    private String validApiKey;
    private String COUPON_TEMPLATE="/api/v1/coupon/template";

    @BeforeEach
    void setUp() {
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


}