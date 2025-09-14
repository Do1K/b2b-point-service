package com.example.b2bpoint.partner.domain;

import com.example.b2bpoint.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "partners")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Partner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255, nullable = false)
    private String contactEmail;

    @Column(length = 50, nullable = false, unique = true)
    private String businessNumber;

    @Column(length = 255, unique = true)
    private String apiKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnerStatus status;

    @Builder
    public Partner(String name, String contactEmail, String businessNumber) {
        this.name = name;
        this.contactEmail = contactEmail;
        this.businessNumber = businessNumber;
        this.status = PartnerStatus.PENDING; // 생성 시 기본 상태는 '승인대기'
    }

    /**
     * 파트너사의 상태를 'ACTIVE'로 변경합니다. (관리자 승인)
     */
    public void approve() {
        this.status = PartnerStatus.ACTIVE;
    }

    public void issueApiKey() {
        this.apiKey = UUID.randomUUID().toString();
    }
}
