package com.payhub.common.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class HttpUtil {

    private static final int TIMEOUT = 30000;

    public static String get(String url) {
        try {
            HttpResponse response = HttpRequest.get(url)
                    .timeout(TIMEOUT)
                    .execute();
            return response.body();
        } catch (Exception e) {
            log.error("HTTP GET请求失败: url={}", url, e);
            return null;
        }
    }

    public static String get(String url, Map<String, String> headers) {
        try {
            HttpResponse response = HttpRequest.get(url)
                    .addHeaders(headers)
                    .timeout(TIMEOUT)
                    .execute();
            return response.body();
        } catch (Exception e) {
            log.error("HTTP GET请求失败: url={}", url, e);
            return null;
        }
    }

    public static String post(String url, Map<String, Object> params) {
        try {
            HttpResponse response = HttpRequest.post(url)
                    .form(params)
                    .timeout(TIMEOUT)
                    .execute();
            return response.body();
        } catch (Exception e) {
            log.error("HTTP POST表单请求失败: url={}", url, e);
            return null;
        }
    }

    public static String postJson(String url, Object body) {
        try {
            HttpResponse response = HttpRequest.post(url)
                    .body(JSON.toJSONString(body), "application/json;charset=UTF-8")
                    .timeout(TIMEOUT)
                    .execute();
            return response.body();
        } catch (Exception e) {
            log.error("HTTP POST JSON请求失败: url={}", url, e);
            return null;
        }
    }

    public static String postJson(String url, Object body, Map<String, String> headers) {
        try {
            HttpResponse response = HttpRequest.post(url)
                    .addHeaders(headers)
                    .body(JSON.toJSONString(body), "application/json;charset=UTF-8")
                    .timeout(TIMEOUT)
                    .execute();
            return response.body();
        } catch (Exception e) {
            log.error("HTTP POST JSON请求失败: url={}", url, e);
            return null;
        }
    }

    public static JSONObject postJsonForResult(String url, Object body) {
        String resp = postJson(url, body);
        if (resp == null) {
            return null;
        }
        try {
            return JSON.parseObject(resp);
        } catch (Exception e) {
            log.error("解析响应JSON失败: resp={}", resp, e);
            return null;
        }
    }

    public static String request(Method method, String url, Map<String, String> headers, String body) {
        try {
            HttpResponse response = HttpRequest.of(url)
                    .method(method)
                    .addHeaders(headers)
                    .body(body)
                    .timeout(TIMEOUT)
                    .execute();
            return response.body();
        } catch (Exception e) {
            log.error("HTTP请求失败: method={}, url={}", method, url, e);
            return null;
        }
    }
}
