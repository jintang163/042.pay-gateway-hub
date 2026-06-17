package com.payhub.common.crypto;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class RsaUtil {

    public static final String KEY_ALGORITHM = "RSA";

    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    public static final int DEFAULT_KEY_SIZE = 2048;

    private RsaUtil() {
    }

    public static KeyPair generateKeyPair() {
        return generateKeyPair(DEFAULT_KEY_SIZE);
    }

    public static KeyPair generateKeyPair(int keySize) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGenerator.initialize(keySize, new SecureRandom());
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            log.error("RSA生成密钥对失败", e);
            throw new RuntimeException("RSA生成密钥对失败", e);
        }
    }

    public static String getPublicKeyBase64(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public static String getPrivateKeyBase64(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    public static String encrypt(String data, String publicKeyBase64) {
        try {
            PublicKey publicKey = parsePublicKey(publicKeyBase64);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("RSA加密失败", e);
            throw new RuntimeException("RSA加密失败", e);
        }
    }

    public static String decrypt(String data, String privateKeyBase64) {
        try {
            PrivateKey privateKey = parsePrivateKey(privateKeyBase64);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(data));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("RSA解密失败", e);
            throw new RuntimeException("RSA解密失败", e);
        }
    }

    public static String sign(String data, String privateKeyBase64) {
        try {
            PrivateKey privateKey = parsePrivateKey(privateKeyBase64);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signed = signature.sign();
            return Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            log.error("RSA签名失败", e);
            throw new RuntimeException("RSA签名失败", e);
        }
    }

    public static boolean verify(String data, String sign, String publicKeyBase64) {
        try {
            PublicKey publicKey = parsePublicKey(publicKeyBase64);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            log.error("RSA验签失败", e);
            return false;
        }
    }

    public static PublicKey parsePublicKey(String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            log.error("解析RSA公钥失败", e);
            throw new RuntimeException("解析RSA公钥失败", e);
        }
    }

    public static PrivateKey parsePrivateKey(String privateKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            log.error("解析RSA私钥失败", e);
            throw new RuntimeException("解析RSA私钥失败", e);
        }
    }
}
