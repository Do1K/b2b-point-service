package com.example.b2bpoint.coupon.application;

import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponTemplate;

import com.example.b2bpoint.coupon.domain.CouponType;
import com.example.b2bpoint.coupon.dto.CouponIssueMessage;
import com.example.b2bpoint.coupon.repository.CouponRepository;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;

import com.example.b2bpoint.partner.domain.Partner;
import com.example.b2bpoint.partner.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BatchInsertPerformanceTest {

    @Autowired
    private CouponIssueSyncService couponIssueSyncService;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private CouponTemplateRepository couponTemplateRepository;

    @Autowired
    private CouponRepository couponRepository;

    private Partner testPartner;
    private List<CouponIssueMessage> testMessages;
    private final int DATA_SIZE = 100_000;
    private CouponTemplate template;

    @BeforeEach
    void setUp() {

        testPartner = Partner.builder().name("테스트 파트너").contactEmail("test@partner.com").businessNumber("111-22-33333").build();
        partnerRepository.save(testPartner);
        template = createCouponTemplate(10000);

        testMessages = new ArrayList<>();
        for (int i = 0; i < DATA_SIZE; i++) {
            testMessages.add(
                    new CouponIssueMessage(
                            testPartner.getId(),
                            template.getId(),
                            "user-" + i,
                            template.getValidUntil()
                    )
            );
        }
    }

    private CouponTemplate createCouponTemplate(Integer totalQuantity) {
        CouponTemplate template = CouponTemplate.builder()
                .partnerId(testPartner.getId())
                .name("선착순 테스트 쿠폰").couponType(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(1000)).totalQuantity(totalQuantity)
                .validFrom(LocalDateTime.now()).validUntil(LocalDateTime.now().plusDays(30))
                .minOrderAmount(0).build();
        return couponTemplateRepository.save(template);
    }

    @Test
    @DisplayName("성능 테스트: 메시지 10,000건으로 쿠폰 생성 및 수량 업데이트 동시 처리")
    @Transactional
    void issue_and_update_quantity_batch_test() {
        // given
        StopWatch stopWatch = new StopWatch();

        // when
        stopWatch.start();
        couponIssueSyncService.issueCouponsAndUpdateQuantityInBatch(testMessages);
        stopWatch.stop();

        // then
        System.out.println("--- 전체 배치 작업 (INSERT + UPDATE) 실행 시간 ---");
        System.out.println("Total Time (ms) for " + DATA_SIZE + " messages: " + stopWatch.getTotalTimeMillis());

        long actualCouponCount = couponRepository.count();
        CouponTemplate updatedTemplate = couponTemplateRepository.findById(template.getId()).orElseThrow();

        assertThat(actualCouponCount).isEqualTo(DATA_SIZE);
        assertThat(updatedTemplate.getIssuedQuantity()).isEqualTo(DATA_SIZE);
    }
}