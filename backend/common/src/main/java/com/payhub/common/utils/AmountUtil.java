package com.payhub.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AmountUtil {

    private static final int SCALE = 2;

    private static final BigDecimal HUNDRED = new BigDecimal(100);

    private AmountUtil() {
    }

    public static Long yuanToFen(BigDecimal yuan) {
        if (yuan == null) {
            return null;
        }
        return yuan.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    public static Long yuanToFen(String yuan) {
        if (yuan == null || yuan.isEmpty()) {
            return null;
        }
        return yuanToFen(new BigDecimal(yuan));
    }

    public static Long yuanToFen(double yuan) {
        return yuanToFen(BigDecimal.valueOf(yuan));
    }

    public static BigDecimal fenToYuan(Long fen) {
        if (fen == null) {
            return null;
        }
        return new BigDecimal(fen).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal fenToYuan(Integer fen) {
        if (fen == null) {
            return null;
        }
        return new BigDecimal(fen).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    }

    public static String fenToYuanStr(Long fen) {
        BigDecimal yuan = fenToYuan(fen);
        return yuan == null ? null : yuan.toPlainString();
    }

    public static String formatYuan(BigDecimal yuan) {
        if (yuan == null) {
            return null;
        }
        return yuan.setScale(SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    public static BigDecimal add(BigDecimal b1, BigDecimal b2) {
        if (b1 == null) {
            b1 = BigDecimal.ZERO;
        }
        if (b2 == null) {
            b2 = BigDecimal.ZERO;
        }
        return b1.add(b2);
    }

    public static BigDecimal subtract(BigDecimal b1, BigDecimal b2) {
        if (b1 == null) {
            b1 = BigDecimal.ZERO;
        }
        if (b2 == null) {
            b2 = BigDecimal.ZERO;
        }
        return b1.subtract(b2);
    }

    public static boolean isZero(BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNegative(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public static boolean greaterThan(BigDecimal b1, BigDecimal b2) {
        if (b1 == null) {
            b1 = BigDecimal.ZERO;
        }
        if (b2 == null) {
            b2 = BigDecimal.ZERO;
        }
        return b1.compareTo(b2) > 0;
    }

    public static boolean lessThan(BigDecimal b1, BigDecimal b2) {
        if (b1 == null) {
            b1 = BigDecimal.ZERO;
        }
        if (b2 == null) {
            b2 = BigDecimal.ZERO;
        }
        return b1.compareTo(b2) < 0;
    }
}
