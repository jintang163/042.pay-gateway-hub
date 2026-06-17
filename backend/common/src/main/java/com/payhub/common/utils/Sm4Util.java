package com.payhub.common.utils;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SM4;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

@Component
public class Sm4Util {

    private static final String DEFAULT_KEY = "PAYHUB_SM4_KEY_16";
    private static final String DEFAULT_IV = "PAYHUB_SM4_IV__16";

    @Value("${payhub.crypto.sm4.key:PAYHUB_SM4_KEY_16}")
    private String sm4Key;

    @Value("${payhub.crypto.sm4.iv:PAYHUB_SM4_IV__16}")
    private String sm4Iv;

    private static String staticSm4Key;
    private static String staticSm4Iv;

    @PostConstruct
    public void init() {
        staticSm4Key = sm4Key;
        staticSm4Iv = sm4Iv;
    }

    private static SM4 getSm4() {
        byte[] key = SecureUtil.generateKey("SM4", 128, staticSm4Key.getBytes(StandardCharsets.UTF_8)).getEncoded();
        byte[] iv = staticSm4Iv.getBytes(StandardCharsets.UTF_8);
        return new SM4(Mode.CBC, Padding.PKCS5Padding, key, iv);
    }

    public static String encrypt(String data) {
        if (data == null) {
            return null;
        }
        return getSm4().encryptHex(data);
    }

    public static String decrypt(String data) {
        if (data == null) {
            return null;
        }
        return getSm4().decryptStr(data);
    }
}
