package com.example.b2bpoint.partner.controller;

import com.example.b2bpoint.partner.dto.PartnerCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
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
}