CREATE DATABASE IF NOT EXISTS pay_gateway_hub_sandbox
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE pay_gateway_hub_sandbox;

-- =====================================================
-- 沙箱测试商户数据
-- =====================================================

CREATE TABLE IF NOT EXISTS merchant_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_no VARCHAR(32) NOT NULL UNIQUE,
    merchant_name VARCHAR(128) NOT NULL,
    business_license_no VARCHAR(64),
    legal_person_name VARCHAR(64),
    legal_person_id_no VARCHAR(64),
    contact_phone VARCHAR(32),
    contact_email VARCHAR(128),
    settlement_bank_name VARCHAR(128),
    settlement_bank_account VARCHAR(64),
    settlement_account_name VARCHAR(64),
    audit_status INT DEFAULT 1,
    audit_remark VARCHAR(256),
    status INT DEFAULT 1,
    industry_code VARCHAR(32),
    industry_name VARCHAR(64),
    audit_step INT DEFAULT 3,
    risk_level VARCHAR(16) DEFAULT 'LOW',
    risk_score INT DEFAULT 0,
    business_verify_passed INT DEFAULT 1,
    business_verify_result VARCHAR(256),
    business_verify_time DATETIME,
    auto_audit_passed INT DEFAULT 1,
    auto_audit_remark VARCHAR(256),
    auto_audit_time DATETIME,
    manual_audit_user VARCHAR(64),
    manual_audit_time DATETIME,
    api_key_md5 VARCHAR(512),
    api_key_rsa_public TEXT,
    api_key_rsa_private TEXT,
    api_key_sm2_public TEXT,
    api_key_sm2_private TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO merchant_info (merchant_no, merchant_name, audit_status, status, audit_step, risk_level,
                           api_key_md5, api_key_rsa_public, api_key_rsa_private)
VALUES
('M000001', '沙箱测试商户1', 1, 1, 3, 'LOW',
 '7A5F3E8B2C1D4A6E9F0B3C5D7E9A1F4C', NULL, NULL),
('M000002', '沙箱测试商户2', 1, 1, 3, 'LOW',
 '1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6E', NULL, NULL);

CREATE TABLE IF NOT EXISTS merchant_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    nickname VARCHAR(64),
    avatar VARCHAR(512),
    merchant_no VARCHAR(32) NOT NULL,
    role VARCHAR(32) DEFAULT 'admin',
    status INT DEFAULT 1,
    last_login_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO merchant_user (username, password, nickname, merchant_no, role, status)
