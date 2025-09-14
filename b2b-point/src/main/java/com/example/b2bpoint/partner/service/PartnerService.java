package com.example.b2bpoint.partner.service;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.partner.domain.Partner;
import com.example.b2bpoint.partner.domain.PartnerStatus;
import com.example.b2bpoint.partner.dto.ApiKeyResponse;
import com.example.b2bpoint.partner.dto.PartnerCreateRequest;
import com.example.b2bpoint.partner.dto.PartnerResponse;
import com.example.b2bpoint.partner.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PartnerService {

    private final PartnerRepository partnerRepository;


    public PartnerResponse createPartner(PartnerCreateRequest request){

        validatePartner(request.getBusinessNumber());

        Partner partner=request.toEntity();

        Partner savedPartner=partnerRepository.save(partner);

        return PartnerResponse.from(savedPartner);
    }

    public ApiKeyResponse issueApiKey(Long partnerId){
        Partner partner=findPartnerById(partnerId);

        if(partner.getStatus()!= PartnerStatus.ACTIVE){
            throw new CustomException(ErrorCode.PARTNER_NOT_ACTIVE);
        }

        partner.issueApiKey();

        return ApiKeyResponse.builder()
                .partnerId(partnerId)
                .apiKey(partner.getApiKey())
                .message("API Key가 성공적으로 발급되었습니다. 이 Key는 다시 확인할 수 없으니 안전한 곳에 즉시 보관하세요.")
                .build();
    }

    private void validatePartner(String businessNumber){
        partnerRepository.findByBusinessNumber(businessNumber)
                .ifPresent(partner -> {
                    throw new CustomException(ErrorCode.PARTNER_ALREADY_EXISTS);
                });
    }

    private Partner findPartnerById(Long partnerId) {
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTNER_NOT_FOUND));
    }
}
