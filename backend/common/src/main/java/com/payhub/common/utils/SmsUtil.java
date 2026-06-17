package com.payhub.common.utils;

import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsUtil {

    public static String generateSmsCode() {
        return RandomUtil.randomNumbers(6);
    }

    public static boolean sendSms(String phone, String code) {
        log.info("发送短信验证码: phone={}, code={}", phone, code);
        return true;
    }

    public static boolean verifySmsCode(String phone, String code, String cachedCode) {
        return code != null && code.equals(cachedCode);
    }
}
