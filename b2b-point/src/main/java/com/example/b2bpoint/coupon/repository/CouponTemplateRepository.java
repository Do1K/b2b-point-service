package com.example.b2bpoint.coupon.repository;

import com.example.b2bpoint.coupon.domain.CouponTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, Long> {
}
