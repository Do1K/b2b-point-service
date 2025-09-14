package com.example.b2bpoint.coupon.repository;

import com.example.b2bpoint.coupon.domain.CouponTemplate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ct from CouponTemplate ct where ct.id = :id")
    Optional<CouponTemplate> findByIdWithLock(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CouponTemplate ct SET ct.issuedQuantity = ct.issuedQuantity + :issueCount WHERE ct.id = :templateId")
    void increaseIssuedQuantity(@Param("templateId") Long templateId, @Param("issueCount") int issueCount);

}
