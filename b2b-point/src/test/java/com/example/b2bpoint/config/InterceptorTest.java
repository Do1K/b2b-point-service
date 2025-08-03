package com.example.b2bpoint.config;

import com.example.b2bpoint.partner.domain.Partner;
import com.example.b2bpoint.partner.dto.PartnerCreateRequest;
import com.example.b2bpoint.partner.repository.PartnerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PartnerRepository partnerRepository;

    private Partner activePartner;
    private String validApiKey;

    private static final String PROTECTED_URL = "/api/v1/partners/test-user";

    @BeforeEach
    void setUp() {

        activePartner = Partner.builder()
                .name("활성 파트너")
                .contactEmail("active@partner.com")
                .businessNumber("100-00-00001")
                .build();
        activePartner.approve();
        activePartner.issueApiKey();

        validApiKey = activePartner.getApiKey();
        partnerRepository.save(activePartner);
    }

    @DisplayName("성공: 유효한 API Key로 보호된 리소스 접근 시 성공(200 OK)한다.")
    @Test
    void accessProtectedResource_withValidApiKey_shouldSucceed() throws Exception {
        mockMvc.perform(get(PROTECTED_URL)
                        .header("X-API-KEY", validApiKey)
                )
                .andExpect(status().isOk())
                .andDo(print());
    }

    @DisplayName("실패: API Key 없이 보호된 리소스 접근 시 실패(401 Unauthorized)한다.")
    @Test
    void accessProtectedResource_withoutApiKey_shouldFail() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("A002"))
                .andDo(print());
    }

    @DisplayName("실패: 유효하지 않은 API Key로 보호된 리소스 접근 시 실패(401 Unauthorized)한다.")
    @Test
    void accessProtectedResource_withInvalidApiKey_shouldFail() throws Exception {
        mockMvc.perform(get(PROTECTED_URL)
                        .header("X-API-KEY", "유효하지 않은 키")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("A002"))
                .andDo(print());
    }

    @DisplayName("예외: 보호되지 않는 리소스(파트너사 등록)는 API Key 없이도 성공한다.")
    @Test
    void accessExcludedResource_withoutApiKey_shouldSucceed() throws Exception {
        // given
        PartnerCreateRequest request = new PartnerCreateRequest("새로운 파트너", "new@partner.com", "200-00-00002");
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andDo(print());
    }

}