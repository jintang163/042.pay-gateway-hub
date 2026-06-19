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
    INDEX idx_created_at (created_at)
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
    target_type VARCHAR(32) NOT NULL,
    target_value VARCHAR(256) NOT NULL,
    reason VARCHAR(512),
    expire_time DATETIME,
    created_by VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
