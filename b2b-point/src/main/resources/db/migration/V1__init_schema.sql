-- V1__init_schema.sql

-- =================================================================
-- 1. partners (파트너사 정보)
-- =================================================================
CREATE TABLE partners
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100)                                              NOT NULL COMMENT '파트너사 이름',
    contact_email     VARCHAR(255)                                       NOT NULL COMMENT '담당자 이메일',
    business_number   VARCHAR(50)                                        NOT NULL COMMENT '사업자 등록 번호',
    api_key    VARCHAR(255)                                              NULL COMMENT '인증을 위한 고유 API Key',
    status     ENUM ('PENDING', 'ACTIVE', 'INACTIVE')                    NOT NULL DEFAULT 'PENDING' COMMENT '파트너사 상태 (승인대기, 활성, 비활성)',
    created_at DATETIME(6)                                               NOT NULL COMMENT '생성 일시',
    updated_at DATETIME(6)                                               NOT NULL COMMENT '마지막 수정 일시',
    CONSTRAINT uk_business_number UNIQUE (business_number),
    CONSTRAINT uk_api_key UNIQUE (api_key)
) COMMENT '파트너사 정보';


-- =================================================================
-- 2. point_wallets (사용자별 포인트 지갑)
-- =================================================================
CREATE TABLE point_wallets
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    partner_id BIGINT                                                    NOT NULL COMMENT '파트너사 ID (FK)',
    user_id    VARCHAR(255)                                              NOT NULL COMMENT '파트너사 시스템의 사용자 ID',
    points     INT                                                       NOT NULL DEFAULT 0 COMMENT '현재 포인트 잔액',
    version    BIGINT                                                    NOT NULL DEFAULT 0 COMMENT '낙관적 락을 위한 버전',
    created_at DATETIME(6)                                               NOT NULL COMMENT '생성 일시',
    updated_at DATETIME(6)                                               NOT NULL COMMENT '마지막 수정 일시',
    CONSTRAINT fk_point_wallets_to_partners FOREIGN KEY (partner_id) REFERENCES partners (id),
    CONSTRAINT uk_partner_user UNIQUE (partner_id, user_id)
) COMMENT '사용자별 포인트 지갑';


-- =================================================================
-- 3. point_histories (포인트 거래 내역)
-- =================================================================
CREATE TABLE point_histories
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    point_wallet_id    BIGINT                                            NOT NULL COMMENT '포인트 지갑 ID (FK)',
    transaction_type   ENUM ('EARN', 'USE')                              NOT NULL COMMENT '거래 종류 (적립, 사용)',
    amount             INT                                               NOT NULL COMMENT '거래 포인트 양 (항상 양수)',
    description        VARCHAR(255)                                      NULL COMMENT '거래 설명',
    partner_order_id   VARCHAR(255)                                      NULL COMMENT '파트너사의 주문 ID 등 외부 추적용 ID',
    created_at         DATETIME(6)                                       NOT NULL COMMENT '거래 발생 일시',
    CONSTRAINT fk_point_histories_to_point_wallets FOREIGN KEY (point_wallet_id) REFERENCES point_wallets (id)
) COMMENT '포인트 거래 내역';


-- =================================================================
-- 4. coupon_templates (쿠폰 템플릿/정책)
-- =================================================================
CREATE TABLE coupon_templates
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    partner_id            BIGINT                                                          NOT NULL COMMENT '파트너사 ID (FK)',
    name                  VARCHAR(255)                                                    NOT NULL COMMENT '쿠폰 이름',
    coupon_type           ENUM ('FIXED_AMOUNT', 'PERCENTAGE')                             NOT NULL COMMENT '쿠폰 종류 (정액, 정률)',
    discount_value        DECIMAL(19, 2)                                                  NOT NULL COMMENT '할인 값 (금액 또는 퍼센트)',
    max_discount_amount   INT                                                             NULL COMMENT '정률 할인 시 최대 할인 가능 금액',
    min_order_amount      INT                                                             NOT NULL DEFAULT 0 COMMENT '최소 주문 금액',
    total_quantity        INT                                                             NULL COMMENT '총 발급 가능 수량 (NULL이면 무제한)',
    issued_quantity       INT                                                             NOT NULL DEFAULT 0 COMMENT '현재까지 발급된 수량',
    valid_from            DATETIME(6)                                                     NOT NULL COMMENT '쿠폰 유효 시작일',
    valid_until           DATETIME(6)                                                     NOT NULL COMMENT '쿠폰 유효 만료일',
    created_at            DATETIME(6)                                                     NOT NULL COMMENT '생성 일시',
    updated_at            DATETIME(6)                                                     NOT NULL COMMENT '마지막 수정 일시',
    CONSTRAINT fk_coupon_templates_to_partners FOREIGN KEY (partner_id) REFERENCES partners (id)
) COMMENT '쿠폰 템플릿/정책';


-- =================================================================
-- 5. coupons (사용자에게 발급된 쿠폰)
-- =================================================================
CREATE TABLE coupons
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_template_id   BIGINT                                                          NOT NULL COMMENT '쿠폰 템플릿 ID (FK)',
    partner_id           BIGINT                                                          NOT NULL COMMENT '파트너사 ID (FK)',
    user_id              VARCHAR(255)                                                    NOT NULL COMMENT '쿠폰 소유 사용자 ID',
    status               ENUM ('AVAILABLE', 'USED', 'EXPIRED')                           NOT NULL DEFAULT 'AVAILABLE' COMMENT '쿠폰 상태 (사용 가능, 사용됨, 만료됨)',
    issued_at            DATETIME(6)                                                     NOT NULL COMMENT '발급 일시',
    used_at              DATETIME(6)                                                     NULL COMMENT '사용 일시',
    expired_at           DATETIME(6)                                                     NOT NULL COMMENT '만료 일시',
    CONSTRAINT fk_coupons_to_coupon_templates FOREIGN KEY (coupon_template_id) REFERENCES coupon_templates (id),
    CONSTRAINT fk_coupons_to_partners FOREIGN KEY (partner_id) REFERENCES partners (id)
) COMMENT '사용자에게 발급된 쿠폰';