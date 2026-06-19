-- ==============================================
-- 营销模块表（并入主库 pay_gateway_hub / pay_gateway_hub_sandbox）
-- ==============================================

DROP TABLE IF EXISTS `pay_link`;
CREATE TABLE `pay_link` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `link_code`       VARCHAR(32)   NOT NULL COMMENT '链接短码',
    `merchant_no`     VARCHAR(32)   NOT NULL COMMENT '商户号',
    `title`           VARCHAR(100)  NOT NULL COMMENT '链接标题',
    `fixed_amount`    DECIMAL(12,2) DEFAULT NULL COMMENT '固定金额',
    `amount_editable` TINYINT(1)    DEFAULT 0 COMMENT '是否允许自定义金额',
    `min_amount`      DECIMAL(12,2) DEFAULT NULL COMMENT '最低金额',
    `max_amount`      DECIMAL(12,2) DEFAULT NULL COMMENT '最高金额',
    `pay_channel`     VARCHAR(32)   DEFAULT NULL COMMENT '支付渠道',
    `product_subject` VARCHAR(200)  DEFAULT NULL COMMENT '商品描述',
    `product_detail`  VARCHAR(500)  DEFAULT NULL COMMENT '商品详情',
    `notify_url`      VARCHAR(500)  DEFAULT NULL COMMENT '异步通知地址',
    `redirect_url`    VARCHAR(500)  DEFAULT NULL COMMENT '同步跳转地址',
    `expire_time`     DATETIME      DEFAULT NULL COMMENT '过期时间',
    `single_use`      TINYINT(1)    DEFAULT 0 COMMENT '是否单次使用',
    `max_use_count`   INT           DEFAULT NULL COMMENT '最大使用次数',
    `used_count`      INT           DEFAULT 0 COMMENT '已使用次数',
    `status`          INT           DEFAULT 1 COMMENT '状态：1生效 2过期 3禁用 4用尽',
    `remark`          VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    `operator_id`     VARCHAR(32)   DEFAULT NULL COMMENT '操作人ID',
    `operator_name`   VARCHAR(64)   DEFAULT NULL COMMENT '操作人姓名',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         INT           DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_link_code` (`link_code`),
    KEY `idx_merchant_no` (`merchant_no`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付链接表';

DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `coupon_code`       VARCHAR(32)   NOT NULL COMMENT '优惠券编码',
    `merchant_no`       VARCHAR(32)   NOT NULL COMMENT '商户号',
    `coupon_name`       VARCHAR(100)  NOT NULL COMMENT '优惠券名称',
    `coupon_type`       INT           NOT NULL COMMENT '类型：1固定抵扣 2折扣抵扣',
    `discount_value`    DECIMAL(12,2) NOT NULL COMMENT '优惠值：固定金额或折扣率',
    `min_order_amount`  DECIMAL(12,2) DEFAULT NULL COMMENT '最低订单金额',
    `max_discount`      DECIMAL(12,2) DEFAULT NULL COMMENT '最大优惠金额(折扣券)',
    `total_quantity`    INT           NOT NULL COMMENT '发放总量',
    `issued_count`      INT           DEFAULT 0 COMMENT '已发放数量',
    `used_count`        INT           DEFAULT 0 COMMENT '已核销数量',
    `start_time`        DATETIME      DEFAULT NULL COMMENT '生效开始时间',
    `end_time`          DATETIME      DEFAULT NULL COMMENT '生效结束时间',
    `status`            INT           DEFAULT 0 COMMENT '状态：0未开始 1发放中 2暂停 3发完 4过期',
    `remark`            VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    `operator_id`       VARCHAR(32)   DEFAULT NULL COMMENT '操作人ID',
    `operator_name`     VARCHAR(64)   DEFAULT NULL COMMENT '操作人姓名',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           INT           DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_coupon_code` (`coupon_code`),
    KEY `idx_merchant_no` (`merchant_no`),
    KEY `idx_coupon_type` (`coupon_type`),
    KEY `idx_status` (`status`),
    KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

