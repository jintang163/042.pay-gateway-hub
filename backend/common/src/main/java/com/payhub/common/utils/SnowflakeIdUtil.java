package com.payhub.common.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SnowflakeIdUtil {

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    public static String nextIdStr() {
        return String.valueOf(SNOWFLAKE.nextId());
    }

    public static String generateMerchantNo() {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        int seq = SEQUENCE.incrementAndGet() % 10000;
        return "M" + dateStr + String.format("%04d", seq);
    }

    public static String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        return "P" + dateStr + nextIdStr().substring(8);
    }

    public static String generateRefundNo() {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        return "R" + dateStr + nextIdStr().substring(8);
    }

    public static String generateSettlementNo() {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        return "S" + dateStr + nextIdStr().substring(8);
    }
}
