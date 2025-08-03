package com.example.b2bpoint.partner.controller;

import com.example.b2bpoint.common.dto.ApiResponse;
import com.example.b2bpoint.partner.dto.ApiKeyResponse;
import com.example.b2bpoint.partner.dto.PartnerCreateRequest;
import com.example.b2bpoint.partner.dto.PartnerResponse;
import com.example.b2bpoint.partner.service.PartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PartnerResponse> createPartner(@RequestBody @Valid PartnerCreateRequest request) {
        PartnerResponse partnerResponse=partnerService.createPartner(request);

        return ApiResponse.success(partnerResponse);
    }

    @PostMapping("/{partnerId}/api-key")
    public ApiResponse<ApiKeyResponse> issueApiKey(@PathVariable Long partnerId){
        ApiKeyResponse apiKeyResponse=partnerService.issueApiKey(partnerId);

        return ApiResponse.success(apiKeyResponse);
    }

    @GetMapping("/{userId}")
    public ApiResponse<String> partnerForTest(
            @RequestAttribute Long partnerId,
            @PathVariable String userId) {
        // 이 메서드는 인터셉터가 정상 통과했는지만 확인하는 용도
        String responseMessage = "테스트";
        return ApiResponse.success(responseMessage);
    }
}
