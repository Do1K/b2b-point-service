ALTER TABLE coupons
    ADD COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시',
    ADD COLUMN updated_at DATETIME(6) NOT NULL COMMENT '마지막 수정 일시';