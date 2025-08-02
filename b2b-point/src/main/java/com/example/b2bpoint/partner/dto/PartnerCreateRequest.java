package com.example.b2bpoint.partner.dto;

import com.example.b2bpoint.partner.domain.Partner;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartnerCreateRequest {

    @NotBlank(message = "파트너사 이름은 필수입니다.") // 유효성 검증: null, "", " " 모두 허용 안 함
    private String name;

    @Email(message = "유효한 이메일 형식이 아닙니다.") // 유효성 검증: 이메일 형식
    @NotBlank(message = "담당자 이메일은 필수입니다.")
    private String contactEmail;

    @NotBlank(message = "사업자 번호는 필수입니다.")
    private String businessNumber;

    public Partner toEntity() {
        return Partner.builder()
                .name(this.name)
                .contactEmail(this.contactEmail) // 엔티티에 해당 필드가 있다면
                .businessNumber(this.businessNumber) // 엔티티에 해당 필드가 있다면
                .build();
    }
}
