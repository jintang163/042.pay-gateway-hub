package com.payhub.channel.client;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class SignUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private SignUtil() {
    }

    public static String buildSignContent(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        List<String> keys = new ArrayList<>(sortedParams.keySet());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = sortedParams.get(key);
            if (value != null && !"sign".equalsIgnoreCase(key) && !"signature".equalsIgnoreCase(key)) {
                sb.append(key).append("=").append(value);
                if (i < keys.size() - 1) {
                    sb.append("&");
                }
            }
        }
        return sb.toString();
    }

    public static String alipaySign(Map<String, String> params, String privateKey) {
        String signContent = buildSignContent(params);
        return signRSA2(signContent, privateKey);
    }

    public static boolean alipayVerify(Map<String, String> params, String publicKey) {
        String sign = params.get("sign");
        if (sign == null) {
            return false;
        }
        Map<String, String> paramsWithoutSign = new TreeMap<>(params);
        paramsWithoutSign.remove("sign");
        paramsWithoutSign.remove("sign_type");
        String signContent = buildSignContent(paramsWithoutSign);
        return verifyRSA2(signContent, sign, publicKey);
    }

    public static String wechatSign(Map<String, String> params, String apiKey) {
        String signContent = buildSignContent(params) + "&key=" + apiKey;
        return DigestUtils.md5Hex(signContent).toUpperCase();
    }

    public static String wechatSignV3(Map<String, String> params, String apiKey) {
        String signContent = buildSignContent(params) + "&key=" + apiKey;
        return DigestUtils.sha256Hex(signContent).toUpperCase();
    }

    public static boolean wechatVerify(Map<String, String> params, String apiKey) {
        String sign = params.get("sign");
        if (sign == null) {
            return false;
        }
        Map<String, String> paramsWithoutSign = new TreeMap<>(params);
        paramsWithoutSign.remove("sign");
        String expectedSign = wechatSign(paramsWithoutSign, apiKey);
        return sign.equalsIgnoreCase(expectedSign);
    }

    public static String unionPaySign(Map<String, String> params, String privateKey) {
        String signContent = buildSignContent(params);
        return signSM2(signContent, privateKey);
    }

    public static boolean unionPayVerify(Map<String, String> params, String publicKey) {
        String sign = params.get("signature");
        if (sign == null) {
            return false;
        }
        Map<String, String> paramsWithoutSign = new TreeMap<>(params);
        paramsWithoutSign.remove("signature");
        String signContent = buildSignContent(paramsWithoutSign);
        return verifySM2(signContent, sign, publicKey);
    }

    public static String signRSA2(String content, String privateKeyStr) {
        try {
            byte[] keyBytes = Base64.decodeBase64(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            Signature signature = Signature.getInstance("SHA256WithRSA");
            signature.initSign(privateKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64String(signature.sign());
        } catch (Exception e) {
            log.error("RSA2签名失败", e);
            throw new RuntimeException("RSA2签名失败: " + e.getMessage());
        }
    }

    public static boolean verifyRSA2(String content, String sign, String publicKeyStr) {
        try {
            byte[] keyBytes = Base64.decodeBase64(publicKeyStr);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance("SHA256WithRSA");
            signature.initVerify(publicKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.decodeBase64(sign));
        } catch (Exception e) {
            log.error("RSA2验签失败", e);
            return false;
        }
    }

    public static String signHmacSHA256(String content, String key) {
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, key.getBytes(StandardCharsets.UTF_8));
        return hMac.digestHex(content);
    }

    public static boolean verifyHmacSHA256(String content, String sign, String key) {
        String expectedSign = signHmacSHA256(content, key);
        return expectedSign.equalsIgnoreCase(sign);
    }

    public static String signSM2(String content, String privateKeyStr) {
        try {
            byte[] privateKeyBytes = Base64.decodeBase64(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            BCECPrivateKey privateKey = (BCECPrivateKey) keyFactory.generatePrivate(keySpec);

            ECPrivateKeyParameters ecPrivateKeyParameters = new ECPrivateKeyParameters(
                    privateKey.getD(),
                    privateKey.getParameters()
            );

            SM2Signer signer = new SM2Signer();
            signer.init(true, ecPrivateKeyParameters);
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            signer.update(contentBytes, 0, contentBytes.length);
            byte[] signature = signer.generateSignature();
            return Base64.encodeBase64String(signature);
        } catch (Exception e) {
            log.error("SM2签名失败", e);
            throw new RuntimeException("SM2签名失败: " + e.getMessage());
        }
    }

    public static boolean verifySM2(String content, String sign, String publicKeyStr) {
        try {
            byte[] publicKeyBytes = Base64.decodeBase64(publicKeyStr);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            BCECPublicKey publicKey = (BCECPublicKey) keyFactory.generatePublic(keySpec);

            ECPublicKeyParameters ecPublicKeyParameters = new ECPublicKeyParameters(
                    publicKey.getQ(),
                    publicKey.getParameters()
            );

            SM2Signer signer = new SM2Signer();
            signer.init(false, ecPublicKeyParameters);
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            signer.update(contentBytes, 0, contentBytes.length);
            byte[] signatureBytes = Base64.decodeBase64(sign);
            return signer.verifySignature(signatureBytes);
        } catch (Exception e) {
            log.error("SM2验签失败", e);
            return false;
        }
    }

    public static String generateRandomSignKey() {
        return SecureUtil.simpleUUID().replace("-", "");
    }
}
