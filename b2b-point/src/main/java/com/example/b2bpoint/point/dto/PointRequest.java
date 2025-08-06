package com.example.b2bpoint.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PointRequest {

    @NotBlank(message = "사용자 ID는 필수입니다.")
    private String userId;

    @Min(value = 1, message = "포인트 양은 1 이상이어야 합니다.")
    private Integer amount;

    private String description;

    private String partnerOrderId;

    public PointRequest(String userId, Integer amount, String description, String partnerOrderId) {
        this.userId = userId;
        this.amount = amount;
        this.description = description;
        this.partnerOrderId = partnerOrderId;
    }
}
