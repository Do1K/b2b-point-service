package com.example.b2bpoint.partner.service;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.partner.domain.Partner;
import com.example.b2bpoint.partner.dto.ApiKeyResponse;
import com.example.b2bpoint.partner.dto.PartnerCreateRequest;
import com.example.b2bpoint.partner.dto.PartnerResponse;
import com.example.b2bpoint.partner.repository.PartnerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @InjectMocks
    private PartnerService partnerService;

    @Mock
    private PartnerRepository partnerRepository;

    @DisplayName("파트너사 등록 성공")
    @Test
    void createPartner_success() {
        //given
        PartnerCreateRequest request=new PartnerCreateRequest("A쇼핑몰", "Amall@gmail.com", "02-12-4567");

        given(partnerRepository.findByBusinessNumber(request.getBusinessNumber())).willReturn(Optional.empty());

        Partner fakeSavedPartner = Partner.builder()
                .name(request.getName())
                .contactEmail(request.getContactEmail())
                .businessNumber(request.getBusinessNumber())
                .build();
        given(partnerRepository.save(any(Partner.class))).willReturn(fakeSavedPartner);

        //when
        PartnerResponse response = partnerService.createPartner(request);

        //then
        assertThat(response.getName()).isEqualTo("A쇼핑몰");
        assertThat(response.getContactEmail()).isEqualTo("Amall@gmail.com");
        assertThat(response.getBusinessNumber()).isEqualTo("02-12-4567");

        verify(partnerRepository).save(any(Partner.class));
    }

    @DisplayName("이미 존재하는 사업자 번호로 등록 시 예외 발생")
    @Test
    void createPartner_fail_whenDuplicateBusinessNumber() {
        //given
        PartnerCreateRequest request=new PartnerCreateRequest("A쇼핑몰", "Amall@gmail.com", "02-123-4567");

        given(partnerRepository.findByBusinessNumber(request.getBusinessNumber()))
                .willReturn(Optional.of(Partner.builder().businessNumber("02-12-4567").build()));

        //when&then
        assertThrows(CustomException.class, () -> {
            partnerService.createPartner(request);
        });

    }

    @DisplayName("apiKey 발급 성공")
    @Test
    void issueApiKey_success() {
        //given
        Long partnerId=1L;

        Partner fakeSavedPartner = Partner.builder()
                .build();

        fakeSavedPartner.approve();
        given(partnerRepository.findById(partnerId)).willReturn(Optional.of(fakeSavedPartner));

        //when
        ApiKeyResponse response = partnerService.issueApiKey(partnerId);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("성공");

    }

    @DisplayName("승인 받지 않은 파트너사는 apiKey 발급받을 수 없음")
    @Test
    void issueApiKey_fail_whenStatusNotActive() {

        //given
        Long partnerId=1L;

        Partner fakeSavedPartner=Partner.builder()
                .build();

        given(partnerRepository.findById(partnerId)).willReturn(Optional.of(fakeSavedPartner));

        //when&then
        assertThrows(CustomException.class, () -> {
            partnerService.issueApiKey(partnerId);
        });


    }

}