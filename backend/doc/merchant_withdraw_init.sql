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
