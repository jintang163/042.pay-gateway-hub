package com.payhub.common.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSignerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${payhub.jwt.secret:PAYHUB_JWT_SECRET_KEY_2024}")
    private String jwtSecret;

    @Value("${payhub.jwt.expire-hours:24}")
    private Integer expireHours;

    private static String staticJwtSecret;
    private static Integer staticExpireHours;

    @PostConstruct
    public void init() {
        staticJwtSecret = jwtSecret;
        staticExpireHours = expireHours;
    }

    public static String generateToken(String merchantNo, String username) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("jti", IdUtil.fastSimpleUUID());
        payload.put("merchantNo", merchantNo);
        payload.put("username", username);
        payload.put("iat", System.currentTimeMillis());
        payload.put("exp", DateUtil.offsetHour(new Date(), staticExpireHours).getTime());

        return JWT.create()
                .setPayload(payload)
                .setSigner(JWTSignerUtil.hs256(staticJwtSecret.getBytes(StandardCharsets.UTF_8)))
                .sign();
    }

    public static Map<String, Object> parseToken(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            if (!jwt.setKey(staticJwtSecret.getBytes(StandardCharsets.UTF_8)).verify()) {
                return null;
            }
            Object exp = jwt.getPayload("exp");
            if (exp != null && ((Number) exp).longValue() < System.currentTimeMillis()) {
                return null;
            }
            return jwt.getPayloads();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getMerchantNo(String token) {
        Map<String, Object> payload = parseToken(token);
        if (payload == null) {
            return null;
        }
        return (String) payload.get("merchantNo");
    }

    public static String getUsername(String token) {
        Map<String, Object> payload = parseToken(token);
        if (payload == null) {
            return null;
        }
        return (String) payload.get("username");
    }

    public static boolean validateToken(String token) {
        return parseToken(token) != null;
    }
}
