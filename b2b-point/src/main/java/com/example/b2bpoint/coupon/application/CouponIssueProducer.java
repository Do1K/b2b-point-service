package com.example.b2bpoint.coupon.application;

import com.example.b2bpoint.coupon.dto.CouponIssueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueProducer {

    private static final String EXCHANGE_NAME = "coupon.exchange";
    private static final String ROUTING_KEY = "coupon.issue.request";
    private final RabbitTemplate rabbitTemplate;

    public void send(CouponIssueMessage message) {
        log.info("Sending message to RabbitMQ: {}", message);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, message);
    }
}