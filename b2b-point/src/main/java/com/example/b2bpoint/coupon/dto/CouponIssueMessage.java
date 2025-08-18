package com.example.b2bpoint.coupon.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 메시지 큐를 통해 전달될 데이터 객체
@Getter
@NoArgsConstructor
public class CouponIssueMessage {
    private Long partnerId;
    private Long couponTemplateId;
    private String userId;
    private LocalDateTime validUntil;

    public CouponIssueMessage(Long partnerId, Long couponTemplateId, String userId, LocalDateTime validUntil) {
        this.partnerId = partnerId;
        this.couponTemplateId = couponTemplateId;
        this.userId = userId;
        this.validUntil = validUntil;
    }
}