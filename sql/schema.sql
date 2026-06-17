-- ============================================================
-- 支付网关平台数据库脚本
-- 数据库: MySQL 5.7+
-- 引擎: InnoDB
-- 字符集: utf8mb4
-- ============================================================

-- -----------------------------------------------
-- 1. 商户信息表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `merchant_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号（唯一）',
  `merchant_name` VARCHAR(128) NOT NULL COMMENT '商户名称',
  `business_license_no` VARCHAR(64) DEFAULT NULL COMMENT '营业执照号',
  `legal_person_name` VARCHAR(64) DEFAULT NULL COMMENT '法人姓名',
  `legal_person_id_no` VARCHAR(32) DEFAULT NULL COMMENT '法人身份证号',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `contact_email` VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
  `settlement_bank_name` VARCHAR(128) DEFAULT NULL COMMENT '结算银行名称',
  `settlement_bank_account` VARCHAR(64) DEFAULT NULL COMMENT '结算银行账号',
  `settlement_account_name` VARCHAR(128) DEFAULT NULL COMMENT '结算账户名称',
  `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0待审核 1通过 2拒绝',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `api_key_md5` VARCHAR(64) DEFAULT NULL COMMENT 'MD5密钥',
  `api_key_rsa_public` TEXT DEFAULT NULL COMMENT 'RSA公钥',
  `api_key_rsa_private` TEXT DEFAULT NULL COMMENT 'RSA私钥',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_no` (`merchant_no`),
  KEY `idx_status` (`status`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户信息表';

-- -----------------------------------------------
-- 2. 商户用户表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `merchant_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(256) NOT NULL COMMENT '密码（加密存储）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `role` VARCHAR(32) DEFAULT NULL COMMENT '角色：admin/operator/finance等',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_username` (`merchant_no`, `username`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户用户表';

