package com.example.b2bpoint.point.controller;

import com.example.b2bpoint.common.dto.ApiResponse;
import com.example.b2bpoint.point.dto.PointChargeRequest;
import com.example.b2bpoint.point.dto.PointResponse;
import com.example.b2bpoint.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @PostMapping("/charge")
    public ApiResponse<PointResponse> chargePoints(
            @RequestAttribute Long partnerId,
            @RequestBody @Valid PointChargeRequest request
            ){
        PointResponse response=pointService.charge(partnerId, request.getUserId(), request.getAmount(), request.getDescription());

        return ApiResponse.success(response);
    }
}
