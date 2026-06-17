package com.payhub.common.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

@Slf4j
public class SM4Util {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static final String ALGORITHM_NAME = "SM4";

    public static final String ALGORITHM_NAME_ECB_PADDING = "SM4/ECB/PKCS7Padding";

    public static final String ALGORITHM_NAME_CBC_PADDING = "SM4/CBC/PKCS7Padding";

    public static final int DEFAULT_KEY_SIZE = 128;

    private SM4Util() {
    }

    public static String generateKey() {
        return generateKey(DEFAULT_KEY_SIZE);
    }

    public static String generateKey(int keySize) {
        try {
            SecureRandom secureRandom = new SecureRandom();
            byte[] key = new byte[keySize / 8];
            secureRandom.nextBytes(key);
            return Base64.getEncoder().encodeToString(key);
        } catch (Exception e) {
            log.error("SM4生成密钥失败", e);
            throw new RuntimeException("SM4生成密钥失败", e);
        }
    }

    public static String generateIv() {
        try {
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[16];
            secureRandom.nextBytes(iv);
            return Base64.getEncoder().encodeToString(iv);
        } catch (Exception e) {
            log.error("SM4生成IV失败", e);
            throw new RuntimeException("SM4生成IV失败", e);
        }
    }

    public static String encryptEcb(String plainText, String keyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM_NAME);
            Cipher cipher = Cipher.getInstance(ALGORITHM_NAME_ECB_PADDING, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] output = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(output);
        } catch (Exception e) {
            log.error("SM4 ECB加密失败", e);
            throw new RuntimeException("SM4 ECB加密失败", e);
        }
    }

    public static String decryptEcb(String cipherText, String keyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM_NAME);
            Cipher cipher = Cipher.getInstance(ALGORITHM_NAME_ECB_PADDING, "BC");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] output = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(output, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("SM4 ECB解密失败", e);
            throw new RuntimeException("SM4 ECB解密失败", e);
        }
    }

    public static String encryptCbc(String plainText, String keyBase64, String ivBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM_NAME);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(ALGORITHM_NAME_CBC_PADDING, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] output = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(output);
        } catch (Exception e) {
            log.error("SM4 CBC加密失败", e);
            throw new RuntimeException("SM4 CBC加密失败", e);
        }
    }

    public static String decryptCbc(String cipherText, String keyBase64, String ivBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM_NAME);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(ALGORITHM_NAME_CBC_PADDING, "BC");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] output = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(output, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("SM4 CBC解密失败", e);
            throw new RuntimeException("SM4 CBC解密失败", e);
        }
    }

    public static byte[] encryptEcbBytes(byte[] plainBytes, byte[] keyBytes) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM_NAME);
            Cipher cipher = Cipher.getInstance(ALGORITHM_NAME_ECB_PADDING, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(plainBytes);
        } catch (Exception e) {
            log.error("SM4 ECB加密字节失败", e);
            throw new RuntimeException("SM4 ECB加密字节失败", e);
        }
    }

    public static byte[] decryptEcbBytes(byte[] cipherBytes, byte[] keyBytes) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM_NAME);
            Cipher cipher = Cipher.getInstance(ALGORITHM_NAME_ECB_PADDING, "BC");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            log.error("SM4 ECB解密字节失败", e);
            throw new RuntimeException("SM4 ECB解密字节失败", e);
        }
    }
}
