package com.example.b2bpoint.coupon.repository;

import com.example.b2bpoint.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    boolean existsByCouponTemplateIdAndUserId(Long couponTemplateId, String userId);

    List<Coupon> findByPartnerIdAndUserId(Long partnerId, String userId);
}
