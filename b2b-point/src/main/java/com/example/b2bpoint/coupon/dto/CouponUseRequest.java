package com.example.b2bpoint.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CouponUseRequest {


    @NotBlank(message = "사용자 ID는 필수입니다.")
    private String userId;


    @NotNull(message = "주문 ID는 필수입니다.")
    private Long orderId;


    @NotNull(message = "주문 금액은 필수입니다.")
    @Positive(message = "주문 금액은 0보다 커야 합니다.")
    private Integer orderAmount;

    @Builder
    private CouponUseRequest(String userId, Long orderId, Integer orderAmount) {
        this.userId = userId;
        this.orderId = orderId;
        this.orderAmount = orderAmount;
    }
}