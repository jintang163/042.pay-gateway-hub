package com.payhub.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class OrderNoGenerator {

    private static final String PREFIX = "P";

    private static final String REFUND_PREFIX = "R";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final int RANDOM_BOUND = 1000000;

    private OrderNoGenerator() {
    }

    public static String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(RANDOM_BOUND);
        return PREFIX + timestamp + String.format("%06d", random);
    }

    public static String generateWithPrefix(String prefix) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(RANDOM_BOUND);
        return prefix + timestamp + String.format("%06d", random);
    }

    public static String generateRefund() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(RANDOM_BOUND);
        return REFUND_PREFIX + timestamp + String.format("%06d", random);
    }
}
