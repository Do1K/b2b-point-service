package com.example.b2bpoint.config;

import com.example.b2bpoint.common.exception.CustomException;
import com.example.b2bpoint.common.exception.ErrorCode;
import com.example.b2bpoint.partner.domain.Partner;
import com.example.b2bpoint.partner.domain.PartnerStatus;
import com.example.b2bpoint.partner.repository.PartnerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    private final PartnerRepository partnerRepository;
    private static final String API_KEY_HEADER = "X-API-KEY";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        log.info("API Key Auth Interceptor - preHandle: {}", request.getRequestURI());

        String apiKey = request.getHeader(API_KEY_HEADER);

        if(apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("API Key 가 request header 에 없음");
            throw new CustomException(ErrorCode.INVALID_API_KEY);
        }

        Partner partner = partnerRepository.findByApiKey(apiKey)
                .orElseThrow(() -> {
                    log.warn("유효하지 않은 API Key: {}", apiKey);
                    return new CustomException(ErrorCode.INVALID_API_KEY);
                });

        if(partner.getStatus()!= PartnerStatus.ACTIVE){
            log.warn("비활성화 파트너: {}", partner.getName());
            throw new CustomException(ErrorCode.PARTNER_NOT_ACTIVE);
        }

        request.setAttribute("partnerId", partner.getId());

        return true;

    }
}
