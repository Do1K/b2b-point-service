package com.example.b2bpoint.coupon.application;

import com.example.b2bpoint.coupon.dto.CouponIssueMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final CouponIssueSyncService couponIssueSyncService;
    private static final String QUEUE_NAME = "coupon.issue.queue";
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private static final String COUPON_ISSUE_REQUEST_LIST_KEY = "coupon:issue:requests";


    @RabbitListener(queues = QUEUE_NAME)
    public void receive(CouponIssueMessage message) {
        log.info("Received message from RabbitMQ: {}", message);
        try {
//            couponIssueSyncService.issueCouponWithoutLock(
//                    message.getPartnerId(),
//                    message.getCouponTemplateId(),
//                    message.getUserId(),
//                    message.getValidUntil()
//            );
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush(COUPON_ISSUE_REQUEST_LIST_KEY, messageJson);

            log.info("[{}] 쿠폰 발급 성공: userId: {}",message.getCouponTemplateId(), message.getUserId());
        } catch (Exception e) {

            log.error("쿠폰 발급 실패 userId: {}. Error: {}", message.getUserId(), e.getMessage());
        }
    }
}