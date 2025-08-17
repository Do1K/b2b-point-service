package com.example.b2bpoint.coupon.application;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.repository.CouponRepository;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponIssueSyncService {
    // 이 클래스는 오직 DB 동기화 작업만 책임짐
    private final CouponTemplateRepository couponTemplateRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public void issueCoupon(Long partnerId, Long couponTemplateId, String userId) {
        // 기존 CouponService에 있던 비관적 락을 사용한 로직 전체를 여기에 구현
        CouponTemplate template = couponTemplateRepository.findByIdWithLock(couponTemplateId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND));

        // ... 기존의 모든 validate 로직 및 increaseIssuedQuantity, Coupon 생성 및 저장 로직 ...
        // 단, 반환 타입은 void 또는 boolean으로 변경
        validateCouponIssuance(partnerId, template, userId);

        template.increaseIssuedQuantity();

        Coupon coupon = Coupon.builder()
                .partnerId(partnerId)
                .userId(userId)
                .couponTemplate(template)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
    }

    private void validateCouponIssuance(Long partnerId, CouponTemplate template, String userId) {
        if (!template.getPartnerId().equals(partnerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(template.getValidFrom()) || now.isAfter(template.getValidUntil())) {
            throw new CustomException(ErrorCode.COUPON_NOT_IN_ISSUE_PERIOD);
        }

        if (couponRepository.existsByCouponTemplateIdAndUserId(template.getId(), userId)) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }
}