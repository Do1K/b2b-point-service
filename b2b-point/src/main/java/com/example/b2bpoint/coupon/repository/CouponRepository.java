package com.example.b2bpoint.coupon.repository;

import com.example.b2bpoint.coupon.domain.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    boolean existsByCouponTemplateIdAndUserId(Long couponTemplateId, String userId);

    List<Coupon> findByPartnerIdAndUserId(Long partnerId, String userId);

    @Query("SELECT c FROM Coupon c JOIN FETCH c.couponTemplate ct WHERE c.userId = :userId AND c.partnerId = :partnerId")
    List<Coupon> findCouponsWithTemplateByUserIdAndPartnerId(@Param("partnerId") Long partnerId, @Param("userId") String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code")
    Optional<Coupon> findByCodeWithLock(@Param("code") String code);

}
