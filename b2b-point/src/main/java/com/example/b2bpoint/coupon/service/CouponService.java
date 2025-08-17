package com.example.b2bpoint.coupon.service;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.coupon.application.CouponIssueProducer;
import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.dto.*;
import com.example.b2bpoint.coupon.repository.CouponRepository;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponTemplateRepository couponTemplateRepository;
    private final CouponRepository couponRepository;

    private final StringRedisTemplate redisTemplate;
    private final CouponIssueProducer couponIssueProducer;

    private static final String COUPON_COUNT_KEY = "coupon:template:%d:count";
    private static final String COUPON_USERS_KEY = "coupon:template:%d:users";


    public CouponTemplateResponse createCouponTemplate(Long partnerId, CouponTemplateCreateRequest request){
        if(request.getValidFrom().isAfter(request.getValidUntil())){
            throw new IllegalArgumentException("유효 기간 설정이 잘못되었습니다. ");
        }

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


        CouponTemplate savedTemplate = couponTemplateRepository.save(couponTemplate);

        if(savedTemplate.getIssuedQuantity()!=null){
            String countKey = String.format(COUPON_COUNT_KEY, savedTemplate.getId());
            Boolean hasKey = redisTemplate.hasKey(countKey);
            if (Boolean.FALSE.equals(hasKey)) {
                redisTemplate.opsForValue().set(countKey, "0");
            }
        }


        return CouponTemplateResponse.from(savedTemplate);
    }

    public CouponResponse issueCoupon(Long partnerId, CouponIssueRequest request){

        CouponTemplate couponTemplate=couponTemplateRepository.findByIdWithLock(request.getCouponTemplateId())
                .orElseThrow(()->new CustomException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND));

        validateCouponIssuance(partnerId, couponTemplate, request.getUserId());

        couponTemplate.increaseIssuedQuantity();

        Coupon coupon = Coupon.builder()
                .partnerId(partnerId)
                .userId(request.getUserId())
                .couponTemplate(couponTemplate)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        return CouponResponse.from(savedCoupon);
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

    public CouponIssueResponse issueCouponAsync(Long partnerId, CouponIssueRequest request) {
        Long templateId = request.getCouponTemplateId();
        String userId = request.getUserId();

        // [준비] 실제 서비스라면 템플릿 생성 시 Redis에 totalQuantity를 저장해두는 것이 좋음
        CouponTemplate template = couponTemplateRepository.findById(templateId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND));
        Integer totalQuantity = template.getTotalQuantity();

        if (totalQuantity == null) {
            // ... 무제한 쿠폰 처리 로직 (여기서는 생략) ...
        }

        String countKey = String.format(COUPON_COUNT_KEY, templateId);
        String usersKey = String.format(COUPON_USERS_KEY, templateId);

        // a. [1차 컷오프 - 중복 발급]
        // SADD 명령어는 Set에 멤버를 추가하고, 성공적으로 추가된 멤버의 수를 반환 (1: 새로 추가, 0: 이미 존재)
        Long addedCount = redisTemplate.opsForSet().add(usersKey, userId);
        if (addedCount == 0) {
            // 이미 발급받은 사용자
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        // b. [1차 컷오프 - 수량 소진]
        Long currentCount = redisTemplate.opsForValue().increment(countKey);

        if (currentCount > totalQuantity) {
            // 수량 초과 시, 방금 Set에 잘못 추가한 userId를 다시 제거해줌 (보정 작업)
            redisTemplate.opsForSet().remove(usersKey, userId);
            throw new CustomException(ErrorCode.COUPON_ISSUE_QUANTITY_EXCEEDED);
        }

        // --- 여기까지 통과하면 '성공 대상'으로 확정 ---

        // c. [성공 대상만] RabbitMQ에 메시지 전송
        try {
            couponIssueProducer.send(new CouponIssueMessage(partnerId, templateId, userId));
        } catch (Exception e) {
            // 메시지 전송 실패 시, 보정 작업 필요
            // 1. 발급 카운트 되돌리기
            redisTemplate.opsForValue().decrement(countKey);
            // 2. Set에서 사용자 제거
            redisTemplate.opsForSet().remove(usersKey, userId);
            // 3. 서버 에러 응답
            throw new CustomException(ErrorCode.MESSAGING_SYSTEM_ERROR);
        }

        // d. 사용자에게 '성공' 응답 반환
        return CouponIssueResponse.builder()
                .message("쿠폰이 성공적으로 발급되었습니다.")
                .build();
    }
}
