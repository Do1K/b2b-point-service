package com.example.b2bpoint.partner.service;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.partner.domain.Partner;
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

    private void validatePartner(String businessNumber){
        partnerRepository.findByBusinessNumber(businessNumber)
                .ifPresent(partner -> {
                    throw new CustomException(ErrorCode.PARTNER_ALREADY_EXISTS);
                });
    }
}
