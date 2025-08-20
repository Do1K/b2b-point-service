package com.example.b2bpoint.coupon.application;

import com.example.b2bpoint.coupon.dto.CouponIssueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final CouponIssueSyncService couponIssueSyncService;
    private static final String QUEUE_NAME = "coupon.issue.queue";

    @RabbitListener(queues = QUEUE_NAME)
    public void receive(CouponIssueMessage message) {
        log.info("Received message from RabbitMQ: {}", message);
        try {
            couponIssueSyncService.issueCouponWithoutLock(
                    message.getPartnerId(),
                    message.getCouponTemplateId(),
                    message.getUserId()
            );
            log.info("Coupon issued successfully for userId: {}", message.getUserId());
        } catch (Exception e) {
            // [중요] 메시지 처리 실패 시 로깅 및 예외 처리
            // 실제 서비스에서는 Dead Letter Queue(DLQ)로 메시지를 보내 재처리하거나 분석해야 함
            log.error("Failed to issue coupon for userId: {}. Error: {}", message.getUserId(), e.getMessage());
        }
    }
}