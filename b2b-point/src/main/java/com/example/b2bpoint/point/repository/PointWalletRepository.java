package com.example.b2bpoint.point.repository;

import com.example.b2bpoint.point.domain.PointWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointWalletRepository extends JpaRepository<PointWallet, Long> {
    Optional<PointWallet> findByPartnerIdAndUserId(Long partnerId, String userId);
}