-- -----------------------------------------------
-- 3. 商户支付配置表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `merchant_pay_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `pay_channel` VARCHAR(32) NOT NULL COMMENT '支付渠道：alipay/wechat/unionpay',
  `channel_merchant_id` VARCHAR(128) DEFAULT NULL COMMENT '渠道商户ID',
  `channel_app_id` VARCHAR(128) DEFAULT NULL COMMENT '渠道应用ID',
  `channel_secret_key` VARCHAR(512) DEFAULT NULL COMMENT '渠道密钥',
  `fee_rate` DECIMAL(10,6) DEFAULT NULL COMMENT '费率',
  `callback_url` VARCHAR(512) DEFAULT NULL COMMENT '页面回调地址',
  `notify_url` VARCHAR(512) DEFAULT NULL COMMENT '异步通知地址',
  `split_rule` JSON DEFAULT NULL COMMENT '分账规则（JSON）',
  `whitelist_ips` VARCHAR(1024) DEFAULT NULL COMMENT '白名单IP（逗号分隔）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_channel` (`merchant_no`, `pay_channel`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_pay_channel` (`pay_channel`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户支付配置表';

-- -----------------------------------------------
-- 4. 支付订单表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '平台订单号（唯一）',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `merchant_order_no` VARCHAR(128) NOT NULL COMMENT '商户订单号',
  `pay_amount` BIGINT NOT NULL COMMENT '支付金额（单位：分）',
  `pay_channel` VARCHAR(32) NOT NULL COMMENT '支付渠道：alipay/wechat/unionpay',
  `actual_amount` BIGINT DEFAULT NULL COMMENT '实际支付金额（单位：分）',
  `fee_amount` BIGINT DEFAULT NULL COMMENT '手续费金额（单位：分）',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付 1成功 2失败 3已关闭',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付成功时间',
  `user_identity` VARCHAR(128) DEFAULT NULL COMMENT '用户标识（如openid）',
  `product_subject` VARCHAR(256) DEFAULT NULL COMMENT '商品标题',
  `product_detail` VARCHAR(1024) DEFAULT NULL COMMENT '商品描述',
  `extra_params` JSON DEFAULT NULL COMMENT '扩展参数（JSON）',
  `channel_trade_no` VARCHAR(128) DEFAULT NULL COMMENT '渠道交易流水号',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  `notify_status` TINYINT NOT NULL DEFAULT 0 COMMENT '通知状态：0未通知 1通知成功 2通知失败',
  `notify_count` INT NOT NULL DEFAULT 0 COMMENT '通知次数',
  `pay_params` JSON DEFAULT NULL COMMENT '支付参数（JSON）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_merchant_order_no` (`merchant_no`, `merchant_order_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_pay_channel` (`pay_channel`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_notify_status` (`notify_status`),
  KEY `idx_channel_trade_no` (`channel_trade_no`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_pay_time` (`pay_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';

-- -----------------------------------------------
-- 5. 退款订单表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_refund` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '平台退款单号（唯一）',
  `order_no` VARCHAR(64) NOT NULL COMMENT '平台订单号',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `merchant_refund_no` VARCHAR(128) DEFAULT NULL COMMENT '商户退款单号',
  `refund_amount` BIGINT NOT NULL COMMENT '退款金额（单位：分）',
  `refund_reason` VARCHAR(512) DEFAULT NULL COMMENT '退款原因',
  `refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态：0处理中 1成功 2失败',
  `channel_refund_no` VARCHAR(128) DEFAULT NULL COMMENT '渠道退款流水号',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `last_retry_time` DATETIME DEFAULT NULL COMMENT '上次重试时间',
  `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_refund_status` (`refund_status`),
  KEY `idx_channel_refund_no` (`channel_refund_no`),
  KEY `idx_next_retry_time` (`next_retry_time`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款订单表';

-- -----------------------------------------------
-- 6. 分账规则表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_split_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_no` VARCHAR(64) NOT NULL COMMENT '规则编号',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `split_details` JSON NOT NULL COMMENT '分账明细（JSON）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_no` (`rule_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账规则表';

-- -----------------------------------------------
-- 7. 分账明细表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_split_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `split_detail_no` VARCHAR(64) NOT NULL COMMENT '分账明细单号',
  `order_no` VARCHAR(64) NOT NULL COMMENT '支付订单号',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户号',
  `rule_no` VARCHAR(64) DEFAULT NULL COMMENT '分账规则号',
  `receiver_account` VARCHAR(128) DEFAULT NULL COMMENT '接收方账户/标识',
  `receiver_name` VARCHAR(128) DEFAULT NULL COMMENT '接收方名称',
  `split_type` VARCHAR(32) DEFAULT NULL COMMENT '分账类型：PERCENT-按比例 FIXED-固定金额 REMAINING-剩余',
  `split_value` DECIMAL(18,2) DEFAULT NULL COMMENT '分账值，比例%或固定金额分',
  `split_amount` DECIMAL(18,2) NOT NULL COMMENT '实际分账金额，单位分',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待结算 1已结算',
  `settle_time` DATETIME DEFAULT NULL COMMENT '结算时间',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_split_detail_no` (`split_detail_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账明细表';

-- -----------------------------------------------
-- 8. 结算记录表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `settlement_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `settlement_no` VARCHAR(64) NOT NULL COMMENT '结算单号',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `settle_date` DATE NOT NULL COMMENT '结算日期',
  `total_amount` BIGINT NOT NULL COMMENT '交易总额（单位：分）',
  `fee_amount` BIGINT NOT NULL COMMENT '手续费总额（单位：分）',
  `actual_settle_amount` BIGINT NOT NULL COMMENT '实际结算金额（单位：分）',
  `order_count` INT NOT NULL COMMENT '订单笔数',
  `settle_status` TINYINT NOT NULL DEFAULT 0 COMMENT '结算状态：0待结算 1已结算',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  UNIQUE KEY `uk_merchant_settle_date` (`merchant_no`, `settle_date`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_settle_date` (`settle_date`),
  KEY `idx_settle_status` (`settle_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算记录表';

-- -----------------------------------------------
-- 8. 风控日志表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `risk_control_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
  `risk_type` VARCHAR(64) NOT NULL COMMENT '风控类型',
  `risk_level` TINYINT NOT NULL DEFAULT 0 COMMENT '风险等级：0低 1中 2高',
  `risk_desc` VARCHAR(1024) DEFAULT NULL COMMENT '风险描述',
  `handle_result` VARCHAR(512) DEFAULT NULL COMMENT '处理结果',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_risk_type` (`risk_type`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控日志表';

-- -----------------------------------------------
-- 9. 对账记录表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `reconcile_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reconcile_no` VARCHAR(64) NOT NULL COMMENT '对账单号',
  `reconcile_date` DATE NOT NULL COMMENT '对账日期',
  `pay_channel` VARCHAR(32) NOT NULL COMMENT '支付渠道：alipay/wechat/unionpay',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '交易总笔数',
  `match_count` INT NOT NULL DEFAULT 0 COMMENT '匹配笔数',
  `mismatch_count` INT NOT NULL DEFAULT 0 COMMENT '不匹配笔数',
  `reconcile_status` TINYINT NOT NULL DEFAULT 0 COMMENT '对账状态：0处理中 1完成 2异常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reconcile_no` (`reconcile_no`),
  UNIQUE KEY `uk_channel_reconcile_date` (`pay_channel`, `reconcile_date`),
  KEY `idx_reconcile_date` (`reconcile_date`),
  KEY `idx_pay_channel` (`pay_channel`),
  KEY `idx_reconcile_status` (`reconcile_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账记录表';

-- -----------------------------------------------
-- 10. API访问日志表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `api_access_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_no` VARCHAR(32) DEFAULT NULL COMMENT '商户编号',
  `api_uri` VARCHAR(512) NOT NULL COMMENT 'API接口地址',
  `request_method` VARCHAR(16) NOT NULL COMMENT '请求方法：GET/POST/PUT/DELETE',
  `request_ip` VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
  `response_code` VARCHAR(32) DEFAULT NULL COMMENT '响应码',
  `response_time_ms` INT DEFAULT NULL COMMENT '响应时间（毫秒）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_api_uri` (`api_uri`(191)),
  KEY `idx_request_method` (`request_method`),
  KEY `idx_response_code` (`response_code`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API访问日志表';

-- -----------------------------------------------
-- 11. 沙箱测试记录表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `sandbox_test_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `test_case` VARCHAR(128) NOT NULL COMMENT '测试用例名称',
  `test_result` TINYINT NOT NULL DEFAULT 0 COMMENT '测试结果：0失败 1成功',
  `test_data` JSON DEFAULT NULL COMMENT '测试数据（JSON）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_test_case` (`test_case`),
  KEY `idx_test_result` (`test_result`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='沙箱测试记录表';
