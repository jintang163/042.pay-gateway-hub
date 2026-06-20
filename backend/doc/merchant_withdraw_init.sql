-- =====================================================
-- 商户提现模块数据库初始化脚本
-- =====================================================

-- 商户提现表
CREATE TABLE IF NOT EXISTS merchant_withdraw (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    withdraw_no VARCHAR(32) NOT NULL UNIQUE COMMENT '提现单号',
    merchant_no VARCHAR(32) NOT NULL COMMENT '商户号',
    merchant_name VARCHAR(128) COMMENT '商户名称',
    withdraw_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '提现金额',
    actual_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '实际到账金额',
    fee_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '手续费',
    withdraw_type TINYINT NOT NULL DEFAULT 1 COMMENT '提现类型 1-T+1到账 2-即时到账',
    withdraw_status TINYINT NOT NULL DEFAULT 0 COMMENT '提现状态 0-待审核 1-审核通过 2-审核拒绝 3-转账中 4-提现成功 5-提现失败 6-已到账',
    bank_name VARCHAR(128) COMMENT '开户银行',
    bank_account VARCHAR(64) COMMENT '银行账号',
    account_name VARCHAR(64) COMMENT '开户名',
    audit_user VARCHAR(64) COMMENT '审核人',
    audit_time DATETIME COMMENT '审核时间',
    audit_remark VARCHAR(256) COMMENT '审核备注',
    transfer_no VARCHAR(32) COMMENT '转账单号',
    transfer_channel VARCHAR(32) COMMENT '转账通道',
    channel_transfer_no VARCHAR(64) COMMENT '通道转账单号',
    transfer_time DATETIME COMMENT '转账时间',
    transfer_fail_reason VARCHAR(256) COMMENT '转账失败原因',
    transfer_retry_count INT DEFAULT 0 COMMENT '转账重试次数',
    next_transfer_retry_time DATETIME COMMENT '下次转账重试时间',
    arrive_time DATETIME COMMENT '到账时间',
    remark VARCHAR(256) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_withdraw_no (withdraw_no),
    INDEX idx_withdraw_status (withdraw_status),
    INDEX idx_created_at (created_at),
    INDEX idx_merchant_status (merchant_no, withdraw_status),
    INDEX idx_status_created (withdraw_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户提现表';

-- =====================================================
-- 商户提现模块配置
-- 可添加到application.yml中
-- =====================================================
-- payhub:
--   merchant:
--     withdraw:
--       min-amount: 10                    # 最低提现金额（元）
--       max-amount: 500000                # 最高提现金额（元）
--       audit-threshold: 50000            # 人工审核门槛（元），超过此金额需要人工审核
--       t1-arrive-days: 1                 # T+1到账天数
--       t1-batch-enabled: true            # 是否启用T+1批量转账定时任务
--       t1-batch-size: 100                # T+1批量转账每次处理数量
--       t1-batch-cron: "0 0 2 * * ?"      # T+1批量转账执行时间（默认每天凌晨2点）
--       retry-enabled: true               # 是否启用失败重试定时任务
--       retry.max-times: 5                # 最大重试次数
--       retry.base-delay-minutes: 1       # 重试基础延迟分钟数（指数退避基数）

-- =====================================================
-- 插入测试数据
-- =====================================================

-- 注意：需要先确保merchant_info表中有对应的商户
-- 清理旧数据（可选）
-- DELETE FROM merchant_withdraw WHERE merchant_no IN ('M000001', 'M000002');

-- 插入测试提现记录
INSERT INTO merchant_withdraw (
    withdraw_no, merchant_no, merchant_name, withdraw_amount, actual_amount, fee_amount,
    withdraw_type, withdraw_status, bank_name, bank_account, account_name,
    audit_user, audit_time, audit_remark, remark, transfer_retry_count
) VALUES
('MW20250620000001', 'M000001', '沙箱测试商户1', 1000.00, 1000.00, 0.00,
1, 4, '中国工商银行', '6222021234567890123', '沙箱测试商户1',
'SYSTEM', NOW(), '自动审核通过', '测试T+1提现', 0),
('MW20250620000002', 'M000001', '沙箱测试商户1', 500.00, 499.50, 0.50,
2, 6, '中国工商银行', '6222021234567890123', '沙箱测试商户1',
'SYSTEM', NOW(), '自动审核通过', '测试即时到账提现', 0),
('MW20250620000003', 'M000002', '沙箱测试商户2', 60000.00, 60000.00, 0.00,
1, 0, '中国建设银行', '6217001234567890456', '沙箱测试商户2',
NULL, NULL, NULL, '大额提现待审核', 0);

-- =====================================================
-- 商户余额查询视图（可选）
-- =====================================================

-- 创建视图方便查询商户提现统计
CREATE OR REPLACE VIEW v_merchant_withdraw_stats AS
SELECT
    m.merchant_no,
    m.merchant_name,
    COUNT(w.id) AS total_count,
    COALESCE(SUM(CASE WHEN w.withdraw_status IN (4, 6) THEN w.withdraw_amount ELSE 0 END), 0) AS success_amount,
    COALESCE(SUM(CASE WHEN w.withdraw_status = 0 THEN w.withdraw_amount ELSE 0 END), 0) AS pending_amount,
    COALESCE(SUM(w.fee_amount), 0) AS total_fee
FROM merchant_info m
LEFT JOIN merchant_withdraw w ON m.merchant_no = w.merchant_no AND w.deleted = 0
WHERE m.deleted = 0
GROUP BY m.merchant_no, m.merchant_name;

-- =====================================================
-- 费率优惠活动模块
-- =====================================================

-- 费率优惠活动表
CREATE TABLE IF NOT EXISTS fee_promotion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    promotion_no VARCHAR(32) NOT NULL UNIQUE COMMENT '活动编号',
    promotion_name VARCHAR(128) NOT NULL COMMENT '活动名称',
    promotion_desc VARCHAR(512) COMMENT '活动描述',
    promotion_type TINYINT NOT NULL DEFAULT 1 COMMENT '活动类型 1-新商户优惠 2-节日特惠 3-周年庆 4-VIP专属 5-自定义活动',
    target_type TINYINT NOT NULL DEFAULT 1 COMMENT '目标范围 1-全体商户 2-指定行业 3-指定商户 4-新注册商户',
    target_industry_codes VARCHAR(512) COMMENT '目标行业编码列表，逗号分隔',
    target_merchant_nos TEXT COMMENT '目标商户号列表，逗号分隔',
    fee_type TINYINT NOT NULL DEFAULT 1 COMMENT '优惠类型 1-费率折扣 2-固定手续费 3-0手续费',
    discount_fee_rate DECIMAL(10,4) COMMENT '折扣费率（fee_type=1时有效，如0.5表示5折）',
    fixed_fee_amount DECIMAL(18,2) COMMENT '固定手续费（fee_type=2时有效，单位元）',
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME NOT NULL COMMENT '活动结束时间',
    total_quota DECIMAL(18,2) DEFAULT 0 COMMENT '总优惠额度（元），0表示不限制',
    used_quota DECIMAL(18,2) DEFAULT 0 COMMENT '已使用优惠额度',
    single_quota DECIMAL(18,2) DEFAULT 0 COMMENT '单笔最高优惠金额（元），0表示不限制',
    daily_quota DECIMAL(18,2) DEFAULT 0 COMMENT '每日优惠额度（元），0表示不限制',
    merchant_daily_quota DECIMAL(18,2) DEFAULT 0 COMMENT '单个商户每日优惠额度（元），0表示不限制',
    merchant_total_quota DECIMAL(18,2) DEFAULT 0 COMMENT '单个商户总优惠额度（元），0表示不限制',
    new_merchant_valid_days INT DEFAULT 0 COMMENT '新商户有效天数（target_type=4时有效，注册后多少天内有效）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '活动状态 0-草稿 1-未开始 2-进行中 3-已结束 4-已停用',
    created_by VARCHAR(64) COMMENT '创建人',
    updated_by VARCHAR(64) COMMENT '更新人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_promotion_no (promotion_no),
    INDEX idx_promotion_type (promotion_type),
    INDEX idx_status (status),
    INDEX idx_start_end_time (start_time, end_time),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费率优惠活动表';

-- 活动商户关联表
CREATE TABLE IF NOT EXISTS fee_promotion_merchant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    promotion_id BIGINT NOT NULL COMMENT '活动ID',
    promotion_no VARCHAR(32) NOT NULL COMMENT '活动编号',
    merchant_no VARCHAR(32) NOT NULL COMMENT '商户号',
    merchant_name VARCHAR(128) COMMENT '商户名称',
    bind_time DATETIME COMMENT '绑定时间',
    total_used_quota DECIMAL(18,2) DEFAULT 0 COMMENT '累计已使用优惠额度',
    daily_used_quota DECIMAL(18,2) DEFAULT 0 COMMENT '当日已使用优惠额度',
    last_use_date DATE COMMENT '最后使用日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_promotion_id (promotion_id),
    INDEX idx_promotion_no (promotion_no),
    INDEX idx_merchant_no (merchant_no),
    INDEX idx_promotion_merchant (promotion_no, merchant_no),
    UNIQUE KEY uk_promotion_merchant (promotion_no, merchant_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动商户关联表';

-- =====================================================
-- 插入测试数据 - 费率优惠活动
-- =====================================================

-- 新商户首月0费率活动
INSERT INTO fee_promotion (
    promotion_no, promotion_name, promotion_desc,
    promotion_type, target_type, fee_type,
    discount_fee_rate, fixed_fee_amount,
    start_time, end_time,
    total_quota, single_quota, merchant_total_quota,
    new_merchant_valid_days,
    status, created_by
) VALUES (
    'FP2025001', '新商户首月0费率', '新注册商户首月享受0手续费优惠',
    1, 4, 3,
    NULL, NULL,
    NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY),
    100000.00, 100.00, 500.00,
    30,
    2, 'SYSTEM'
);

-- 节日特惠活动 - 费率5折
INSERT INTO fee_promotion (
    promotion_no, promotion_name, promotion_desc,
    promotion_type, target_type, fee_type,
    discount_fee_rate, fixed_fee_amount,
    start_time, end_time,
    total_quota, single_quota,
    status, created_by
) VALUES (
    'FP2025002', '夏日狂欢季 费率5折', '全场提现手续费5折优惠',
    2, 1, 1,
    0.5, NULL,
    NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY),
    500000.00, 50.00,
    2, 'SYSTEM'
);

-- 为测试商户绑定新商户优惠
INSERT INTO fee_promotion_merchant (
    promotion_id, promotion_no, merchant_no, merchant_name, bind_time
) VALUES
(1, 'FP2025001', 'M000001', '沙箱测试商户1', NOW()),
(1, 'FP2025001', 'M000002', '沙箱测试商户2', NOW());
