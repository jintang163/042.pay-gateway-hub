package com.payhub.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class MerchantNoGenerator {

    private static final String PREFIX = "M";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final int RANDOM_BOUND = 10000;

    private MerchantNoGenerator() {
    }

    public static String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(RANDOM_BOUND);
        return PREFIX + timestamp + String.format("%04d", random);
    }
}
