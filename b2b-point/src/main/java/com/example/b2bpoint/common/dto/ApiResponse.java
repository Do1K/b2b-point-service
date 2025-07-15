package com.example.b2bpoint.common.dto;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data; // 성공 시 실제 데이터
    private final ErrorResponse error; // 실패 시 에러 정보

    private ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
        this.error = null;
    }

    private ApiResponse(boolean success, ErrorResponse error) {
        this.success = success;
        this.data = null;
        this.error = error;
    }


    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data);
    }


    public static ApiResponse<?> error(ErrorResponse error) {
        return new ApiResponse<>(false, error);
    }
}



