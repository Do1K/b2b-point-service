package com.example.b2bpoint.point.controller;

import com.example.b2bpoint.partner.domain.Partner;
import com.example.b2bpoint.partner.repository.PartnerRepository;
import com.example.b2bpoint.point.domain.PointWallet;
import com.example.b2bpoint.point.dto.PointRequest;
import com.example.b2bpoint.point.repository.PointHistoryRepository;
import com.example.b2bpoint.point.repository.PointWalletRepository;
import com.example.b2bpoint.point.service.PointService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PointService pointService;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private PointWalletRepository pointWalletRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    private Partner testPartner;
    private String validApiKey;
    private static final String CHARGE_URL = "/api/v1/points/charge";

    @BeforeEach
    void setUp() {
        testPartner = Partner.builder()
                .name("테스트 파트너")
                .contactEmail("test@partner.com")
                .businessNumber("111-22-33333")
                .build();

        testPartner.approve();
        testPartner.issueApiKey();
        validApiKey = testPartner.getApiKey();
        partnerRepository.save(testPartner);
    }

    @DisplayName("성공: 신규 사용자의 포인트를 처음으로 적립한다.")
    @Test
    void charge_newUser_success() throws Exception {
        //given
        String newUserId = "new-user-123";
        PointRequest request = new PointRequest(newUserId, 1000, "신규 가입 축하 포인트", null);
        String requestBody = objectMapper.writeValueAsString(request);

        //when&then
        mockMvc.perform(post(CHARGE_URL)
                .header("X-API-KEY", validApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(newUserId))
                .andExpect(jsonPath("$.data.points").value(1000))
                .andDo(print());

        PointWallet wallet= pointWalletRepository.findByPartnerIdAndUserId(testPartner.getId(), newUserId).get();
        assertThat(wallet.getPoints()).isEqualTo(1000);

    }

    @DisplayName("성공: 기존 사용자의 포인트를 추가로 적립한다.")
    @Test
    void charge_existUser_success() throws Exception {
        //given
        String existingUserId = "existing-user-456";
        PointWallet existingWallet = PointWallet.builder()
                .partnerId(testPartner.getId())
                .userId(existingUserId)
                .build();
        existingWallet.earn(500);
        pointWalletRepository.save(existingWallet);

        PointRequest request= new PointRequest(existingUserId, 1000, "이벤트 참여 보상", null);
        String requestBody = objectMapper.writeValueAsString(request);

        //when&then
        mockMvc.perform(post(CHARGE_URL)
                .header("X-API-KEY", validApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(existingUserId))
                .andExpect(jsonPath("$.data.points").value(1500));

        PointWallet wallet= pointWalletRepository.findByPartnerIdAndUserId(testPartner.getId(), existingUserId).get();
        assertThat(wallet.getPoints()).isEqualTo(1500);

    }

    @DisplayName("실패: 유효하지 않은 요청(음수 포인트)으로 적립 시 400 에러가 발생한다.")
    @Test
    void chargePoints_withInvalidRequest_shouldFail() throws Exception {
        // given
        String userId = "user-789";
        PointRequest request = new PointRequest(userId, -100, "잘못된 요청", null);
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post(CHARGE_URL)
                        .header("X-API-KEY", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isBadRequest()) // 400 Bad Request
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C001")) // INVALID_INPUT_VALUE
                .andDo(print());
    }
}