VALUES
('sandbox_merchant1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '沙箱商户1管理员', 'M000001', 'admin', 1),
('sandbox_merchant2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '沙箱商户2管理员', 'M000002', 'admin', 1);

-- =====================================================
-- 沙箱支付通道配置
-- =====================================================

CREATE TABLE IF NOT EXISTS pay_channel_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_code VARCHAR(32) NOT NULL UNIQUE,
    channel_name VARCHAR(64) NOT NULL,
    channel_merchant_id VARCHAR(64),
    channel_app_id VARCHAR(64),
    channel_secret_key VARCHAR(256),
    channel_public_key TEXT,
    channel_private_key TEXT,
    notify_url VARCHAR(256),
    sandbox_mode INT DEFAULT 1,
    gateway_url VARCHAR(256),
    status INT DEFAULT 1,
    remark VARCHAR(256),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO pay_channel_config (channel_code, channel_name, channel_merchant_id, channel_app_id,
                                channel_secret_key, sandbox_mode, gateway_url, status)
VALUES
('ALIPAY_SANDBOX', '支付宝沙箱通道', 'sandbox_alipay_mch', 'sandbox_alipay_app',
 'sandbox_alipay_secret', 1, 'https://openapi.alipaydev.com/gateway.do', 1),
('WECHAT_SANDBOX', '微信支付沙箱通道', 'sandbox_wechat_mch', 'sandbox_wechat_app',
 'sandbox_wechat_secret', 1, 'https://api.mch.weixin.qq.com/sandbox', 1),
('UNIONPAY_SANDBOX', '银联支付沙箱通道', 'sandbox_unionpay_mch', 'sandbox_unionpay_app',
 'sandbox_unionpay_secret', 1, 'https://gateway.test.95516.com', 1);

-- =====================================================
-- 沙箱商户支付配置
-- =====================================================

CREATE TABLE IF NOT EXISTS merchant_pay_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_no VARCHAR(32) NOT NULL,
    pay_channel VARCHAR(32) NOT NULL,
    pay_type VARCHAR(32) NOT NULL,
    channel_code VARCHAR(32) NOT NULL,
    fee_rate DECIMAL(10,4) DEFAULT 0.6000,
    min_fee DECIMAL(10,2) DEFAULT 0.01,
    max_fee DECIMAL(10,2) DEFAULT 99.99,
    status INT DEFAULT 1,
    priority INT DEFAULT 1,
    whitelist_ips VARCHAR(512),
    remark VARCHAR(256),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO merchant_pay_config (merchant_no, pay_channel, pay_type, channel_code, fee_rate, status, priority)
VALUES
('M000001', 'ALIPAY', 'NATIVE', 'ALIPAY_SANDBOX', 0.60, 1, 1),
('M000001', 'ALIPAY', 'H5', 'ALIPAY_SANDBOX', 0.60, 1, 2),
('M000001', 'ALIPAY', 'JSAPI', 'ALIPAY_SANDBOX', 0.60, 1, 3),
('M000001', 'ALIPAY', 'APP', 'ALIPAY_SANDBOX', 0.60, 1, 4),
('M000001', 'WECHAT_PAY', 'NATIVE', 'WECHAT_SANDBOX', 0.60, 1, 5),
('M000001', 'WECHAT_PAY', 'H5', 'WECHAT_SANDBOX', 0.60, 1, 6),
('M000001', 'WECHAT_PAY', 'JSAPI', 'WECHAT_SANDBOX', 0.60, 1, 7),
('M000001', 'WECHAT_PAY', 'APP', 'WECHAT_SANDBOX', 0.60, 1, 8),
('M000001', 'UNION_PAY', 'NATIVE', 'UNIONPAY_SANDBOX', 0.60, 1, 9),
('M000001', 'UNION_PAY', 'H5', 'UNIONPAY_SANDBOX', 0.60, 1, 10),
('M000002', 'ALIPAY', 'NATIVE', 'ALIPAY_SANDBOX', 0.60, 1, 1),
('M000002', 'WECHAT_PAY', 'NATIVE', 'WECHAT_SANDBOX', 0.60, 1, 2),
('M000002', 'UNION_PAY', 'NATIVE', 'UNIONPAY_SANDBOX', 0.60, 1, 3);

-- =====================================================
-- 沙箱业务表
-- =====================================================

CREATE TABLE IF NOT EXISTS pay_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    merchant_no VARCHAR(32) NOT NULL,
    merchant_order_no VARCHAR(64),
    link_code VARCHAR(32) DEFAULT NULL COMMENT '来源支付链接',
    coupon_code VARCHAR(32) DEFAULT NULL COMMENT '使用的优惠券编码',
    activity_code VARCHAR(32) DEFAULT NULL COMMENT '参与的活动编码',
    coupon_discount DECIMAL(12,2) DEFAULT 0 COMMENT '优惠券优惠金额',
    activity_discount DECIMAL(12,2) DEFAULT 0 COMMENT '活动优惠金额',
    pay_amount DECIMAL(12,2) NOT NULL,
    actual_amount DECIMAL(12,2),
    fee_amount DECIMAL(12,2),
    pay_channel VARCHAR(32),
    pay_type VARCHAR(32),
    user_identity VARCHAR(128),
    product_subject VARCHAR(256),
    product_detail VARCHAR(512),
    notify_url VARCHAR(256),
    client_ip VARCHAR(64),
    extra_params TEXT,
    pay_status INT DEFAULT 0,
    channel_trade_no VARCHAR(64),
    pay_time DATETIME,
    expire_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_order_no (order_no),
    INDEX idx_pay_status (pay_status),
    INDEX idx_created_at (created_at),
    INDEX idx_link_code (link_code),
    INDEX idx_coupon_code (coupon_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pay_refund (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_no VARCHAR(64) NOT NULL UNIQUE,
    order_no VARCHAR(64) NOT NULL,
    merchant_no VARCHAR(32) NOT NULL,
    merchant_refund_no VARCHAR(64),
    pay_amount DECIMAL(12,2),
    refund_amount DECIMAL(12,2) NOT NULL,
    refund_reason VARCHAR(256),
    refund_status INT DEFAULT 0,
    channel_refund_no VARCHAR(64),
    retry_count INT DEFAULT 0,
    next_retry_time DATETIME,
    refund_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_order_no (order_no),
    INDEX idx_refund_status (refund_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pay_channel_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_no VARCHAR(32),
    order_no VARCHAR(64),
    channel_code VARCHAR(32),
    request_type VARCHAR(32),
    request_url VARCHAR(256),
    request_data TEXT,
    response_data TEXT,
    channel_trade_no VARCHAR(64),
    cost_time INT,
    error_msg VARCHAR(512),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_no (order_no),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sandbox_test_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_no VARCHAR(32) NOT NULL,
    test_scene VARCHAR(32) NOT NULL,
    test_name VARCHAR(128),
    pay_channel VARCHAR(32),
    pay_type VARCHAR(32),
    pay_amount DECIMAL(12,2),
    test_params TEXT,
    expect_result INT,
    actual_result INT,
    response_data TEXT,
    notify_result VARCHAR(256),
    error_msg VARCHAR(512),
    start_time DATETIME,
    end_time DATETIME,
    cost_time BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_test_scene (test_scene),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS merchant_config_test_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_no VARCHAR(32) NOT NULL,
    test_type VARCHAR(32),
    test_result VARCHAR(16),
    test_detail TEXT,
    test_duration_ms BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_merchant_no (merchant_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS callback_simulate_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_no VARCHAR(32) NOT NULL,
    order_no VARCHAR(64),
    notify_type VARCHAR(32),
    notify_url VARCHAR(256),
    request_data TEXT,
    response_data TEXT,
    response_code INT,
    success TINYINT(1) DEFAULT 0,
    retry_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_merchant_no (merchant_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS risk_control_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_no VARCHAR(32),
    order_no VARCHAR(64),
    risk_level VARCHAR(16),
    risk_score INT,
    risk_desc VARCHAR(512),
    action_type VARCHAR(32),
    rule_code VARCHAR(32),
    rule_name VARCHAR(64),
    request_data TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS api_access_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_no VARCHAR(32),
    api_path VARCHAR(256),
    http_method VARCHAR(16),
    request_params TEXT,
    response_code INT,
    response_msg VARCHAR(512),
    client_ip VARCHAR(64),
    user_agent VARCHAR(512),
    cost_time INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fee_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_no VARCHAR(64) NOT NULL UNIQUE,
    rule_name VARCHAR(128) NOT NULL,
    merchant_no VARCHAR(32),
    pay_channel VARCHAR(32),
    fee_rate DECIMAL(10,4) NOT NULL,
    min_fee DECIMAL(10,2) DEFAULT 0.01,
    max_fee DECIMAL(10,2) DEFAULT 99.99,
    status INT DEFAULT 1,
    priority INT DEFAULT 1,
    effective_time DATETIME,
    expire_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO fee_rule (rule_no, rule_name, merchant_no, pay_channel, fee_rate, status, priority)
VALUES
('FEE_SANDBOX_001', '沙箱测试费率规则-支付宝', 'M000001', 'ALIPAY', 0.60, 1, 1),
('FEE_SANDBOX_002', '沙箱测试费率规则-微信', 'M000001', 'WECHAT_PAY', 0.60, 1, 2),
('FEE_SANDBOX_003', '沙箱测试费率规则-银联', 'M000001', 'UNION_PAY', 0.60, 1, 3);

CREATE TABLE IF NOT EXISTS risk_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL UNIQUE,
    rule_name VARCHAR(128) NOT NULL,
    rule_type VARCHAR(32),
    risk_level VARCHAR(16),
    action_type VARCHAR(32),
    threshold_value DECIMAL(10,2),
    time_window_minutes INT,
    enabled TINYINT(1) DEFAULT 1,
    description VARCHAR(512),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS risk_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    list_type VARCHAR(16) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_value VARCHAR(256) NOT NULL,
    reason VARCHAR(512),
    expire_time DATETIME,
    created_by VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_list_type (list_type),
    INDEX idx_target (target_type, target_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS risk_whitelist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_no VARCHAR(32) DEFAULT NULL COMMENT '商户号，为空表示全局白名单',
    list_type VARCHAR(32) NOT NULL COMMENT '名单类型：IP-IP地址, USER-用户, MERCHANT-商户, DEVICE-设备',
    list_value VARCHAR(256) NOT NULL COMMENT '名单值',
    list_source VARCHAR(64) DEFAULT NULL COMMENT '名单来源',
    bypass_rules VARCHAR(512) DEFAULT NULL COMMENT '免检规则，*表示全部免检，多个规则用逗号分隔',
    reason VARCHAR(512) DEFAULT NULL COMMENT '添加原因',
    operator_id VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
    status INT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_list_type (list_type),
    INDEX idx_list_value (list_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控白名单表';

-- =====================================================
-- 沙箱发票表
-- =====================================================

CREATE TABLE IF NOT EXISTS pay_invoice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(64) NOT NULL UNIQUE,
    merchant_no VARCHAR(32) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    channel_invoice_no VARCHAR(128),
    channel_code VARCHAR(32) NOT NULL,
    invoice_type INT DEFAULT 1,
    invoice_status INT DEFAULT 0,
    title_type INT DEFAULT 1,
    buyer_title VARCHAR(256) NOT NULL,
    buyer_tax_no VARCHAR(64),
    buyer_address VARCHAR(256),
    buyer_bank_name VARCHAR(128),
    buyer_bank_account VARCHAR(64),
    buyer_phone VARCHAR(20),
    buyer_email VARCHAR(128),
    invoice_content VARCHAR(256),
    invoice_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    tax_rate VARCHAR(16) DEFAULT '6%',
    pdf_url VARCHAR(512),
    original_invoice_no VARCHAR(64),
    red_reason VARCHAR(512),
    remark VARCHAR(512),
    fail_reason VARCHAR(512),
    notify_url VARCHAR(512),
    issue_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_order_no (order_no),
    INDEX idx_invoice_status (invoice_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pay_invoice_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    invoice_no VARCHAR(64) NOT NULL,
    item_name VARCHAR(256) NOT NULL,
    item_code VARCHAR(64),
    specification VARCHAR(128),
    unit VARCHAR(32),
    quantity DECIMAL(18,4),
    unit_price DECIMAL(18,4),
    amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    tax_rate VARCHAR(16) DEFAULT '6%',
    tax_included_flag INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_invoice_id (invoice_id),
    INDEX idx_invoice_no (invoice_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pay_invoice_callback_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(64),
    channel_code VARCHAR(32),
    channel_invoice_no VARCHAR(128),
    request_body TEXT,
    response_body TEXT,
    notify_status VARCHAR(32) DEFAULT 'RECEIVED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_invoice_no (invoice_no),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pay_invoice_channel_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_no VARCHAR(32) NOT NULL,
    channel_code VARCHAR(32) NOT NULL,
    app_id VARCHAR(128),
    app_secret VARCHAR(512),
    access_token VARCHAR(512),
    tax_num VARCHAR(64),
    company_name VARCHAR(256),
    company_address VARCHAR(256),
    company_phone VARCHAR(20),
    bank_name VARCHAR(128),
    bank_account VARCHAR(64),
    enabled INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_merchant_channel (merchant_no, channel_code),
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO pay_invoice_channel_config (merchant_no, channel_code, app_id, app_secret, access_token,
                                       tax_num, company_name, enabled, created_at, updated_at)
VALUES
('M000001', 'NUONUO', 'sandbox_nuonuo_appkey', 'sandbox_nuonuo_secret', 'sandbox_nuonuo_token',
 '91330100MA12345678', '沙箱测试商户1诺诺配置', 1, NOW(), NOW()),
('M000001', 'BAIWANG', 'sandbox_baiwang_appid', 'sandbox_baiwang_secret', NULL,
 '91330100MA12345678', '沙箱测试商户1百望配置', 1, NOW(), NOW()),
('M000002', 'NUONUO', 'sandbox_nuonuo_appkey2', 'sandbox_nuonuo_secret2', 'sandbox_nuonuo_token2',
 '91330100MA87654321', '沙箱测试商户2诺诺配置', 1, NOW(), NOW());

-- ==============================================
-- 报表订阅配置表
-- ==============================================
DROP TABLE IF EXISTS `report_subscription`;
CREATE TABLE `report_subscription` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `subscription_no` VARCHAR(32)   NOT NULL COMMENT '订阅编号',
    `merchant_no`     VARCHAR(32)   NOT NULL COMMENT '商户号',
    `report_type`     TINYINT       NOT NULL COMMENT '报表类型: 1=日报, 2=周报',
    `report_category` VARCHAR(32)   NOT NULL DEFAULT 'SETTLEMENT' COMMENT '报表类别: SETTLEMENT=结算报表, ORDER=订单报表, FEE=手续费报表',
    `push_channel`    TINYINT       NOT NULL DEFAULT 1 COMMENT '推送渠道: 1=邮件, 2=邮件+短信',
    `email_list`      VARCHAR(500)  NOT NULL COMMENT '接收邮箱列表，多个逗号分隔',
    `phone_list`      VARCHAR(200)  DEFAULT NULL COMMENT '接收手机号列表，多个逗号分隔',
    `push_time`       VARCHAR(20)   NOT NULL DEFAULT '09:00' COMMENT '推送时间(HH:mm)',
    `enabled`         TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用: 0=禁用, 1=启用',
    `remark`          VARCHAR(200)  DEFAULT NULL COMMENT '备注',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subscription_no` (`subscription_no`),
    KEY `idx_merchant_no` (`merchant_no`),
    KEY `idx_report_type` (`report_type`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表订阅配置表';

-- 初始化报表订阅数据
INSERT INTO `report_subscription`
    (subscription_no, merchant_no, report_type, report_category, push_channel, email_list, phone_list, push_time, enabled, remark, created_at, updated_at)
VALUES
('SUB202501010001', 'M000001', 1, 'SETTLEMENT', 1, 'finance@example.com,ceo@example.com', NULL, '09:00', 1, '沙箱商户1-结算日报订阅', NOW(), NOW()),
('SUB202501010002', 'M000001', 2, 'SETTLEMENT', 1, 'finance@example.com', NULL, '09:30', 1, '沙箱商户1-结算周报订阅', NOW(), NOW()),
('SUB202501010003', 'M000002', 1, 'SETTLEMENT', 1, 'ops@merchant2.com', NULL, '08:30', 1, '沙箱商户2-结算日报订阅', NOW(), NOW());

-- ==============================================
-- 报表推送记录表
-- ==============================================
DROP TABLE IF EXISTS `report_push_record`;
CREATE TABLE `report_push_record` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `record_no`       VARCHAR(32)   NOT NULL COMMENT '推送记录编号',
    `subscription_no` VARCHAR(32)   DEFAULT NULL COMMENT '订阅编号',
    `merchant_no`     VARCHAR(32)   NOT NULL COMMENT '商户号',
    `report_type`     TINYINT       NOT NULL COMMENT '报表类型: 1=日报, 2=周报',
    `report_category` VARCHAR(32)   NOT NULL DEFAULT 'SETTLEMENT' COMMENT '报表类别',
    `report_title`    VARCHAR(200)  NOT NULL COMMENT '报表标题',
    `report_period`   VARCHAR(50)   NOT NULL COMMENT '报表周期(日期范围)',
    `start_date`      DATE          NOT NULL COMMENT '统计开始日期',
    `end_date`        DATE          NOT NULL COMMENT '统计结束日期',
    `push_status`     TINYINT       NOT NULL DEFAULT 0 COMMENT '推送状态: 0=待推送, 1=推送中, 2=推送成功, 3=推送失败',
    `push_channel`    TINYINT       NOT NULL DEFAULT 1 COMMENT '推送渠道: 1=邮件, 2=邮件+短信',
    `email_targets`   VARCHAR(500)  DEFAULT NULL COMMENT '实际推送邮箱',
    `phone_targets`   VARCHAR(200)  DEFAULT NULL COMMENT '实际推送手机号',
    `file_url`        VARCHAR(500)  DEFAULT NULL COMMENT '报表文件URL',
    `file_size`       BIGINT        DEFAULT NULL COMMENT '文件大小(字节)',
    `success_count`   INT           NOT NULL DEFAULT 0 COMMENT '成功数量',
    `fail_count`      INT           NOT NULL DEFAULT 0 COMMENT '失败数量',
    `fail_reason`     VARCHAR(500)  DEFAULT NULL COMMENT '失败原因',
    `trigger_type`    TINYINT       NOT NULL DEFAULT 1 COMMENT '触发方式: 1=定时任务, 2=手动触发',
    `push_time`       DATETIME      DEFAULT NULL COMMENT '推送时间',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_no` (`record_no`),
    KEY `idx_subscription_no` (`subscription_no`),
    KEY `idx_merchant_no` (`merchant_no`),
    KEY `idx_push_status` (`push_status`),
    KEY `idx_start_end_date` (`start_date`, `end_date`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表推送记录表';

-- =====================================================
-- 营销模块表
-- =====================================================

CREATE TABLE IF NOT EXISTS pay_link (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    link_code       VARCHAR(32)   NOT NULL COMMENT '链接短码',
    merchant_no     VARCHAR(32)   NOT NULL COMMENT '商户号',
    title           VARCHAR(100)  NOT NULL COMMENT '链接标题',
    fixed_amount    DECIMAL(12,2) DEFAULT NULL COMMENT '固定金额',
    amount_editable TINYINT(1)    DEFAULT 0 COMMENT '是否允许自定义金额',
    min_amount      DECIMAL(12,2) DEFAULT NULL COMMENT '最低金额',
    max_amount      DECIMAL(12,2) DEFAULT NULL COMMENT '最高金额',
    pay_channel     VARCHAR(32)   DEFAULT NULL COMMENT '支付渠道',
    product_subject VARCHAR(200)  DEFAULT NULL COMMENT '商品描述',
    product_detail  VARCHAR(500)  DEFAULT NULL COMMENT '商品详情',
    notify_url      VARCHAR(500)  DEFAULT NULL COMMENT '异步通知地址',
    redirect_url    VARCHAR(500)  DEFAULT NULL COMMENT '同步跳转地址',
    expire_time     DATETIME      DEFAULT NULL COMMENT '过期时间',
    single_use      TINYINT(1)    DEFAULT 0 COMMENT '是否单次使用',
    max_use_count   INT           DEFAULT NULL COMMENT '最大使用次数',
    used_count      INT           DEFAULT 0 COMMENT '已使用次数',
    status          INT           DEFAULT 1 COMMENT '状态：1生效 2过期 3禁用 4用尽',
    remark          VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    operator_id     VARCHAR(32)   DEFAULT NULL COMMENT '操作人ID',
    operator_name   VARCHAR(64)   DEFAULT NULL COMMENT '操作人姓名',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT           DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_link_code (link_code),
    KEY idx_merchant_no (merchant_no),
    KEY idx_status (status),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付链接表';

CREATE TABLE IF NOT EXISTS coupon (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    coupon_code       VARCHAR(32)   NOT NULL COMMENT '优惠券编码',
    merchant_no       VARCHAR(32)   NOT NULL COMMENT '商户号',
    coupon_name       VARCHAR(100)  NOT NULL COMMENT '优惠券名称',
    coupon_type       INT           NOT NULL COMMENT '类型：1固定抵扣 2折扣抵扣',
    discount_value    DECIMAL(12,2) NOT NULL COMMENT '优惠值：固定金额或折扣率',
    min_order_amount  DECIMAL(12,2) DEFAULT NULL COMMENT '最低订单金额',
    max_discount      DECIMAL(12,2) DEFAULT NULL COMMENT '最大优惠金额(折扣券)',
    total_quantity    INT           NOT NULL COMMENT '发放总量',
    issued_count      INT           DEFAULT 0 COMMENT '已发放数量',
    used_count        INT           DEFAULT 0 COMMENT '已核销数量',
    start_time        DATETIME      DEFAULT NULL COMMENT '生效开始时间',
    end_time          DATETIME      DEFAULT NULL COMMENT '生效结束时间',
    status            INT           DEFAULT 0 COMMENT '状态：0未开始 1发放中 2暂停 3发完 4过期',
    remark            VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    operator_id       VARCHAR(32)   DEFAULT NULL COMMENT '操作人ID',
    operator_name     VARCHAR(64)   DEFAULT NULL COMMENT '操作人姓名',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           INT           DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_code (coupon_code),
    KEY idx_merchant_no (merchant_no),
    KEY idx_coupon_type (coupon_type),
    KEY idx_status (status),
    KEY idx_time_range (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

CREATE TABLE IF NOT EXISTS activity (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    activity_code     VARCHAR(32)   NOT NULL COMMENT '活动编码',
    merchant_no       VARCHAR(32)   NOT NULL COMMENT '商户号',
    activity_name     VARCHAR(100)  NOT NULL COMMENT '活动名称',
    activity_type     INT           NOT NULL COMMENT '类型：1满减 2折扣',
    threshold_amount  DECIMAL(12,2) DEFAULT NULL COMMENT '门槛金额',
    discount_amount   DECIMAL(12,2) DEFAULT NULL COMMENT '减免金额(满减)',
    discount_rate     DECIMAL(5,2)  DEFAULT NULL COMMENT '折扣率(折扣活动)',
    max_discount      DECIMAL(12,2) DEFAULT NULL COMMENT '最大优惠(折扣活动)',
    start_time        DATETIME      DEFAULT NULL COMMENT '活动开始时间',
    end_time          DATETIME      DEFAULT NULL COMMENT '活动结束时间',
    status            INT           DEFAULT 0 COMMENT '状态：0未开始 1进行中 2暂停 3结束',
    remark            VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    operator_id       VARCHAR(32)   DEFAULT NULL COMMENT '操作人ID',
    operator_name     VARCHAR(64)   DEFAULT NULL COMMENT '操作人姓名',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           INT           DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_code (activity_code),
    KEY idx_merchant_no (merchant_no),
    KEY idx_activity_type (activity_type),
    KEY idx_status (status),
    KEY idx_time_range (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

CREATE TABLE IF NOT EXISTS coupon_use_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    coupon_code     VARCHAR(32)   NOT NULL COMMENT '优惠券编码',
    merchant_no     VARCHAR(32)   NOT NULL COMMENT '商户号',
    order_no        VARCHAR(64)   DEFAULT NULL COMMENT '关联订单号',
    user_id         VARCHAR(64)   DEFAULT NULL COMMENT '用户标识',
    order_amount    DECIMAL(12,2) DEFAULT NULL COMMENT '订单金额',
    discount_amount DECIMAL(12,2) DEFAULT NULL COMMENT '优惠金额',
    use_type        INT           DEFAULT NULL COMMENT '使用类型：1核销 2锁定 3释放',
    used_at         DATETIME      DEFAULT NULL COMMENT '使用时间',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_coupon_code (coupon_code),
    KEY idx_merchant_no (merchant_no),
    KEY idx_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券使用记录';

-- =====================================================
-- 营销模块初始化数据
-- =====================================================

INSERT INTO pay_link (link_code, merchant_no, title, fixed_amount, amount_editable, single_use, status, remark, created_at, updated_at)
VALUES
('LK000001', 'M000001', '会员年费支付', 299.00, 0, 0, 1, '沙箱示例-固定金额链接', NOW(), NOW()),
('LK000002', 'M000001', '自助捐赠通道', NULL, 1, 0, 1, '沙箱示例-自定义金额链接', NOW(), NOW()),
('LK000003', 'M000001', '一次性缴费链接', 99.00, 0, 1, 1, '沙箱示例-单次使用链接', NOW(), NOW());

INSERT INTO coupon (coupon_code, merchant_no, coupon_name, coupon_type, discount_value, min_order_amount, max_discount, total_quantity, issued_count, used_count, start_time, end_time, status, remark, created_at, updated_at)
VALUES
('CP000001', 'M000001', '新人立减10元', 1, 10.00, 50.00, NULL, 1000, 100, 23, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '沙箱示例-固定抵扣券', NOW(), NOW()),
('CP000002', 'M000001', '85折优惠券', 2, 8.50, 100.00, 50.00, 500, 50, 12, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '沙箱示例-折扣券', NOW(), NOW()),
('CP000003', 'M000001', '满200减30', 1, 30.00, 200.00, NULL, 200, 0, 0, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), 0, '沙箱示例-未开始', NOW(), NOW());

INSERT INTO activity (activity_code, merchant_no, activity_name, activity_type, threshold_amount, discount_amount, discount_rate, max_discount, start_time, end_time, status, remark, created_at, updated_at)
VALUES
('ACT000001', 'M000001', '春季满减活动', 1, 100.00, 10.00, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '沙箱示例-满100减10', NOW(), NOW()),
('ACT000002', 'M000001', '夏季折扣季', 2, 200.00, NULL, 8.00, 100.00, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), 1, '沙箱示例-8折封顶100', NOW(), NOW());

-- =====================================================
-- 分账接收方实名认证表
-- =====================================================

CREATE TABLE IF NOT EXISTS split_receiver (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    receiver_no       VARCHAR(32)   NOT NULL COMMENT '接收方编号',
    merchant_no       VARCHAR(32)   NOT NULL COMMENT '所属商户号',
    receiver_name     VARCHAR(100)  NOT NULL COMMENT '接收方姓名/企业名',
    receiver_type     INT           NOT NULL COMMENT '接收方类型：1个人 2企业',
    id_card_no        VARCHAR(64)   NOT NULL COMMENT '身份证号/统一社会信用代码',
    id_card_name      VARCHAR(100)  NOT NULL COMMENT '证件姓名/企业法人姓名',
    bank_card_no      VARCHAR(64)   NOT NULL COMMENT '银行卡号',
    bank_phone        VARCHAR(32)   NOT NULL COMMENT '银行预留手机号',
    bank_name         VARCHAR(100)  NOT NULL COMMENT '开户银行',
    bank_branch_name  VARCHAR(200)  DEFAULT NULL COMMENT '开户支行',
    verify_status     INT           DEFAULT 0 COMMENT '认证状态：0未认证 1认证中 2已认证 3认证失败',
    verify_channel    INT           DEFAULT NULL COMMENT '认证渠道：1银行卡四要素 2银行卡三要素 3人脸识别',
    verify_time       DATETIME      DEFAULT NULL COMMENT '最近认证时间',
    verify_fail_code  VARCHAR(64)   DEFAULT NULL COMMENT '认证失败错误码',
    verify_fail_reason VARCHAR(500) DEFAULT NULL COMMENT '认证失败原因',
    verify_request_id VARCHAR(64)   DEFAULT NULL COMMENT '认证请求流水号',
    contact_name      VARCHAR(64)   DEFAULT NULL COMMENT '联系人姓名',
    contact_phone     VARCHAR(32)   DEFAULT NULL COMMENT '联系人电话',
    contact_email     VARCHAR(128)  DEFAULT NULL COMMENT '联系人邮箱',
    status            INT           DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark            VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    operator_id       VARCHAR(32)   DEFAULT NULL COMMENT '操作人ID',
    operator_name     VARCHAR(64)   DEFAULT NULL COMMENT '操作人姓名',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           INT           DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_receiver_no (receiver_no),
    KEY idx_merchant_no (merchant_no),
    KEY idx_verify_status (verify_status),
    KEY idx_status (status),
    KEY idx_id_card_no (id_card_no),
    KEY idx_bank_card_no (bank_card_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账接收方表';

CREATE TABLE IF NOT EXISTS split_receiver_verify_log (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    log_no            VARCHAR(32)   NOT NULL COMMENT '日志编号',
    merchant_no       VARCHAR(32)   NOT NULL COMMENT '商户号',
    receiver_no       VARCHAR(32)   NOT NULL COMMENT '接收方编号',
    verify_channel    INT           DEFAULT NULL COMMENT '认证渠道：1银行卡四要素 2银行卡三要素 3人脸识别',
    verify_request_id VARCHAR(64)   DEFAULT NULL COMMENT '认证请求流水号',
    id_card_name      VARCHAR(100)  DEFAULT NULL COMMENT '证件姓名',
    id_card_no        VARCHAR(64)   DEFAULT NULL COMMENT '证件号码',
    bank_card_no      VARCHAR(64)   DEFAULT NULL COMMENT '银行卡号',
    bank_phone        VARCHAR(32)   DEFAULT NULL COMMENT '预留手机号',
    verify_status     INT           DEFAULT NULL COMMENT '认证结果：1成功 2失败 3处理中',
    verify_result     VARCHAR(500)  DEFAULT NULL COMMENT '认证结果描述',
    verify_fail_code  VARCHAR(64)   DEFAULT NULL COMMENT '认证失败错误码',
    verify_fail_reason VARCHAR(500) DEFAULT NULL COMMENT '认证失败原因',
    verify_time       DATETIME      DEFAULT NULL COMMENT '认证时间',
    request_data      TEXT          DEFAULT NULL COMMENT '请求原始数据(JSON)',
    response_data     TEXT          DEFAULT NULL COMMENT '响应原始数据(JSON)',
    operator_id       VARCHAR(32)   DEFAULT NULL COMMENT '操作人ID',
    operator_name     VARCHAR(64)   DEFAULT NULL COMMENT '操作人姓名',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_no (log_no),
    KEY idx_merchant_no (merchant_no),
    KEY idx_receiver_no (receiver_no),
    KEY idx_verify_request_id (verify_request_id),
    KEY idx_verify_status (verify_status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账接收方实名认证记录表';

-- =====================================================
-- 分账接收方初始化数据（沙箱）
-- =====================================================

INSERT INTO split_receiver (receiver_no, merchant_no, receiver_name, receiver_type, id_card_no, id_card_name,
                             bank_card_no, bank_phone, bank_name, bank_branch_name,
                             verify_status, verify_channel, verify_time, verify_request_id,
                             contact_name, contact_phone, contact_email, status, remark,
                             operator_id, operator_name, created_at, updated_at)
VALUES
('RE000001', 'M000001', '张三供应商', 1, '110101199001011234', '张三',
 '6222021234567890000', '13800138000', '中国工商银行', '北京朝阳支行',
 2, 1, NOW(), 'VR202501010001',
 '张三', '13800138000', 'zhangsan@example.com', 1, '沙箱示例-已认证个人接收方',
 'admin', '系统管理员', NOW(), NOW()),
('RE000002', 'M000001', '李四服务商', 1, '310101199203035678', '李四',
 '6227009876543219999', '13900139000', '中国建设银行', '上海浦东支行',
 3, 1, NOW(), 'VR202501010002',
 '李四', '13900139000', 'lisi@example.com', 1, '沙箱示例-认证失败接收方（测试）',
 'admin', '系统管理员', NOW(), NOW()),
('RE000003', 'M000001', '王五工作室', 1, '440101199505059012', '王五',
 '6228481122334455678', '13700137000', '中国农业银行', '广州天河支行',
 0, NULL, NULL, NULL,
 '王五', '13700137000', 'wangwu@example.com', 1, '沙箱示例-未认证接收方',
 'admin', '系统管理员', NOW(), NOW());

-- =====================================================
-- 对账自动平账规则配置表
-- =====================================================

CREATE TABLE IF NOT EXISTS reconcile_auto_writeoff_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    merchant_no VARCHAR(32) DEFAULT NULL COMMENT '商户号，为空表示全局规则',
    pay_channel VARCHAR(32) DEFAULT NULL COMMENT '支付渠道，为空表示所有渠道',
    diff_type INT NOT NULL COMMENT '适用的差异类型：1长款 2短款 3金额不一致',
    max_amount DECIMAL(12,2) NOT NULL DEFAULT 10.00 COMMENT '最大自动平账金额',
    auto_writeoff INT NOT NULL DEFAULT 1 COMMENT '是否自动平账：1是 0否',
    handle_type INT DEFAULT NULL COMMENT '自动平账处理方式：1补单 2退款 3调账 4忽略',
    enabled INT NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0禁用',
    priority INT DEFAULT 0 COMMENT '优先级，数值越大优先级越高',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    operator_id VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_pay_channel (pay_channel),
    INDEX idx_diff_type (diff_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账自动平账规则配置表';

INSERT INTO reconcile_auto_writeoff_rule (rule_name, merchant_no, pay_channel, diff_type, max_amount, auto_writeoff, handle_type, enabled, priority, remark, operator_id, operator_name)
VALUES
('小额长款自动平账', NULL, NULL, 1, 10.00, 1, 4, 1, 10, '全局规则：长款金额小于10元自动忽略平账', 'admin', '系统管理员'),
('小额短款自动补单', NULL, NULL, 2, 10.00, 1, 1, 1, 10, '全局规则：短款金额小于10元自动补单', 'admin', '系统管理员'),
('小额金额差异自动调账', NULL, NULL, 3, 10.00, 1, 3, 1, 5, '全局规则：金额差异小于10元自动调账', 'admin', '系统管理员');

-- =====================================================
-- 对账补账记录表
-- =====================================================

CREATE TABLE IF NOT EXISTS reconcile_writeoff_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    writeoff_no VARCHAR(64) NOT NULL UNIQUE COMMENT '补账编号',
    reconcile_no VARCHAR(64) NOT NULL COMMENT '对账单号',
    detail_id BIGINT NOT NULL COMMENT '差异明细ID',
    detail_no VARCHAR(64) DEFAULT NULL COMMENT '差异明细编号',
    merchant_no VARCHAR(32) DEFAULT NULL COMMENT '商户号',
    pay_channel VARCHAR(32) DEFAULT NULL COMMENT '支付渠道',
    diff_type INT DEFAULT NULL COMMENT '差异类型',
    diff_amount DECIMAL(12,2) DEFAULT NULL COMMENT '差异金额',
    writeoff_amount DECIMAL(12,2) DEFAULT NULL COMMENT '平账金额',
    writeoff_type INT NOT NULL COMMENT '平账类型：1补单 2退款 3调账 4忽略',
    writeoff_source INT NOT NULL DEFAULT 1 COMMENT '平账来源：1自动 2手动',
    rule_id BIGINT DEFAULT NULL COMMENT '匹配的自动平账规则ID',
    rule_name VARCHAR(128) DEFAULT NULL COMMENT '匹配的自动平账规则名称',
    writeoff_status INT NOT NULL DEFAULT 0 COMMENT '平账状态：0待执行 1执行中 2成功 3失败',
    error_order_no VARCHAR(64) DEFAULT NULL COMMENT '关联差错单号',
    order_no VARCHAR(64) DEFAULT NULL COMMENT '平台订单号',
    channel_trade_no VARCHAR(64) DEFAULT NULL COMMENT '渠道交易号',
    writeoff_remark VARCHAR(512) DEFAULT NULL COMMENT '平账备注',
    execute_time DATETIME DEFAULT NULL COMMENT '执行时间',
    execute_result VARCHAR(512) DEFAULT NULL COMMENT '执行结果',
    operator_id VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_reconcile_no (reconcile_no),
    INDEX idx_detail_id (detail_id),
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_writeoff_status (writeoff_status),
    INDEX idx_writeoff_source (writeoff_source),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账补账记录表';
