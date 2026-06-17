package com.payhub.channel.client;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpUtil {

    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 30;
    private static final int WRITE_TIMEOUT = 30;
    private static final int MAX_RETRY = 3;

    private static final OkHttpClient CLIENT;

    static {
        CLIENT = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    private HttpUtil() {
    }

    public static String get(String url) {
        return get(url, null, MAX_RETRY);
    }

    public static String get(String url, Map<String, String> headers) {
        return get(url, headers, MAX_RETRY);
    }

    public static String get(String url, Map<String, String> headers, int retryCount) {
        Request.Builder builder = new Request.Builder().url(url).get();
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        Request request = builder.build();
        return executeWithRetry(request, retryCount);
    }

    public static String postForm(String url, Map<String, String> params) {
        return postForm(url, params, null, MAX_RETRY);
    }

    public static String postForm(String url, Map<String, String> params, Map<String, String> headers) {
        return postForm(url, params, headers, MAX_RETRY);
    }

    public static String postForm(String url, Map<String, String> params, Map<String, String> headers, int retryCount) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (params != null) {
            params.forEach((key, value) -> {
                if (StrUtil.isNotBlank(key) && value != null) {
                    formBuilder.add(key, value);
                }
            });
        }
        RequestBody body = formBuilder.build();
        Request.Builder builder = new Request.Builder().url(url).post(body);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        Request request = builder.build();
        return executeWithRetry(request, retryCount);
    }

    public static String postJson(String url, String json) {
        return postJson(url, json, null, MAX_RETRY);
    }

    public static String postJson(String url, String json, Map<String, String> headers) {
        return postJson(url, json, headers, MAX_RETRY);
    }

    public static String postJson(String url, String json, Map<String, String> headers, int retryCount) {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, mediaType);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        Request request = builder.build();
        return executeWithRetry(request, retryCount);
    }

    public static String postXml(String url, String xml) {
        return postXml(url, xml, null, MAX_RETRY);
    }

    public static String postXml(String url, String xml, Map<String, String> headers) {
        return postXml(url, xml, headers, MAX_RETRY);
    }

    public static String postXml(String url, String xml, Map<String, String> headers, int retryCount) {
        MediaType mediaType = MediaType.parse("text/xml; charset=utf-8");
        RequestBody body = RequestBody.create(xml, mediaType);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        Request request = builder.build();
        return executeWithRetry(request, retryCount);
    }

    public static String buildUrlParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                if (!first) {
                    sb.append("&");
                }
                try {
                    sb.append(entry.getKey())
                            .append("=")
                            .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
                } catch (Exception e) {
                    log.error("URL编码失败", e);
                }
                first = false;
            }
        }
        return sb.toString();
    }

    private static String executeWithRetry(Request request, int retryCount) {
        IOException lastException = null;
        for (int i = 0; i < retryCount; i++) {
            try {
                return execute(request);
            } catch (IOException e) {
                lastException = e;
                log.warn("HTTP请求第{}次失败: {}", i + 1, e.getMessage());
                if (i < retryCount - 1) {
                    try {
                        Thread.sleep((long) Math.pow(2, i) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        log.error("HTTP请求最终失败", lastException);
        throw new RuntimeException("HTTP请求失败: " + (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    private static String execute(Request request) throws IOException {
        long startTime = System.currentTimeMillis();
        try (Response response = CLIENT.newCall(request).execute()) {
            long costTime = System.currentTimeMillis() - startTime;
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                String result = body != null ? body.string() : "";
                log.debug("HTTP请求成功, URL:{}, 耗时:{}ms, 响应:{}", request.url(), costTime, result);
                return result;
            } else {
                log.warn("HTTP请求失败, URL:{}, 状态码:{}, 耗时:{}ms", request.url(), response.code(), costTime);
                throw new IOException("HTTP请求失败，状态码: " + response.code());
            }
        }
    }
}