DROP TABLE IF EXISTS `activity`;
CREATE TABLE `activity` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `activity_code`     VARCHAR(32)   NOT NULL COMMENT '活动编码',
    `merchant_no`       VARCHAR(32)   NOT NULL COMMENT '商户号',
    `activity_name`     VARCHAR(100)  NOT NULL COMMENT '活动名称',
    `activity_type`     INT           NOT NULL COMMENT '类型：1满减 2折扣',
    `threshold_amount`  DECIMAL(12,2) DEFAULT NULL COMMENT '门槛金额',
    `discount_amount`   DECIMAL(12,2) DEFAULT NULL COMMENT '减免金额(满减)',
    `discount_rate`     DECIMAL(5,2)  DEFAULT NULL COMMENT '折扣率(折扣活动)',
    `max_discount`      DECIMAL(12,2) DEFAULT NULL COMMENT '最大优惠(折扣活动)',
    `start_time`        DATETIME      DEFAULT NULL COMMENT '活动开始时间',
    `end_time`          DATETIME      DEFAULT NULL COMMENT '活动结束时间',
    `status`            INT           DEFAULT 0 COMMENT '状态：0未开始 1进行中 2暂停 3结束',
    `remark`            VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    `operator_id`       VARCHAR(32)   DEFAULT NULL COMMENT '操作人ID',
    `operator_name`     VARCHAR(64)   DEFAULT NULL COMMENT '操作人姓名',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           INT           DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_code` (`activity_code`),
    KEY `idx_merchant_no` (`merchant_no`),
    KEY `idx_activity_type` (`activity_type`),
    KEY `idx_status` (`status`),
    KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

DROP TABLE IF EXISTS `coupon_use_log`;
CREATE TABLE `coupon_use_log` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `coupon_code`     VARCHAR(32)   NOT NULL COMMENT '优惠券编码',
    `merchant_no`     VARCHAR(32)   NOT NULL COMMENT '商户号',
    `order_no`        VARCHAR(64)   DEFAULT NULL COMMENT '关联订单号',
    `user_id`         VARCHAR(64)   DEFAULT NULL COMMENT '用户标识',
    `order_amount`    DECIMAL(12,2) DEFAULT NULL COMMENT '订单金额',
    `discount_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '优惠金额',
    `use_type`        INT           DEFAULT NULL COMMENT '使用类型：1核销 2锁定 3释放',
    `used_at`         DATETIME      DEFAULT NULL COMMENT '使用时间',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_coupon_code` (`coupon_code`),
    KEY `idx_merchant_no` (`merchant_no`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券使用记录';

-- ==============================================
-- 给订单表增加营销字段
-- ==============================================
-- 注意：实际部署时请用 ALTER TABLE 增量添加，这里仅为初始化脚本参考
-- ALTER TABLE pay_order ADD COLUMN link_code VARCHAR(32) DEFAULT NULL COMMENT '来源支付链接' AFTER merchant_order_no;
-- ALTER TABLE pay_order ADD COLUMN coupon_code VARCHAR(32) DEFAULT NULL COMMENT '使用的优惠券编码' AFTER link_code;
-- ALTER TABLE pay_order ADD COLUMN activity_code VARCHAR(32) DEFAULT NULL COMMENT '参与的活动编码' AFTER coupon_code;
-- ALTER TABLE pay_order ADD COLUMN coupon_discount DECIMAL(12,2) DEFAULT 0 COMMENT '优惠券优惠金额' AFTER activity_code;
-- ALTER TABLE pay_order ADD COLUMN activity_discount DECIMAL(12,2) DEFAULT 0 COMMENT '活动优惠金额' AFTER coupon_discount;

-- ==============================================
-- 营销模块初始化数据（沙箱）
-- ==============================================
INSERT INTO `pay_link` (link_code, merchant_no, title, fixed_amount, amount_editable, single_use, status, remark, created_at, updated_at)
VALUES
('LK000001', 'M000001', '会员年费支付', 299.00, 0, 0, 1, '沙箱示例-固定金额链接', NOW(), NOW()),
('LK000002', 'M000001', '自助捐赠通道', NULL, 1, 0, 1, '沙箱示例-自定义金额链接', NOW(), NOW()),
('LK000003', 'M000001', '一次性缴费链接', 99.00, 0, 1, 1, '沙箱示例-单次使用链接', NOW(), NOW());

INSERT INTO `coupon` (coupon_code, merchant_no, coupon_name, coupon_type, discount_value, min_order_amount, max_discount, total_quantity, issued_count, used_count, start_time, end_time, status, remark, created_at, updated_at)
VALUES
('CP000001', 'M000001', '新人立减10元', 1, 10.00, 50.00, NULL, 1000, 100, 23, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '沙箱示例-固定抵扣券', NOW(), NOW()),
('CP000002', 'M000001', '85折优惠券', 2, 8.50, 100.00, 50.00, 500, 50, 12, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '沙箱示例-折扣券', NOW(), NOW()),
('CP000003', 'M000001', '满200减30', 1, 30.00, 200.00, NULL, 200, 0, 0, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), 0, '沙箱示例-未开始', NOW(), NOW());

INSERT INTO `activity` (activity_code, merchant_no, activity_name, activity_type, threshold_amount, discount_amount, discount_rate, max_discount, start_time, end_time, status, remark, created_at, updated_at)
VALUES
('ACT000001', 'M000001', '春季满减活动', 1, 100.00, 10.00, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '沙箱示例-满100减10', NOW(), NOW()),
('ACT000002', 'M000001', '夏季折扣季', 2, 200.00, NULL, 8.00, 100.00, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), 1, '沙箱示例-8折封顶100', NOW(), NOW());
