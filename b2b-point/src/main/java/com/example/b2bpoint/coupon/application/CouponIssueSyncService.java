package com.example.b2bpoint.coupon.application;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.dto.CouponIssueMessage;
import com.example.b2bpoint.coupon.repository.CouponRepository;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponIssueSyncService {

    private final CouponTemplateRepository couponTemplateRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public void issueCoupon(Long partnerId, Long couponTemplateId, String userId) {
        CouponTemplate template = couponTemplateRepository.findByIdWithLock(couponTemplateId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND));

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

    @Transactional
    public void issueCouponWithoutLock(Long partnerId, Long couponTemplateId, String userId,LocalDateTime validUntil) {

        Coupon coupon=Coupon.createFromMessage(partnerId, userId, couponTemplateId, validUntil);

        Coupon savedCoupon = couponRepository.save(coupon);
    }

    @Transactional
    public void issueCouponsInBatch(List<CouponIssueMessage> messages) {
        List<Coupon> couponsToSave = messages.stream()
                .map(message -> Coupon.createFromMessage(
                        message.getPartnerId(),
                        message.getUserId(),
                        message.getCouponTemplateId(),
                        message.getValidUntil()
                ))
                .toList();

        if (!couponsToSave.isEmpty()) {
            couponRepository.saveAll(couponsToSave);
        }
    }
}