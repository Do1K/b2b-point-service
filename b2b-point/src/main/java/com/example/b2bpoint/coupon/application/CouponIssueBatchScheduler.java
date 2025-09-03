package com.example.b2bpoint.coupon.application;

import com.example.b2bpoint.coupon.dto.CouponIssueMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueBatchScheduler {

    private final StringRedisTemplate redisTemplate;
    private final CouponIssueSyncService couponIssueSyncService;
    private final ObjectMapper objectMapper;
    private static final String COUPON_ISSUE_REQUEST_LIST_KEY = "coupon:issue:requests";

    @Scheduled(fixedDelay = 10000)
    public void processCouponIssueRequests() {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(COUPON_ISSUE_REQUEST_LIST_KEY))) {
            return;
        }
        String processingKey = COUPON_ISSUE_REQUEST_LIST_KEY + ":processing:" + UUID.randomUUID();


        try {

            Boolean renamed = redisTemplate.renameIfAbsent(COUPON_ISSUE_REQUEST_LIST_KEY, processingKey);

            if (Boolean.TRUE.equals(renamed)) {
                List<String> messageJsonList = redisTemplate.opsForList().range(processingKey, 0, -1);

                if (messageJsonList == null || messageJsonList.isEmpty()) {
                    return;
                }

                List<CouponIssueMessage> messages = messageJsonList.stream()
                        .map(json -> {
                            try {
                                return objectMapper.readValue(json, CouponIssueMessage.class);
                            } catch (JsonProcessingException e) {
                                log.error("메시지 역직렬화 실패: {}", json, e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList();

                if (!messages.isEmpty()) {
                    log.info("{}개의 쿠폰 발급 요청을 배치 처리합니다.", messages.size());
                    couponIssueSyncService.issueCouponsAndUpdateQuantityInBatch(messages);
                }

            }

        } finally {
            redisTemplate.delete(processingKey);
        }
    }
}
