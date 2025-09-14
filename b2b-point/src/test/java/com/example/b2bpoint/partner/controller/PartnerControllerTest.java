package com.example.b2bpoint.partner.controller;

import com.example.b2bpoint.partner.domain.Partner;
import com.example.b2bpoint.partner.dto.ApiKeyResponse;
import com.example.b2bpoint.partner.dto.PartnerCreateRequest;
import com.example.b2bpoint.partner.repository.PartnerRepository;
import com.example.b2bpoint.partner.service.PartnerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PartnerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private PartnerService partnerService;

    @Autowired
    private PartnerRepository partnerRepository;

    private Partner testPartner;

    @BeforeEach
    void setUp() {
        testPartner = Partner.builder()
                .name("테스트 파트너")
                .contactEmail("test@partner.com")
                .businessNumber("111-22-33333")
                .build();
        partnerRepository.save(testPartner);
    }


    @DisplayName("파트너사 등록 API호출에 성공한다.")
    @Test
    void createPartnerApi_success() throws Exception {
        //given
        PartnerCreateRequest request = new PartnerCreateRequest("B쇼핑몰", "bMall@gmail.com", "987-65-43210");
        String requestJson = objectMapper.writeValueAsString(request);

        //when&then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/partners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("B쇼핑몰"))
                .andDo(print());
    }

    @DisplayName("API Key 발급에 성공한다.")
    @Test
    void issueApiKey_success() throws Exception {
        // given
        testPartner.approve();
        partnerRepository.save(testPartner);

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/partners/{partnerId}/api-key", testPartner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.partnerId").value(testPartner.getId()))
                .andExpect(jsonPath("$.data.apiKey").exists())
                .andExpect(jsonPath("$.data.apiKey").isString())
                .andDo(print());
    }

    @DisplayName("존재하지 않는 파트너 ID로 API Key 발급 시 404 에러가 발생한다.")
    @Test
    void issueApiKey_fail_partnerNotFound() throws Exception {
        // given
        Long nonExistentPartnerId = 9999L;

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/partners/{partnerId}/api-key", nonExistentPartnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P001"))
                .andDo(print());
    }

    @DisplayName("아직 승인되지 않은(PENDING) 파트너의 API Key 발급 시 400 에러가 발생한다.")
    @Test
    void issueApiKey_fail_partnerNotActive() throws Exception {
        // given


        // when & then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/partners/{partnerId}/api-key", testPartner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P003"))
                .andDo(print());
    }
}