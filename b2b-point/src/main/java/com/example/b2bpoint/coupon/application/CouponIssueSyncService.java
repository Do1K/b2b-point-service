package com.example.b2bpoint.coupon.application;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.coupon.domain.Coupon;
import com.example.b2bpoint.coupon.domain.CouponTemplate;
import com.example.b2bpoint.coupon.dto.CouponIssueMessage;
import com.example.b2bpoint.coupon.repository.CouponRepository;
import com.example.b2bpoint.coupon.repository.CouponTemplateRepository;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static jakarta.persistence.GenerationType.UUID;
import static java.util.stream.Collectors.toList;


@Service
@RequiredArgsConstructor
public class CouponIssueSyncService {

    private final CouponTemplateRepository couponTemplateRepository;
    private final CouponRepository couponRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void issueCoupon(Long partnerId, Long couponTemplateId, String userId) {
        CouponTemplate template = couponTemplateRepository.findByIdWithLock(couponTemplateId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND));

        validateCouponIssuance(partnerId, template, userId);

        template.increaseIssuedQuantity();

        Coupon coupon = Coupon.builder()
                .partnerId(partnerId)
                .userId(userId)
                .couponTemplate(template)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
    }

    private void validateCouponIssuance(Long partnerId, CouponTemplate template, String userId) {
        if (!template.getPartnerId().equals(partnerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(template.getValidFrom()) || now.isAfter(template.getValidUntil())) {
            throw new CustomException(ErrorCode.COUPON_NOT_IN_ISSUE_PERIOD);
        }

        if (couponRepository.existsByCouponTemplateIdAndUserId(template.getId(), userId)) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    @Transactional
    public void issueCouponWithoutLock(Long partnerId, Long couponTemplateId, String userId,LocalDateTime validUntil) {

        Coupon coupon=Coupon.createFromMessage(partnerId, userId, couponTemplateId, validUntil);

        Coupon savedCoupon = couponRepository.save(coupon);
    }

    @Transactional
    public void issueCouponsAndUpdateQuantityInBatch(List<CouponIssueMessage> messages){
        //issueCouponsInBatch(messages);
        issueCouponsInBatchByJdbc(messages);

        Map<Long, Long> issueCountByTemplateId = messages.stream()
                .collect(Collectors.groupingBy(
                        CouponIssueMessage::getCouponTemplateId,
                        Collectors.counting()
                ));


        issueCountByTemplateId.forEach((templateId, count) -> {
            couponTemplateRepository.increaseIssuedQuantity(templateId, count.intValue());
        });
    }

    private void issueCouponsInBatch(List<CouponIssueMessage> messages) {

        List<Coupon> couponsToSave = messages.stream()
                .map(message -> Coupon.createFromMessage(
                        message.getPartnerId(),
                        message.getUserId(),
                        message.getCouponTemplateId(),
                        message.getValidUntil()
                ))
                .toList();

        if (!couponsToSave.isEmpty()) {
            couponRepository.saveAll(couponsToSave);
        }
    }

    private void issueCouponsInBatchByJdbc(List<CouponIssueMessage> messages) {


        String sql = "INSERT INTO coupons " +
                "(partner_id, user_id, coupon_template_id, status, issued_at, expired_at, code, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int batchSize = 10000;


        List<List<CouponIssueMessage>> partitionedMessages = Lists.partition(messages, batchSize);

        for (List<CouponIssueMessage> chunk : partitionedMessages) {

            List<Coupon> couponsToSave = chunk.stream()
                    .map(message -> Coupon.createFromMessage(
                            message.getPartnerId(),
                            message.getUserId(),
                            message.getCouponTemplateId(),
                            message.getValidUntil()
                    ))
                    .toList();

            jdbcTemplate.batchUpdate(sql,
                    couponsToSave,
                    chunk.size(), // 현재 청크의 실제 크기
                    (PreparedStatement ps, Coupon coupon) -> {
                        ps.setLong(1, coupon.getPartnerId());
                        ps.setString(2, coupon.getUserId());
                        ps.setLong(3, coupon.getCouponTemplateId());
                        ps.setString(4, coupon.getStatus().toString()); // CouponStatus.AVAILABLE
                        ps.setTimestamp(5, Timestamp.valueOf(coupon.getIssuedAt()));
                        ps.setTimestamp(6, Timestamp.valueOf(coupon.getExpiredAt()));
                        ps.setString(7, coupon.getCode());
                        ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                        ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                    });
        }
    }
}