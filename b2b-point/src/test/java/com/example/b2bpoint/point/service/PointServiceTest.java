package com.example.b2bpoint.point.service;

import com.example.b2bpoint.point.domain.PointHistory;
import com.example.b2bpoint.point.domain.PointWallet;
import com.example.b2bpoint.point.domain.TransactionType;
import com.example.b2bpoint.point.dto.PointResponse;
import com.example.b2bpoint.point.repository.PointHistoryRepository;
import com.example.b2bpoint.point.repository.PointWalletRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointWalletRepository walletRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private PointService pointService;

    private final Long partnerId = 1L;
    private final String userId = "user123";
    private final int amount = 1000;
    private final String description = "충전 테스트";

    @DisplayName("지갑이 없으면 새로 생성하고 충전한다.")
    @Test
    void charge_success_whenNotingWallet() {
        //given
        BDDMockito.given(walletRepository.findByPartnerIdAndUserId(partnerId, userId))
                .willReturn(Optional.empty());

        PointWallet newWallet = PointWallet.create(partnerId,userId);

        BDDMockito.given(walletRepository.save(BDDMockito.any(PointWallet.class)))
                .willReturn(newWallet);

        //when
        PointResponse pointResponse = pointService.charge(partnerId, userId, amount, description);

        //then
        verify(walletRepository).save(BDDMockito.any(PointWallet.class));
        verify(pointHistoryRepository).save(BDDMockito.any(PointHistory.class));
        assertThat(pointResponse.getUserId()).isEqualTo(userId);
        assertThat(pointResponse.getPoints()).isEqualTo(amount);

    }


    @DisplayName("기존 지갑에 충전하고 히스토리 저장")
    @Test
    void charge_success_whenWalletExists() {
        //given
        PointWallet wallet = PointWallet.create(partnerId,userId);

        BDDMockito.given(walletRepository.findByPartnerIdAndUserId(partnerId, userId))
                .willReturn(Optional.of(wallet));

        //when
        PointResponse pointResponse = pointService.charge(partnerId, userId, amount, description);

        //then
        verify(walletRepository, never()).save(BDDMockito.any(PointWallet.class));
        verify(pointHistoryRepository).save(BDDMockito.any(PointHistory.class));
        assertThat(pointResponse.getUserId()).isEqualTo(userId);
        assertThat(pointResponse.getPoints()).isEqualTo(amount);

    }

    @DisplayName("charge: 히스토리 정상적으로 저장되는지 확인")
    @Test
    void charge_success_with_history() {
        //given

        PointWallet wallet = PointWallet.create(partnerId, userId);
        BDDMockito.given(walletRepository.findByPartnerIdAndUserId(partnerId, userId))
                .willReturn(Optional.of(wallet));

        // when
        pointService.charge(partnerId, userId, amount, description);

        // then
        ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(historyCaptor.capture());
        PointHistory history = historyCaptor.getValue();

        assertThat(history.getAmount()).isEqualTo(amount);
        assertThat(history.getDescription()).isEqualTo(description);
        assertThat(history.getTransactionType()).isEqualTo(TransactionType.EARN);
        assertThat(history.getPointWallet()).isEqualTo(wallet);
    }

    @DisplayName("charge: 적립할 포인트는 0 보다 커야합니다.")
    @Test
    void charge_fail_whenAmountNotEnough() {
        //given
        PointWallet wallet = PointWallet.create(partnerId, userId);
        BDDMockito.given(walletRepository.findByPartnerIdAndUserId(partnerId, userId))
                .willReturn(Optional.of(wallet));

        //when&then
        assertThrows(IllegalArgumentException.class, () -> pointService.charge(partnerId, userId, 0, description));

    }

}