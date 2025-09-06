package com.example.b2bpoint.coupon.application;

import com.example.b2bpoint.coupon.dto.CouponIssueMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;



@SpringBootTest
class CouponIssueConsumerTest {

    // --- 테스트를 위한 별도의 설정 클래스 ---
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ObjectMapper spiedObjectMapper() {
            return Mockito.spy(new ObjectMapper());
        }
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin; // [수정] AmqpAdmin 주입

    @Autowired
    private ObjectMapper objectMapper;

    private static final String QUEUE_NAME = "coupon.issue.queue";
    private static final String DLQ_NAME = "coupon.issue.dlq";

    @BeforeEach
    void setUp() {
        // 테스트 시작 전, 큐를 깨끗하게 비워서 테스트 간 독립성 보장
        amqpAdmin.purgeQueue(DLQ_NAME, true);
        amqpAdmin.purgeQueue(QUEUE_NAME, true);
    }

    @Test
    @DisplayName("실패: 메시지 직렬화 실패 시, 메시지가 DLQ로 전송된다")
    void whenSerializationFails_thenMessageGoesToDlq() throws Exception {
        // given
        CouponIssueMessage testMessage = new CouponIssueMessage(1L, 5L, "dlq-test-user", LocalDateTime.now());

        doThrow(new JsonProcessingException("Test Serialization Failure") {
        })
                .when(objectMapper).writeValueAsString(any(CouponIssueMessage.class));

        // when
        rabbitTemplate.convertAndSend(QUEUE_NAME, testMessage);


        // then: [핵심 수정] 메시지를 직접 꺼내는 대신, DLQ의 메시지 개수를 확인합니다.
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // DLQ의 속성(메시지 개수 포함)을 조회합니다.
                    Properties queueProperties = amqpAdmin.getQueueProperties(DLQ_NAME);
                    // 큐가 존재하고, 메시지 개수가 1개인지 검증합니다.
                    assertThat(queueProperties).isNotNull();
                    assertThat(queueProperties.get("MESSAGE_COUNT")).isEqualTo(1);
                });
    }
}