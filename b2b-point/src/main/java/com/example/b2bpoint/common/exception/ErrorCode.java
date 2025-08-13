package com.example.b2bpoint.common.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "유효하지 않은 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버에 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "유효하지 않은 타입 값입니다."),

    // Auth
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "A001", "접근 권한이 없습니다."),
    INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 API Key 입니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "A003", "해당 리소스에 대한 접근 권한이 없습니다."),


    // Partner
    PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "해당 파트너사를 찾을 수 없습니다."),
    PARTNER_ALREADY_EXISTS(HttpStatus.CONFLICT, "P002", "이미 존재하는 파트너사입니다."),
    PARTNER_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "P003", "활성 상태의 파트너사가 아닙니다."),

    // Point
    POINT_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "PT001", "포인트 지갑을 찾을 수 없습니다."),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "PT002", "포인트가 부족합니다."),

    // Coupon
    COUPON_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "CP001", "쿠폰 템플릿을 찾을 수 없습니다."),
    COUPON_ISSUE_QUANTITY_EXCEEDED(HttpStatus.BAD_REQUEST, "CP002", "쿠폰 발급 가능 수량을 초과했습니다."),
    COUPON_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "CP003", "사용할 수 없는 쿠폰입니다."),
    COUPON_NOT_IN_ISSUE_PERIOD(HttpStatus.BAD_REQUEST, "CP004", "쿠폰 발급 기간이 아닙니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "CP005", "이미 발급받은 쿠폰입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
