package com.payhub.common.utils;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.SM2;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
public class SignUtil {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static String generateMd5Key() {
        return RandomUtil.randomString(32);
    }

    public static RsaKeyPair generateRsaKeyPair() {
        RSA rsa = SecureUtil.rsa();
        RsaKeyPair pair = new RsaKeyPair();
        pair.setPublicKey(Base64.getEncoder().encodeToString(rsa.getPublicKey().getEncoded()));
        pair.setPrivateKey(Base64.getEncoder().encodeToString(rsa.getPrivateKey().getEncoded()));
        return pair;
    }

    public static Sm2KeyPair generateSm2KeyPair() {
        SM2 sm2 = SecureUtil.sm2();
        Sm2KeyPair pair = new Sm2KeyPair();
        pair.setPublicKey(HexUtil.encodeHexStr(sm2.getPublicKey().getEncoded()));
        pair.setPrivateKey(HexUtil.encodeHexStr(sm2.getPrivateKey().getEncoded()));
        return pair;
    }

    public static String signMd5(Map<String, Object> params, String md5Key) {
        TreeMap<String, Object> sortedMap = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            if (StrUtil.isNotBlank(String.valueOf(entry.getValue()))
                    && !"sign".equals(entry.getKey())
                    && !"signType".equals(entry.getKey())) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        sb.append("key=").append(md5Key);
        return SecureUtil.md5(sb.toString()).toUpperCase();
    }

    public static boolean verifyMd5(Map<String, Object> params, String md5Key, String sign) {
        String calculatedSign = signMd5(params, md5Key);
        return calculatedSign.equalsIgnoreCase(sign);
    }

    public static String signRsa(Map<String, Object> params, String privateKey) {
        TreeMap<String, Object> sortedMap = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            if (StrUtil.isNotBlank(String.valueOf(entry.getValue()))
                    && !"sign".equals(entry.getKey())
                    && !"signType".equals(entry.getKey())) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        RSA rsa = SecureUtil.rsa(privateKey, null);
        return Base64.getEncoder().encodeToString(rsa.sign(sb.toString().getBytes(StandardCharsets.UTF_8)));
    }

    public static boolean verifyRsa(Map<String, Object> params, String publicKey, String sign) {
        try {
            TreeMap<String, Object> sortedMap = new TreeMap<>(params);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
                if (StrUtil.isNotBlank(String.valueOf(entry.getValue()))
                        && !"sign".equals(entry.getKey())
                        && !"signType".equals(entry.getKey())) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            RSA rsa = SecureUtil.rsa(null, publicKey);
            return rsa.verify(sb.toString().getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            log.error("RSA验签失败", e);
            return false;
        }
    }

    public static String signSm2(Map<String, Object> params, String privateKey) {
        TreeMap<String, Object> sortedMap = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            if (StrUtil.isNotBlank(String.valueOf(entry.getValue()))
                    && !"sign".equals(entry.getKey())
                    && !"signType".equals(entry.getKey())) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        SM2 sm2 = SecureUtil.sm2(privateKey, null);
        byte[] signBytes = sm2.sign(sb.toString().getBytes(StandardCharsets.UTF_8));
        return HexUtil.encodeHexStr(signBytes);
    }

    public static boolean verifySm2(Map<String, Object> params, String publicKey, String sign) {
        try {
            TreeMap<String, Object> sortedMap = new TreeMap<>(params);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
                if (StrUtil.isNotBlank(String.valueOf(entry.getValue()))
                        && !"sign".equals(entry.getKey())
                        && !"signType".equals(entry.getKey())) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            SM2 sm2 = SecureUtil.sm2(null, publicKey);
            return sm2.verify(sb.toString().getBytes(StandardCharsets.UTF_8), HexUtil.decodeHex(sign));
        } catch (Exception e) {
            log.error("SM2验签失败", e);
            return false;
        }
    }

    public static String sign(Map<String, Object> params, String signType, String md5Key, String rsaPrivateKey, String sm2PrivateKey) {
        switch (signType.toUpperCase()) {
            case "MD5":
                return signMd5(params, md5Key);
            case "RSA":
                return signRsa(params, rsaPrivateKey);
            case "SM2":
                return signSm2(params, sm2PrivateKey);
            default:
                return signMd5(params, md5Key);
        }
    }

    public static boolean verify(Map<String, Object> params, String signType, String sign,
                                 String md5Key, String rsaPublicKey, String sm2PublicKey) {
        switch (signType.toUpperCase()) {
            case "MD5":
                return verifyMd5(params, md5Key, sign);
            case "RSA":
                return verifyRsa(params, rsaPublicKey, sign);
            case "SM2":
                return verifySm2(params, sm2PublicKey, sign);
            default:
                return verifyMd5(params, md5Key, sign);
        }
    }

    @Data
    public static class RsaKeyPair {
        private String publicKey;
        private String privateKey;
    }

    @Data
    public static class Sm2KeyPair {
        private String publicKey;
        private String privateKey;
    }
}
