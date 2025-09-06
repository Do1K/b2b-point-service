package com.example.b2bpoint.coupon.service;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.coupon.application.CouponIssueProducer;
import com.example.b2bpoint.coupon.application.CouponReader;
import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.dto.*;
import com.example.b2bpoint.coupon.repository.CouponRepository;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponTemplateRepository couponTemplateRepository;
    private final CouponRepository couponRepository;

    private final StringRedisTemplate redisTemplate;
    private final CouponIssueProducer couponIssueProducer;
    private final ObjectMapper objectMapper;
    private final CouponReader couponReader;

    private static final String COUPON_COUNT_KEY = "coupon:template:%d:count";
    private static final String COUPON_USERS_KEY = "coupon:template:%d:users";
    private static final String COUPON_TEMPLATE_KEY = "coupon:template:%d";

    @Transactional
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

        CouponTemplateCacheDto cacheDto = CouponTemplateCacheDto.fromEntity(savedTemplate);

        try {
            String cacheKey = String.format(COUPON_TEMPLATE_KEY, savedTemplate.getId());
            String templateJson = objectMapper.writeValueAsString(cacheDto);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime validFrom = savedTemplate.getValidFrom();
            Duration ttl;

            if (validFrom.isBefore(now)) {
                ttl = Duration.ofDays(1);
            } else {
                Duration durationUntilValid = Duration.between(now, validFrom);
                ttl = durationUntilValid.plusDays(1);
            }
            redisTemplate.opsForValue().set(cacheKey, templateJson, ttl);
        } catch (JsonProcessingException e) {
            log.error("쿠폰 템플릿 JSON 직렬화 실패. Template ID: {}", savedTemplate.getId(), e);

            String cacheKey = String.format(COUPON_TEMPLATE_KEY, savedTemplate.getId());
            redisTemplate.delete(cacheKey);
        }

        return CouponTemplateResponse.from(savedTemplate);
    }

    public CouponResponse issueCoupon(Long partnerId, CouponIssueRequest request){

        CouponTemplate couponTemplate=couponTemplateRepository.findByIdWithLock(request.getCouponTemplateId())
                .orElseThrow(()->new CustomException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND));


        validateCouponIssuance(partnerId, CouponTemplateCacheDto.fromEntity(couponTemplate));

        couponTemplate.increaseIssuedQuantity();

        Coupon coupon = Coupon.builder()
                .partnerId(partnerId)
                .userId(request.getUserId())
                .couponTemplate(couponTemplate)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        return CouponResponse.from(savedCoupon);
    }

    private void validateCouponIssuance(Long partnerId, CouponTemplateCacheDto template) {
        if (!template.getPartnerId().equals(partnerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(template.getValidFrom()) || now.isAfter(template.getValidUntil())) {
            throw new CustomException(ErrorCode.COUPON_NOT_IN_ISSUE_PERIOD);
        }

    }

    public CouponIssueResponse issueCouponAsync(Long partnerId, CouponIssueRequest request) {
        Long templateId = request.getCouponTemplateId();
        String userId = request.getUserId();

        CouponTemplateCacheDto couponTemplate=getCouponTemplateAvoidingStampede(templateId);
        Integer totalQuantity = couponTemplate.getTotalQuantity();

        validateCouponIssuance(partnerId, couponTemplate);


        String countKey = String.format(COUPON_COUNT_KEY, templateId);
        String usersKey = String.format(COUPON_USERS_KEY, templateId);

        Long addedCount = redisTemplate.opsForSet().add(usersKey, userId);
        if (addedCount == 0) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
            //return CouponIssueResult.fail(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        Long currentCount = redisTemplate.opsForValue().increment(countKey);

        if (currentCount > totalQuantity) {
            redisTemplate.opsForSet().remove(usersKey, userId);

            throw new CustomException(ErrorCode.COUPON_ISSUE_QUANTITY_EXCEEDED);
            //return CouponIssueResult.fail(ErrorCode.COUPON_ISSUE_QUANTITY_EXCEEDED);
        }

        // --- 여기까지 통과하면 '성공 대상'으로 확정 ---


        try {
            couponIssueProducer.send(new CouponIssueMessage(partnerId, templateId, userId,couponTemplate.getValidUntil()));
        } catch (Exception e) {

            redisTemplate.opsForValue().decrement(countKey);
            redisTemplate.opsForSet().remove(usersKey, userId);
            throw new CustomException(ErrorCode.MESSAGING_SYSTEM_ERROR);
        }
        CouponIssueResponse response = CouponIssueResponse.builder()
                .message("쿠폰이 성공적으로 발급되었습니다.")
                .build();

        return response;
    }


    private CouponTemplateCacheDto getCouponTemplateAvoidingStampede(Long templateId) {
        CouponTemplateCacheDto dto = couponReader.findTemplateFromCache(templateId);
        if (dto != null) {
            return dto;
        }

        String lockKey = "lock:coupon:template:" + templateId;
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofSeconds(5));

            if (Boolean.TRUE.equals(acquired)) {

                try {
                    return couponReader.findTemplateFromDbAndCache(templateId);
                } finally {
                    redisTemplate.delete(lockKey);
                }
            } else {
                Thread.sleep(100);
                return getCouponTemplateAvoidingStampede(templateId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted", e);
        }
    }

    public List<CouponResponse> getCoupons(Long partnerId, String userId) {

        List<Coupon> coupons=couponRepository.findCouponsWithTemplateByUserIdAndPartnerId(partnerId, userId);

        List<CouponResponse> response=new ArrayList<>();
        coupons.forEach(coupon->{
            response.add(CouponResponse.from(coupon));
        });
        return response;

    }

}
