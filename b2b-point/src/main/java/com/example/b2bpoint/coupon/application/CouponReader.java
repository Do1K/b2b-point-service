package com.example.b2bpoint.coupon.application;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.dto.CouponTemplateCacheDto;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;


@Component
@RequiredArgsConstructor
public class CouponReader {

    private final StringRedisTemplate redisTemplate;
    private final CouponTemplateRepository couponTemplateRepository;
    private final ObjectMapper objectMapper;

    private static final String COUPON_TEMPLATE_KEY = "coupon:template:%d";

    public CouponTemplateCacheDto findTemplateFromCache(Long templateId) {
        String cacheKey = String.format(COUPON_TEMPLATE_KEY, templateId);

        String templateJson = redisTemplate.opsForValue().get(cacheKey);
        if (templateJson != null) {
            try {
                return objectMapper.readValue(templateJson, CouponTemplateCacheDto.class);
            } catch (JsonProcessingException e) {

                return null;
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public CouponTemplateCacheDto findTemplateFromDbAndCache(Long templateId) {
        String cacheKey = String.format(COUPON_TEMPLATE_KEY, templateId);

        CouponTemplate templateFromDb = couponTemplateRepository.findById(templateId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND));

        try {
            CouponTemplateCacheDto dto = CouponTemplateCacheDto.fromEntity(templateFromDb);
            String newTemplateJson = objectMapper.writeValueAsString(dto);

            redisTemplate.opsForValue().set(cacheKey, newTemplateJson, Duration.ofDays(1));
            return dto;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("DTO 직렬화 실패", e);
        }
    }
}