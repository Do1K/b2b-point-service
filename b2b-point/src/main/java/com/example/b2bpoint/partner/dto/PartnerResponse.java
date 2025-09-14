package com.example.b2bpoint.partner.dto;

import com.example.b2bpoint.partner.domain.Partner;
import com.example.b2bpoint.partner.domain.PartnerStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PartnerResponse {

    private final Long id;
    private final String name;
    private final String contactEmail;
    private final String businessNumber;
    private final PartnerStatus status;
    private final LocalDateTime createdAt;

    @Builder
    private PartnerResponse(Long id, String name, String contactEmail, String businessNumber, PartnerStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.contactEmail = contactEmail;
        this.businessNumber = businessNumber;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static PartnerResponse from(Partner partner) {
        return PartnerResponse.builder()
                .id(partner.getId())
                .name(partner.getName())
                .contactEmail(partner.getContactEmail())
                .businessNumber(partner.getBusinessNumber())
                .status(partner.getStatus())
                .createdAt(partner.getCreatedAt())
                .build();
    }
}
