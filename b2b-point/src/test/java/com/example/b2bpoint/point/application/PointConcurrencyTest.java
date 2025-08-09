package com.example.b2bpoint.point.application;

import com.example.b2bpoint.partner.domain.Partner;
import com.example.b2bpoint.partner.repository.PartnerRepository;
import com.example.b2bpoint.point.domain.PointWallet;
import com.example.b2bpoint.point.repository.PointHistoryRepository;
import com.example.b2bpoint.point.repository.PointWalletRepository;
import com.example.b2bpoint.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointConcurrencyTest {

    @Autowired
    private PointService pointService; // 실제 서비스 로직을 테스트

    @Autowired
    private PointWalletRepository pointWalletRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    private Long partnerId;
    private String userId = "concurrency-test-user";

    @BeforeEach
    void setUp() {
        // 테스트 전에 기존 데이터 삭제 (테스트 독립성 보장)
        pointHistoryRepository.deleteAllInBatch();
        pointWalletRepository.deleteAll();
        partnerRepository.deleteAll();

        // 파트너 생성
        Partner partner = partnerRepository.save(Partner.builder()
                .name("동시성 테스트 파트너")
                .contactEmail("concurrency@test.com")
                .businessNumber("999-99-99999")
                .build());
        this.partnerId = partner.getId();

        // 10000 포인트를 가진 지갑 생성
        PointWallet wallet = PointWallet.builder()
                .partnerId(partnerId)
                .userId(userId)
                .build();
        wallet.earn(10000); // 10000 포인트로 시작
        pointWalletRepository.save(wallet);
    }

    @DisplayName("동시에 100개의 포인트 사용 요청이 발생해도 데이터 정합성이 유지된다 (잔액이 0이 된다).")
    @Test
    void pointUsage_concurrencyTest() throws InterruptedException {
        // given (준비)
        final int numberOfThreads = 100; // 동시에 실행할 스레드 수
        final int pointsToUse = 100; // 각 스레드가 사용할 포인트

        // 멀티 스레드 환경 구성
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads); // 모든 스레드가 작업을 완료할 때까지 기다리게 할 '카운터'

        // when (실행)
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    // 서비스의 포인트 사용 메서드를 호출
                    // 실제로는 PointUseRequest DTO를 만들어서 전달해야 함
                    pointService.use(partnerId, userId, pointsToUse, "동시성 테스트");
                } finally {
                    latch.countDown(); // 작업이 성공하든 실패하든 카운터를 1 감소시킴
                }
            });
        }

        latch.await(); // 모든 스레드의 작업이 끝날 때까지(카운터가 0이 될 때까지) 메인 스레드는 여기서 대기
        executorService.shutdown();

        // then (검증)
        PointWallet finalWallet = pointWalletRepository.findByPartnerIdAndUserId(partnerId, userId).get();

        // 10000 - (100 * 100) = 0
        System.out.println("Final Points: " + finalWallet.getPoints());
        assertThat(finalWallet.getPoints()).isEqualTo(0);
    }
}
