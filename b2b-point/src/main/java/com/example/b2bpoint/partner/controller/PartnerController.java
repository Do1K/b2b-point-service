package com.example.b2bpoint.partner.controller;

import com.example.b2bpoint.common.dto.ApiResponse;
import com.example.b2bpoint.partner.dto.PartnerCreateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PartnerController {

    @PostMapping
    public ApiResponse<?> createPartner(@RequestBody @Valid PartnerCreateRequest request) {


    }
}
