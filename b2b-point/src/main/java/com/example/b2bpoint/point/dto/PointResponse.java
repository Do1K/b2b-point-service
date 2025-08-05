package com.example.b2bpoint.point.dto;

import com.example.b2bpoint.point.domain.PointWallet;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class PointResponse {
    private final String userId;
    private final int points;

    @Builder
    private PointResponse(String userId, int points) {
        this.userId = userId;
        this.points = points;
    }

    public static PointResponse from(PointWallet pointWallet){
        return PointResponse.builder()
                .userId(pointWallet.getUserId())
                .points(pointWallet.getPoints())
                .build();
    }
}
