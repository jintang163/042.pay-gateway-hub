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
  `industry_code` VARCHAR(64) DEFAULT NULL COMMENT '行业编码',
  `industry_name` VARCHAR(128) DEFAULT NULL COMMENT '行业名称',
  `api_key_md5` VARCHAR(64) DEFAULT NULL COMMENT 'MD5密钥',
  `api_key_rsa_public` TEXT DEFAULT NULL COMMENT 'RSA公钥',
  `api_key_rsa_private` TEXT DEFAULT NULL COMMENT 'RSA私钥',
  `audit_step` TINYINT NOT NULL DEFAULT 1 COMMENT '审核步骤：1=资料提交 2=工商核验中 3=工商核验完成 4=风险评估中 5=评估完成 6=自动审核完成 7=人工审核中',
  `risk_level` VARCHAR(16) DEFAULT NULL COMMENT '风险等级：LOW低 MEDIUM中 HIGH高',
  `risk_score` INT DEFAULT NULL COMMENT '风险评分0-100，越高风险越大',
  `business_verify_passed` TINYINT DEFAULT NULL COMMENT '工商核验是否通过：0否 1是',
  `business_verify_result` TEXT DEFAULT NULL COMMENT '工商核验结果JSON',
  `business_verify_time` DATETIME DEFAULT NULL COMMENT '工商核验时间',
  `auto_audit_passed` TINYINT DEFAULT NULL COMMENT '自动审核是否通过：0否 1是',
  `auto_audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '自动审核备注',
  `auto_audit_time` DATETIME DEFAULT NULL COMMENT '自动审核时间',
  `manual_audit_user` VARCHAR(64) DEFAULT NULL COMMENT '人工审核人',
  `manual_audit_time` DATETIME DEFAULT NULL COMMENT '人工审核时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_no` (`merchant_no`),
  KEY `idx_status` (`status`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_audit_step` (`audit_step`),
  KEY `idx_risk_level` (`risk_level`),
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
  `parent_order_no` VARCHAR(64) DEFAULT NULL COMMENT '父订单号（聚合单子单关联用）',
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
  `split_detail_no` VARCHAR(32) NOT NULL COMMENT '分账明细单号',
  `settlement_id` BIGINT DEFAULT NULL COMMENT '结算记录ID',
  `settlement_no` VARCHAR(32) DEFAULT NULL COMMENT '结算单号',
  `order_no` VARCHAR(32) NOT NULL COMMENT '支付订单号',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户号',
  `rule_no` VARCHAR(32) DEFAULT NULL COMMENT '分账规则号',
  `receiver_account` VARCHAR(64) NOT NULL COMMENT '接收方账户/标识',
  `receiver_name` VARCHAR(64) DEFAULT NULL COMMENT '接收方名称',
  `split_type` VARCHAR(16) NOT NULL COMMENT '分账类型 PERCENT比例 FIXED固定金额 REMAINING剩余',
  `split_value` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '分账值 比例%或固定金额',
  `split_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '实际分账金额(分)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '分账状态 0待结算 1已结算',
  `transfer_status` TINYINT NOT NULL DEFAULT 0 COMMENT '转账状态 0待打款 1打款中 2打款成功 3打款失败',
  `transfer_no` VARCHAR(32) DEFAULT NULL COMMENT '内部转账单号',
  `channel_transfer_no` VARCHAR(64) DEFAULT NULL COMMENT '渠道转账单号',
  `transfer_fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '打款失败原因',
  `transfer_time` DATETIME DEFAULT NULL COMMENT '打款完成时间',
  `transfer_retry_count` INT NOT NULL DEFAULT 0 COMMENT '打款重试次数',
  `next_transfer_retry_time` DATETIME DEFAULT NULL COMMENT '下次打款重试时间',
  `transfer_channel` VARCHAR(32) DEFAULT NULL COMMENT '转账通道',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `settle_time` DATETIME DEFAULT NULL COMMENT '结算时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_split_detail_no` (`split_detail_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_settlement_id` (`settlement_id`),
  KEY `idx_status` (`status`),
  KEY `idx_transfer_status` (`transfer_status`),
  KEY `idx_next_transfer_retry` (`next_transfer_retry_time`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账明细表';

-- -----------------------------------------------
-- 8. 结算记录表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `settlement_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `settlement_no` VARCHAR(32) NOT NULL COMMENT '结算单号',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户号',
  `pay_channel` VARCHAR(32) NOT NULL COMMENT '支付渠道 ALIPAY/WECHAT_PAY/UNION_PAY',
  `settle_date` DATE NOT NULL COMMENT '结算日期',
  `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '订单总金额(分)',
  `fee_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '手续费总金额(分)',
  `actual_settle_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '实际结算金额(分)',
  `order_count` INT NOT NULL DEFAULT 0 COMMENT '订单笔数',
  `settle_status` TINYINT NOT NULL DEFAULT 0 COMMENT '结算状态 0待结算 1结算中 2已结算 3结算失败',
  `bank_name` VARCHAR(64) DEFAULT NULL COMMENT '银行名称',
  `bank_account` VARCHAR(64) DEFAULT NULL COMMENT '银行账号',
  `account_name` VARCHAR(64) DEFAULT NULL COMMENT '账户名称',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
  `settle_time` DATETIME DEFAULT NULL COMMENT '结算完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  UNIQUE KEY `uk_merchant_channel_settle_date` (`merchant_no`, `pay_channel`, `settle_date`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_settle_date` (`settle_date`),
  KEY `idx_settle_status` (`settle_status`),
  KEY `idx_pay_channel` (`pay_channel`),
  KEY `idx_next_retry_time` (`next_retry_time`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算记录表';

-- -----------------------------------------------
-- 8. 风控规则表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `risk_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` VARCHAR(64) NOT NULL COMMENT '规则编码',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `rule_type` VARCHAR(32) NOT NULL COMMENT '规则类型：AMOUNT金额/FREQUENCY频率/IP黑名单/DEVICE设备/BEHAVIOR行为',
  `risk_level` TINYINT NOT NULL DEFAULT 1 COMMENT '风险等级：1低 2中 3高',
  `rule_condition` TEXT NOT NULL COMMENT '规则条件（JSON格式参数）',
  `rule_content` TEXT COMMENT 'Drools规则内容',
  `action_type` VARCHAR(32) NOT NULL DEFAULT 'BLOCK' COMMENT '命中动作：PASS放行/BLOCK拦截/SMS短信验证/MANUAL人工审核',
  `sms_template_id` VARCHAR(64) DEFAULT NULL COMMENT '短信模板ID（短信验证时使用）',
  `priority` INT NOT NULL DEFAULT 100 COMMENT '优先级：数字越小优先级越高',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `effect_start_time` DATETIME DEFAULT NULL COMMENT '生效开始时间',
  `effect_end_time` DATETIME DEFAULT NULL COMMENT '生效结束时间',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控规则表';

-- -----------------------------------------------
-- 9. 黑名单表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `risk_blacklist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `list_type` VARCHAR(32) NOT NULL COMMENT '类型：IP/USER/MERCHANT/DEVICE',
  `list_value` VARCHAR(256) NOT NULL COMMENT '值（IP地址/用户ID/商户号/设备指纹）',
  `list_source` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL手动/SYSTEM系统/AUDIT审核/RISK风控命中',
  `risk_level` TINYINT NOT NULL DEFAULT 2 COMMENT '关联风险等级：1低 2中 3高',
  `reason` VARCHAR(512) DEFAULT NULL COMMENT '加入原因',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0无效 1有效',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间（null表示永久有效）',
  `hit_count` INT NOT NULL DEFAULT 0 COMMENT '命中次数',
  `last_hit_time` DATETIME DEFAULT NULL COMMENT '最后命中时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_value` (`list_type`, `list_value`),
  KEY `idx_list_type` (`list_type`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控黑名单表';

-- -----------------------------------------------
-- 10. 白名单表（免检）
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `risk_whitelist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `list_type` VARCHAR(32) NOT NULL COMMENT '类型：IP/USER/MERCHANT/DEVICE',
  `list_value` VARCHAR(256) NOT NULL COMMENT '值（IP地址/用户ID/商户号/设备指纹）',
  `list_source` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL手动/SYSTEM系统/AUDIT审核',
  `bypass_rules` VARCHAR(512) DEFAULT NULL COMMENT '免检规则编码（逗号分隔，空表示全部免检）',
  `reason` VARCHAR(512) DEFAULT NULL COMMENT '加入原因',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0无效 1有效',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间（null表示永久有效）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_value` (`list_type`, `list_value`),
  KEY `idx_list_type` (`list_type`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控白名单表（免检）';

-- -----------------------------------------------
-- 11. 设备指纹表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `risk_device_fingerprint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(128) NOT NULL COMMENT '设备唯一标识',
  `device_type` VARCHAR(32) DEFAULT NULL COMMENT '设备类型：MOBILE/DESKTOP/TABLET',
  `os_type` VARCHAR(32) DEFAULT NULL COMMENT '操作系统：IOS/ANDROID/WINDOWS/MAC/LINUX',
  `os_version` VARCHAR(64) DEFAULT NULL COMMENT '系统版本',
  `browser_type` VARCHAR(32) DEFAULT NULL COMMENT '浏览器类型',
  `browser_version` VARCHAR(64) DEFAULT NULL COMMENT '浏览器版本',
  `app_version` VARCHAR(64) DEFAULT NULL COMMENT 'APP版本',
  `screen_resolution` VARCHAR(32) DEFAULT NULL COMMENT '屏幕分辨率',
  `language` VARCHAR(32) DEFAULT NULL COMMENT '语言设置',
  `timezone` VARCHAR(32) DEFAULT NULL COMMENT '时区',
  `user_agent` VARCHAR(1024) DEFAULT NULL COMMENT 'User Agent',
  `user_identity` VARCHAR(128) DEFAULT NULL COMMENT '关联用户标识',
  `merchant_no` VARCHAR(32) DEFAULT NULL COMMENT '关联商户号',
  `first_seen_ip` VARCHAR(64) DEFAULT NULL COMMENT '首次出现IP',
  `last_seen_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后出现IP',
  `first_seen_time` DATETIME DEFAULT NULL COMMENT '首次出现时间',
  `last_seen_time` DATETIME DEFAULT NULL COMMENT '最后出现时间',
  `total_request_count` BIGINT NOT NULL DEFAULT 0 COMMENT '总请求次数',
  `risk_score` INT NOT NULL DEFAULT 0 COMMENT '风险评分 0-100',
  `risk_tags` VARCHAR(512) DEFAULT NULL COMMENT '风险标签（逗号分隔）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0标记异常 1正常',
  `extra_info` JSON DEFAULT NULL COMMENT '扩展信息（JSON）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_user_identity` (`user_identity`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_risk_score` (`risk_score`),
  KEY `idx_status` (`status`),
  KEY `idx_last_seen_time` (`last_seen_time`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备指纹表';

-- -----------------------------------------------
-- 12. 风控审核记录表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `risk_audit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `audit_no` VARCHAR(64) NOT NULL COMMENT '审核单号',
  `risk_log_id` BIGINT NOT NULL COMMENT '关联风控日志ID',
  `merchant_no` VARCHAR(32) DEFAULT NULL COMMENT '商户编号',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
  `audit_type` VARCHAR(32) NOT NULL COMMENT '审核类型：BLOCK拦截审核/SMS验证审核/RISK_ALERT风险告警审核',
  `audit_level` TINYINT NOT NULL DEFAULT 1 COMMENT '审核级别：1一审 2二审',
  `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0待审核 1审核通过（放行） 2审核拒绝（拦截） 3需要二审',
  `risk_level_before` TINYINT DEFAULT NULL COMMENT '审核前风险等级',
  `risk_level_after` TINYINT DEFAULT NULL COMMENT '审核后风险等级',
  `audit_result` VARCHAR(32) DEFAULT NULL COMMENT '审核结果：PASS放行/BLOCK拦截',
  `audit_remark` VARCHAR(1024) DEFAULT NULL COMMENT '审核意见',
  `audit_user_id` VARCHAR(64) DEFAULT NULL COMMENT '审核人ID',
  `audit_user_name` VARCHAR(64) DEFAULT NULL COMMENT '审核人姓名',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `sms_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已短信验证：0否 1是',
  `sms_mobile` VARCHAR(20) DEFAULT NULL COMMENT '短信验证手机号',
  `sms_verify_time` DATETIME DEFAULT NULL COMMENT '短信验证时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_audit_no` (`audit_no`),
  KEY `idx_risk_log_id` (`risk_log_id`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_audit_user_id` (`audit_user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控审核记录表';

-- -----------------------------------------------
-- 13. 风控日志表（增强版）
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `risk_control_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
  `risk_type` VARCHAR(64) NOT NULL COMMENT '风控类型',
  `risk_level` TINYINT NOT NULL DEFAULT 0 COMMENT '风险等级：0无风险 1低 2中 3高',
  `risk_rule` VARCHAR(512) DEFAULT NULL COMMENT '命中的规则编码（逗号分隔）',
  `risk_desc` VARCHAR(1024) DEFAULT NULL COMMENT '风险描述',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  `user_identity` VARCHAR(128) DEFAULT NULL COMMENT '用户标识',
  `device_id` VARCHAR(128) DEFAULT NULL COMMENT '设备指纹ID',
  `pay_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '支付金额',
  `pay_channel` VARCHAR(32) DEFAULT NULL COMMENT '支付渠道',
  `pay_type` VARCHAR(32) DEFAULT NULL COMMENT '支付方式',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数（JSON）',
  `action_type` VARCHAR(32) NOT NULL DEFAULT 'PASS' COMMENT '执行动作：PASS放行/BLOCK拦截/SMS短信验证/MANUAL人工审核',
  `handle_result` TINYINT DEFAULT NULL COMMENT '处理结果：0拦截 1通过 2待审核',
  `handle_desc` VARCHAR(512) DEFAULT NULL COMMENT '处理描述',
  `audit_status` TINYINT DEFAULT 0 COMMENT '审核状态：0无需审核 1待审核 2审核通过 3审核拒绝',
  `audit_id` BIGINT DEFAULT NULL COMMENT '关联审核记录ID',
  `trigger_time` DATETIME DEFAULT NULL COMMENT '触发时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_risk_type` (`risk_type`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_client_ip` (`client_ip`),
  KEY `idx_user_identity` (`user_identity`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_trigger_time` (`trigger_time`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控日志表';

-- -----------------------------------------------
-- 初始化风控规则数据
-- -----------------------------------------------
INSERT INTO `risk_rule` (`rule_code`, `rule_name`, `rule_type`, `risk_level`, `rule_condition`, `rule_content`, `action_type`, `priority`, `status`, `remark`, `created_at`, `updated_at`) VALUES
('SINGLE_AMOUNT_LIMIT', '单笔金额超限规则', 'AMOUNT', 2, '{"maxAmount": 5000000}', NULL, 'BLOCK', 10, 1, '单笔金额超过5万元触发中风险拦截', NOW(), NOW()),
('DAILY_AMOUNT_LIMIT', '日累计金额超限规则', 'AMOUNT', 2, '{"minAmount": 50000000}', NULL, 'MANUAL', 20, 1, '日累计金额超过50万元触发人工审核', NOW(), NOW()),
('IP_FREQUENCY_LIMIT', 'IP高频请求规则', 'FREQUENCY', 2, '{"windowSeconds": 60, "maxCount": 100}', NULL, 'MANUAL', 30, 1, '同一IP+商户1分钟内请求超过100次需人工审核', NOW(), NOW()),
('IP_BLACKLIST_RULE', 'IP黑名单规则', 'IP_BLACKLIST', 3, '{}', NULL, 'BLOCK', 5, 1, 'IP在黑名单中直接拦截', NOW(), NOW()),
('DEVICE_HIGH_RISK', '高风险设备规则', 'DEVICE', 3, '{"minRiskScore": 80}', NULL, 'SMS', 15, 1, '设备风险评分≥80需要短信二次验证', NOW(), NOW()),
('DEVICE_MEDIUM_RISK', '中风险设备规则', 'DEVICE', 2, '{"minRiskScore": 60}', NULL, 'MANUAL', 25, 0, '设备风险评分≥60需要人工审核（默认禁用）', NOW(), NOW()),
('LARGE_AMOUNT_NO_USER', '大额无身份验证交易', 'BEHAVIOR', 3, '{"customCondition": "payAmount != null && payAmount.compareTo(new java.math.BigDecimal(100000)) > 0 && (userIdentity == null || userIdentity.trim().length() == 0)"}', NULL, 'MANUAL', 50, 1, '超过1万元且无用户身份的交易需人工审核', NOW(), NOW());

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
-- 12. 对账差异明细表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `reconcile_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `detail_no` VARCHAR(64) NOT NULL COMMENT '差异明细单号',
  `reconcile_no` VARCHAR(64) NOT NULL COMMENT '对账单号',
  `reconcile_date` DATE NOT NULL COMMENT '对账日期',
  `pay_channel` VARCHAR(32) NOT NULL COMMENT '支付渠道：alipay/wechat/unionpay',
  `diff_type` TINYINT NOT NULL COMMENT '差异类型：1长款(渠道有本地无) 2短款(本地有渠道无) 3金额不一致 4状态不一致',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '平台订单号',
  `merchant_no` VARCHAR(32) DEFAULT NULL COMMENT '商户编号',
  `channel_trade_no` VARCHAR(128) DEFAULT NULL COMMENT '渠道交易流水号',
  `local_amount` BIGINT DEFAULT NULL COMMENT '本地金额(分)',
  `channel_amount` BIGINT DEFAULT NULL COMMENT '渠道金额(分)',
  `diff_amount` BIGINT DEFAULT NULL COMMENT '差异金额(分)',
  `local_status` TINYINT DEFAULT NULL COMMENT '本地支付状态',
  `channel_status` VARCHAR(32) DEFAULT NULL COMMENT '渠道支付状态',
  `local_pay_time` DATETIME DEFAULT NULL COMMENT '本地支付时间',
  `channel_pay_time` DATETIME DEFAULT NULL COMMENT '渠道支付时间',
  `error_order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联差错单号',
  `handle_status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理 1处理中 2已处理 3忽略',
  `handle_remark` VARCHAR(512) DEFAULT NULL COMMENT '处理备注',
  `handle_user_id` VARCHAR(64) DEFAULT NULL COMMENT '处理人ID',
  `handle_user_name` VARCHAR(64) DEFAULT NULL COMMENT '处理人姓名',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `extra_info` JSON DEFAULT NULL COMMENT '扩展信息(JSON)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_detail_no` (`detail_no`),
  KEY `idx_reconcile_no` (`reconcile_no`),
  KEY `idx_reconcile_date` (`reconcile_date`),
  KEY `idx_pay_channel` (`pay_channel`),
  KEY `idx_diff_type` (`diff_type`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_channel_trade_no` (`channel_trade_no`),
  KEY `idx_handle_status` (`handle_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账差异明细表';

-- -----------------------------------------------
-- 13. 差错单表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `error_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `error_no` VARCHAR(64) NOT NULL COMMENT '差错单号',
  `reconcile_no` VARCHAR(64) DEFAULT NULL COMMENT '关联对账单号',
  `reconcile_detail_id` BIGINT DEFAULT NULL COMMENT '关联差异明细ID',
  `pay_channel` VARCHAR(32) NOT NULL COMMENT '支付渠道：alipay/wechat/unionpay',
  `error_type` TINYINT NOT NULL COMMENT '差错类型：1长款 2短款 3金额差异 4状态差异',
  `handle_type` TINYINT DEFAULT NULL COMMENT '处理方式：1补单 2退款 3调账 4忽略',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '平台订单号',
  `merchant_no` VARCHAR(32) DEFAULT NULL COMMENT '商户编号',
  `channel_trade_no` VARCHAR(128) DEFAULT NULL COMMENT '渠道交易流水号',
  `order_amount` BIGINT DEFAULT NULL COMMENT '订单金额(分)',
  `actual_amount` BIGINT DEFAULT NULL COMMENT '实际金额(分)',
  `diff_amount` BIGINT DEFAULT NULL COMMENT '差异金额(分)',
  `error_status` TINYINT NOT NULL DEFAULT 0 COMMENT '差错状态：0待处理 1处理中 2处理成功 3处理失败 4已关闭',
  `apply_user_id` VARCHAR(64) DEFAULT NULL COMMENT '申请人ID',
  `apply_user_name` VARCHAR(64) DEFAULT NULL COMMENT '申请人姓名',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `apply_remark` VARCHAR(512) DEFAULT NULL COMMENT '申请备注',
  `audit_user_id` VARCHAR(64) DEFAULT NULL COMMENT '审核人ID',
  `audit_user_name` VARCHAR(64) DEFAULT NULL COMMENT '审核人姓名',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `audit_status` TINYINT DEFAULT NULL COMMENT '审核状态：0待审核 1审核通过 2审核拒绝',
  `handle_user_id` VARCHAR(64) DEFAULT NULL COMMENT '处理人ID',
  `handle_user_name` VARCHAR(64) DEFAULT NULL COMMENT '处理人姓名',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理完成时间',
  `handle_result` VARCHAR(512) DEFAULT NULL COMMENT '处理结果',
  `refund_no` VARCHAR(64) DEFAULT NULL COMMENT '关联退款单号',
  `new_order_no` VARCHAR(64) DEFAULT NULL COMMENT '补单生成的新订单号',
  `extra_info` JSON DEFAULT NULL COMMENT '扩展信息(JSON)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_error_no` (`error_no`),
  KEY `idx_reconcile_no` (`reconcile_no`),
  KEY `idx_reconcile_detail_id` (`reconcile_detail_id`),
  KEY `idx_pay_channel` (`pay_channel`),
  KEY `idx_error_type` (`error_type`),
  KEY `idx_error_status` (`error_status`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='差错单表';

-- -----------------------------------------------
-- 14. API访问日志表
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

-- -----------------------------------------------
-- 15. 回调模拟日志表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `callback_simulate_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `log_no` VARCHAR(64) NOT NULL COMMENT '日志编号',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `merchant_name` VARCHAR(128) DEFAULT NULL COMMENT '商户名称',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
  `callback_url` VARCHAR(512) NOT NULL COMMENT '回调地址',
  `callback_type` VARCHAR(32) NOT NULL DEFAULT 'PAY' COMMENT '回调类型：PAY支付/REFUND退款',
  `simulate_status` VARCHAR(16) NOT NULL COMMENT '模拟状态：SUCCESS成功/FAIL失败',
  `sign_type` VARCHAR(16) NOT NULL DEFAULT 'MD5' COMMENT '签名类型：MD5/RSA/SM2',
  `request_headers` JSON DEFAULT NULL COMMENT '请求头（JSON）',
  `request_body` TEXT DEFAULT NULL COMMENT '请求体（JSON）',
  `response_http_status` INT DEFAULT NULL COMMENT '响应HTTP状态码',
  `response_body` TEXT DEFAULT NULL COMMENT '响应体',
  `response_time_ms` INT DEFAULT NULL COMMENT '响应耗时（毫秒）',
  `callback_status` TINYINT NOT NULL DEFAULT 0 COMMENT '回调状态：0待发送 1发送成功 2发送失败 3已重发',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_log_no` (`log_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_callback_type` (`callback_type`),
  KEY `idx_simulate_status` (`simulate_status`),
  KEY `idx_callback_status` (`callback_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调模拟日志表';

-- -----------------------------------------------
-- 16. 动态手续费规则表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `fee_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_no` VARCHAR(32) NOT NULL COMMENT '规则编号',
  `industry_code` VARCHAR(64) NOT NULL COMMENT '行业编码',
  `industry_name` VARCHAR(128) NOT NULL COMMENT '行业名称',
  `pay_channel` VARCHAR(32) DEFAULT NULL COMMENT '支付渠道（空=全部渠道）',
  `min_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额区间最小值（元，含）',
  `max_amount` DECIMAL(18,2) NOT NULL DEFAULT 99999999.99 COMMENT '金额区间最大值（元，含）',
  `fee_rate` DECIMAL(10,6) NOT NULL COMMENT '费率(百分比，如0.6表示0.6%)',
  `min_fee` DECIMAL(18,2) DEFAULT 0.00 COMMENT '单笔最低手续费（元）',
  `max_fee` DECIMAL(18,2) DEFAULT NULL COMMENT '单笔最高手续费（元，空=不限制）',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级（数字越大优先级越高）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_no` (`rule_no`),
  KEY `idx_industry_code` (`industry_code`),
  KEY `idx_pay_channel` (`pay_channel`),
  KEY `idx_amount_range` (`min_amount`, `max_amount`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态手续费规则表';

-- -----------------------------------------------
-- 22. 支付页面个性化配置表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `payment_page_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户号',
  `page_title` VARCHAR(100) DEFAULT NULL COMMENT '页面标题',
  `logo_url` VARCHAR(1024) DEFAULT NULL COMMENT 'Logo图片URL',
  `primary_color` VARCHAR(20) DEFAULT '#1677ff' COMMENT '主色调',
  `secondary_color` VARCHAR(20) DEFAULT NULL COMMENT '辅助色',
  `background_color` VARCHAR(20) DEFAULT '#f5f7fa' COMMENT '页面背景色',
  `text_color` VARCHAR(20) DEFAULT '#333333' COMMENT '文字颜色',
  `button_color` VARCHAR(20) DEFAULT '#1677ff' COMMENT '按钮颜色',
  `button_text_color` VARCHAR(20) DEFAULT '#ffffff' COMMENT '按钮文字颜色',
  `color_scheme_code` VARCHAR(50) DEFAULT 'DEFAULT' COMMENT '配色方案编码',
  `custom_css` TEXT DEFAULT NULL COMMENT '自定义CSS',
  `footer_text` VARCHAR(200) DEFAULT NULL COMMENT '页脚文字',
  `return_url` VARCHAR(500) DEFAULT NULL COMMENT '支付完成返回地址',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_no` (`merchant_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付页面个性化配置表';

-- -----------------------------------------------
-- 23. 商户配置测试记录表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `merchant_config_test_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `log_no` VARCHAR(64) NOT NULL COMMENT '测试日志编号',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户号',
  `merchant_name` VARCHAR(128) DEFAULT NULL COMMENT '商户名称',
  `total_tests` INT NOT NULL DEFAULT 0 COMMENT '测试项总数',
  `passed_tests` INT NOT NULL DEFAULT 0 COMMENT '通过数',
  `failed_tests` INT NOT NULL DEFAULT 0 COMMENT '失败/警告数',
  `overall_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '整体状态：PASS/FAIL/PARTIAL',
  `overall_status_desc` VARCHAR(32) DEFAULT NULL COMMENT '状态描述',
  `callback_url` VARCHAR(512) DEFAULT NULL COMMENT '使用的回调地址',
  `sign_type` VARCHAR(16) DEFAULT NULL COMMENT '使用的签名算法',
  `total_time_ms` INT DEFAULT NULL COMMENT '总耗时（毫秒）',
  `items_json` MEDIUMTEXT DEFAULT NULL COMMENT '测试项详情（JSON）',
  `summary` TEXT DEFAULT NULL COMMENT '测试摘要',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_log_no` (`log_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_overall_status` (`overall_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户配置测试记录表';

-- -----------------------------------------------
-- 24. 代理关系表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `agent_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `merchant_name` VARCHAR(128) NOT NULL COMMENT '商户名称',
  `parent_merchant_no` VARCHAR(32) DEFAULT NULL COMMENT '上级商户编号',
  `parent_merchant_name` VARCHAR(128) DEFAULT NULL COMMENT '上级商户名称',
  `agent_level` INT NOT NULL DEFAULT 1 COMMENT '代理层级 1=一级 2=二级 ...',
  `agent_path` VARCHAR(512) DEFAULT NULL COMMENT '代理路径 如：M000001/M000002/M000003',
  `commission_rate` DECIMAL(10,4) DEFAULT 0.0000 COMMENT '分润比例(百分比)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_no` (`merchant_no`),
  KEY `idx_parent_merchant_no` (`parent_merchant_no`),
  KEY `idx_agent_level` (`agent_level`),
  KEY `idx_agent_path` (`agent_path`(255)),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理关系表';

-- -----------------------------------------------
-- 25. 代理分润规则表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `agent_profit_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_no` VARCHAR(32) NOT NULL COMMENT '规则编号',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '代理商号',
  `merchant_name` VARCHAR(128) DEFAULT NULL COMMENT '代理商名称',
  `agent_level` INT NOT NULL DEFAULT 1 COMMENT '适用的下级层级',
  `commission_rate` DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '分润比例(百分比)',
  `min_commission` DECIMAL(18,2) DEFAULT 0.00 COMMENT '最低分润金额(分)',
  `settle_type` TINYINT NOT NULL DEFAULT 0 COMMENT '结算方式：0单独结算 1叠加分账',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_no` (`rule_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_agent_level` (`agent_level`),
  KEY `idx_settle_type` (`settle_type`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理分润规则表';

-- -----------------------------------------------
-- 26. 代理分润记录表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `agent_profit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `profit_no` VARCHAR(32) NOT NULL COMMENT '分润单号',
  `order_no` VARCHAR(64) NOT NULL COMMENT '平台订单号',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '下级商户号',
  `merchant_name` VARCHAR(128) DEFAULT NULL COMMENT '下级商户名称',
  `agent_merchant_no` VARCHAR(32) NOT NULL COMMENT '代理商号',
  `agent_merchant_name` VARCHAR(128) DEFAULT NULL COMMENT '代理商名称',
  `agent_level` INT NOT NULL DEFAULT 1 COMMENT '代理层级',
  `order_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额(分)',
  `fee_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '手续费金额(分)',
  `profit_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '分润金额(分)',
  `commission_rate` DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '分润比例(百分比)',
  `settle_date` VARCHAR(16) NOT NULL COMMENT '结算日期 yyyy-MM-dd',
  `profit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '分润状态：0待结算 1已结算 2结算失败',
  `settlement_id` BIGINT DEFAULT NULL COMMENT '结算记录ID',
  `settlement_no` VARCHAR(32) DEFAULT NULL COMMENT '结算单号',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profit_no` (`profit_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_agent_merchant_no` (`agent_merchant_no`),
  KEY `idx_agent_level` (`agent_level`),
  KEY `idx_settle_date` (`settle_date`),
  KEY `idx_profit_status` (`profit_status`),
  KEY `idx_settlement_id` (`settlement_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理分润记录表';

-- -----------------------------------------------
-- 27. 代理佣金提现表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `agent_withdraw` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `withdraw_no` VARCHAR(32) NOT NULL COMMENT '提现单号',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户号',
  `merchant_name` VARCHAR(128) DEFAULT NULL COMMENT '商户名称',
  `withdraw_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '提现金额(分)',
  `actual_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实际到账金额(分)',
  `fee_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '手续费金额(分)',
  `withdraw_status` TINYINT NOT NULL DEFAULT 0 COMMENT '提现状态：0待审核 1审核通过 2审核拒绝 3转账中 4提现成功 5提现失败',
  `bank_name` VARCHAR(64) DEFAULT NULL COMMENT '开户银行',
  `bank_account` VARCHAR(64) DEFAULT NULL COMMENT '银行账号',
  `account_name` VARCHAR(128) DEFAULT NULL COMMENT '开户名',
  `audit_user` VARCHAR(64) DEFAULT NULL COMMENT '审核人',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `transfer_no` VARCHAR(64) DEFAULT NULL COMMENT '转账单号',
  `transfer_channel` VARCHAR(32) DEFAULT NULL COMMENT '转账通道',
  `transfer_time` DATETIME DEFAULT NULL COMMENT '转账时间',
  `transfer_fail_reason` VARCHAR(512) DEFAULT NULL COMMENT '转账失败原因',
  `transfer_retry_count` INT NOT NULL DEFAULT 0 COMMENT '转账重试次数',
  `next_transfer_retry_time` DATETIME DEFAULT NULL COMMENT '下次转账重试时间',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_withdraw_no` (`withdraw_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_withdraw_status` (`withdraw_status`),
  KEY `idx_next_transfer_retry` (`next_transfer_retry_time`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理佣金提现表';

-- -----------------------------------------------
-- 28. 发票主表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_invoice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `invoice_no` VARCHAR(64) NOT NULL COMMENT '发票号(唯一)',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `order_no` VARCHAR(64) NOT NULL COMMENT '关联订单号',
  `channel_invoice_no` VARCHAR(128) DEFAULT NULL COMMENT '渠道发票号',
  `channel_code` VARCHAR(32) NOT NULL COMMENT '开票渠道: NUONUO/BAIWANG',
  `invoice_type` TINYINT NOT NULL DEFAULT 1 COMMENT '发票类型: 1蓝票 2红票',
  `invoice_status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待开票 1开票中 2成功 3失败 10待红冲 11红冲中 12红冲成功 13红冲失败',
  `title_type` TINYINT NOT NULL DEFAULT 1 COMMENT '抬头类型: 1个人 2企业',
  `buyer_title` VARCHAR(256) NOT NULL COMMENT '购方抬头',
  `buyer_tax_no` VARCHAR(64) DEFAULT NULL COMMENT '购方税号',
  `buyer_address` VARCHAR(256) DEFAULT NULL COMMENT '购方地址',
  `buyer_bank_name` VARCHAR(128) DEFAULT NULL COMMENT '购方开户行',
  `buyer_bank_account` VARCHAR(64) DEFAULT NULL COMMENT '购方银行账号',
  `buyer_phone` VARCHAR(20) DEFAULT NULL COMMENT '购方手机号',
  `buyer_email` VARCHAR(128) DEFAULT NULL COMMENT '购方邮箱',
  `invoice_content` VARCHAR(256) DEFAULT NULL COMMENT '发票内容',
  `invoice_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '开票金额(元)',
  `tax_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '税额(元)',
  `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '价税合计(元)',
  `tax_rate` VARCHAR(16) DEFAULT '6%' COMMENT '税率',
  `pdf_url` VARCHAR(512) DEFAULT NULL COMMENT '发票PDF下载地址',
  `original_invoice_no` VARCHAR(64) DEFAULT NULL COMMENT '原发票号(红票必填)',
  `red_reason` VARCHAR(512) DEFAULT NULL COMMENT '红冲原因',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `fail_reason` VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
  `notify_url` VARCHAR(512) DEFAULT NULL COMMENT '商户回调通知地址',
  `issue_time` DATETIME DEFAULT NULL COMMENT '开票完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoice_no` (`invoice_no`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_channel_invoice_no` (`channel_invoice_no`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_invoice_type` (`invoice_type`),
  KEY `idx_invoice_status` (`invoice_status`),
  KEY `idx_original_invoice_no` (`original_invoice_no`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_issue_time` (`issue_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票主表';

-- -----------------------------------------------
-- 29. 发票明细分项表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_invoice_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `invoice_id` BIGINT NOT NULL COMMENT '发票主表ID',
  `invoice_no` VARCHAR(64) NOT NULL COMMENT '发票号',
  `item_name` VARCHAR(256) NOT NULL COMMENT '商品名称',
  `item_code` VARCHAR(64) DEFAULT NULL COMMENT '商品编码',
  `specification` VARCHAR(128) DEFAULT NULL COMMENT '规格型号',
  `unit` VARCHAR(32) DEFAULT NULL COMMENT '单位',
  `quantity` DECIMAL(18,4) DEFAULT NULL COMMENT '数量',
  `unit_price` DECIMAL(18,4) DEFAULT NULL COMMENT '单价',
  `amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额(元)',
  `tax_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '税额(元)',
  `tax_rate` VARCHAR(16) DEFAULT '6%' COMMENT '税率',
  `tax_included_flag` TINYINT DEFAULT 1 COMMENT '是否含税: 0否 1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_invoice_id` (`invoice_id`),
  KEY `idx_invoice_no` (`invoice_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票明细分项表';

-- -----------------------------------------------
-- 30. 发票渠道回调日志表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_invoice_callback_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `invoice_no` VARCHAR(64) DEFAULT NULL COMMENT '发票号',
  `channel_code` VARCHAR(32) DEFAULT NULL COMMENT '开票渠道',
  `channel_invoice_no` VARCHAR(128) DEFAULT NULL COMMENT '渠道发票号',
  `request_body` TEXT DEFAULT NULL COMMENT '回调请求体',
  `response_body` TEXT DEFAULT NULL COMMENT '回调响应体',
  `notify_status` VARCHAR(32) DEFAULT 'RECEIVED' COMMENT '通知状态: RECEIVED/SUCCESS/FAIL',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_invoice_no` (`invoice_no`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票渠道回调日志表';

-- -----------------------------------------------
-- 31. 商户发票渠道配置表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_invoice_channel_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_no` VARCHAR(32) NOT NULL COMMENT '商户编号',
  `channel_code` VARCHAR(32) NOT NULL COMMENT '开票渠道: NUONUO/BAIWANG',
  `app_id` VARCHAR(128) DEFAULT NULL COMMENT '渠道应用ID/AppKey',
  `app_secret` VARCHAR(512) DEFAULT NULL COMMENT '渠道密钥',
  `access_token` VARCHAR(512) DEFAULT NULL COMMENT '访问令牌',
  `tax_num` VARCHAR(64) DEFAULT NULL COMMENT '销方税号',
  `company_name` VARCHAR(256) DEFAULT NULL COMMENT '销方公司名称',
  `company_address` VARCHAR(256) DEFAULT NULL COMMENT '销方公司地址',
  `company_phone` VARCHAR(20) DEFAULT NULL COMMENT '销方公司电话',
  `bank_name` VARCHAR(128) DEFAULT NULL COMMENT '销方开户银行',
  `bank_account` VARCHAR(64) DEFAULT NULL COMMENT '销方银行账号',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0否 1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_channel` (`merchant_no`, `channel_code`),
  KEY `idx_merchant_no` (`merchant_no`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户发票渠道配置表';

-- -----------------------------------------------
-- 初始化发票渠道配置数据(沙箱测试)
-- -----------------------------------------------
INSERT IGNORE INTO `pay_invoice_channel_config`
(`merchant_no`, `channel_code`, `app_id`, `app_secret`, `access_token`,
 `tax_num`, `company_name`, `enabled`, `created_at`, `updated_at`)
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
