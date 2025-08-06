package com.example.b2bpoint.point.service;

import com.example.b2bpoint.point.domain.PointHistory;
import com.example.b2bpoint.point.domain.PointWallet;
import com.example.b2bpoint.point.domain.TransactionType;
import com.example.b2bpoint.point.dto.PointResponse;
import com.example.b2bpoint.point.repository.PointHistoryRepository;
import com.example.b2bpoint.point.repository.PointWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PointResponse charge(Long partnerId, String userId, int amount, String description) {

        PointWallet wallet = pointWalletRepository.findByPartnerIdAndUserId(partnerId, userId)
                .orElseGet(() -> {
                    PointWallet newWallet = PointWallet.create(partnerId, userId);
                    return pointWalletRepository.save(newWallet);
                });

        wallet.earn(amount);

        PointHistory history = PointHistory.builder()
                .pointWallet(wallet)
                .transactionType(TransactionType.EARN)
                .amount(amount)
                .description(description)
                .build();

        pointHistoryRepository.save(history);

        return PointResponse.from(wallet);
    }

    public PointResponse use(Long partnerId, String userId, int amount, String description) {

        PointWallet wallet = pointWalletRepository.findByPartnerIdAndUserId(partnerId, userId)
                .orElseGet(() -> {
                    PointWallet newWallet = PointWallet.create(partnerId, userId);
                    return pointWalletRepository.save(newWallet);
                });

        wallet.use(amount);

        PointHistory history = PointHistory.builder()
                .pointWallet(wallet)
                .transactionType(TransactionType.USE)
                .amount(amount)
                .description(description)
                .build();

        pointHistoryRepository.save(history);

        return PointResponse.from(wallet);
    }
}
