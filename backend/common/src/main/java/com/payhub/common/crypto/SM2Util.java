package com.payhub.common.crypto;

import cn.hutool.core.util.HexUtil;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class SM2Util {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String CURVE_NAME = "sm2p256v1";

    private static final X9ECParameters X9_EC_PARAMETERS = GMNamedCurves.getByName(CURVE_NAME);

    private static final ECDomainParameters EC_DOMAIN_PARAMETERS = new ECDomainParameters(
            X9_EC_PARAMETERS.getCurve(),
            X9_EC_PARAMETERS.getG(),
            X9_EC_PARAMETERS.getN(),
            X9_EC_PARAMETERS.getH()
    );

    private SM2Util() {
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
            ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(CURVE_NAME);
            kpg.initialize(ecGenParameterSpec, new SecureRandom());
            return kpg.generateKeyPair();
        } catch (Exception e) {
            log.error("SM2生成密钥对失败", e);
            throw new RuntimeException("SM2生成密钥对失败", e);
        }
    }

    public static AsymmetricCipherKeyPair generateKeyPairParams() {
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        ECDomainParameters domainParameters = new ECDomainParameters(
                X9_EC_PARAMETERS.getCurve(),
                X9_EC_PARAMETERS.getG(),
                X9_EC_PARAMETERS.getN()
        );
        generator.init(new ECKeyGenerationParameters(domainParameters, new SecureRandom()));
        return generator.generateKeyPair();
    }

    public static String getPublicKeyHex(KeyPair keyPair) {
        BCECPublicKey publicKey = (BCECPublicKey) keyPair.getPublic();
        byte[] bytes = publicKey.getQ().getEncoded(false);
        return HexUtil.encodeHexStr(bytes);
    }

    public static String getPrivateKeyHex(KeyPair keyPair) {
        BCECPrivateKey privateKey = (BCECPrivateKey) keyPair.getPrivate();
        return HexUtil.encodeHexStr(privateKey.getD().toByteArray());
    }

    public static String encrypt(String plainText, String publicKeyHex) {
        try {
            byte[] publicKeyBytes = HexUtil.decodeHex(publicKeyHex);
            ECPoint ecPoint = X9_EC_PARAMETERS.getCurve().decodePoint(publicKeyBytes);
            ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(ecPoint, EC_DOMAIN_PARAMETERS);

            SM2Engine sm2Engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            sm2Engine.init(true, new ParametersWithRandom(publicKeyParameters, new SecureRandom()));

            byte[] input = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] output = sm2Engine.processBlock(input, 0, input.length);
            return Base64.getEncoder().encodeToString(output);
        } catch (InvalidCipherTextException e) {
            log.error("SM2加密失败", e);
            throw new RuntimeException("SM2加密失败", e);
        }
    }

    public static String decrypt(String cipherText, String privateKeyHex) {
        try {
            byte[] privateKeyBytes = HexUtil.decodeHex(privateKeyHex);
            BigInteger privateKeyD = new BigInteger(1, privateKeyBytes);
            ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKeyD, EC_DOMAIN_PARAMETERS);

            SM2Engine sm2Engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            sm2Engine.init(false, privateKeyParameters);

            byte[] input = Base64.getDecoder().decode(cipherText);
            byte[] output = sm2Engine.processBlock(input, 0, input.length);
            return new String(output, StandardCharsets.UTF_8);
        } catch (InvalidCipherTextException e) {
            log.error("SM2解密失败", e);
            throw new RuntimeException("SM2解密失败", e);
        }
    }

    public static String sign(String data, String privateKeyHex, String userId) {
        try {
            byte[] privateKeyBytes = HexUtil.decodeHex(privateKeyHex);
            BigInteger privateKeyD = new BigInteger(1, privateKeyBytes);
            ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKeyD, EC_DOMAIN_PARAMETERS);

            SM2Signer signer = new SM2Signer();
            if (userId != null && !userId.isEmpty()) {
                signer.init(true, new ParametersWithID(privateKeyParameters, userId.getBytes(StandardCharsets.UTF_8)));
            } else {
                signer.init(true, privateKeyParameters);
            }

            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            signer.update(dataBytes, 0, dataBytes.length);
            byte[] signature = signer.generateSignature();
            return Base64.getEncoder().encodeToString(signature);
        } catch (CryptoException e) {
            log.error("SM2签名失败", e);
            throw new RuntimeException("SM2签名失败", e);
        }
    }

    public static String sign(String data, String privateKeyHex) {
        return sign(data, privateKeyHex, null);
    }

    public static boolean verify(String data, String signature, String publicKeyHex, String userId) {
        try {
            byte[] publicKeyBytes = HexUtil.decodeHex(publicKeyHex);
            ECPoint ecPoint = X9_EC_PARAMETERS.getCurve().decodePoint(publicKeyBytes);
            ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(ecPoint, EC_DOMAIN_PARAMETERS);

            SM2Signer signer = new SM2Signer();
            if (userId != null && !userId.isEmpty()) {
                signer.init(false, new ParametersWithID(publicKeyParameters, userId.getBytes(StandardCharsets.UTF_8)));
            } else {
                signer.init(false, publicKeyParameters);
            }

            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            signer.update(dataBytes, 0, dataBytes.length);
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return signer.verifySignature(signatureBytes);
        } catch (Exception e) {
            log.error("SM2验签失败", e);
            return false;
        }
    }

    public static boolean verify(String data, String signature, String publicKeyHex) {
        return verify(data, signature, publicKeyHex, null);
    }

    public static PublicKey parsePublicKey(String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("EC", "BC");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            log.error("解析SM2公钥失败", e);
            throw new RuntimeException("解析SM2公钥失败", e);
        }
    }

    public static PrivateKey parsePrivateKey(String privateKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("EC", "BC");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            log.error("解析SM2私钥失败", e);
            throw new RuntimeException("解析SM2私钥失败", e);
        }
    }
}
