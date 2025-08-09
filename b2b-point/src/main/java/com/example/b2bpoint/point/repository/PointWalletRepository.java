package com.example.b2bpoint.point.repository;

import com.example.b2bpoint.point.domain.PointWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointWalletRepository extends JpaRepository<PointWallet, Long> {
    Optional<PointWallet> findByPartnerIdAndUserId(Long partnerId, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PointWallet p where p.partnerId = :partnerId and p.userId = :userId")
    Optional<PointWallet> findByPartnerIdAndUserIdWithLock(
            @Param("partnerId") Long partnerId,
            @Param("userId") String userId
    );
}

