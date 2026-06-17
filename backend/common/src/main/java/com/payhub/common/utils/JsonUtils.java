package com.payhub.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.alibaba.fastjson2.filter.PropertyFilter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;

import java.util.List;
import java.util.Map;

public class JsonUtils {

    private static final FastJsonConfig CONFIG = new FastJsonConfig();

    static {
        CONFIG.setWriterFeatures(JSONWriter.Feature.WriteMapNullValue,
                JSONWriter.Feature.WriteNullListAsEmpty,
                JSONWriter.Feature.WriteNullStringAsEmpty,
                JSONWriter.Feature.WriteNullNumberAsZero);
        CONFIG.setReaderFeatures(JSONReader.Feature.SupportSmartMatch);
    }

    private JsonUtils() {
    }

    public static String toJson(Object obj) {
        return obj == null ? null : JSON.toJSONString(obj, CONFIG.getWriterFeatures());
    }

    public static String toJsonPretty(Object obj) {
        return obj == null ? null : JSON.toJSONString(obj, CONFIG.getWriterFeatures(), JSONWriter.Feature.PrettyFormat);
    }

    public static String toJsonWithFilter(Object obj, PropertyFilter filter) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj, filter, CONFIG.getWriterFeatures());
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, clazz, CONFIG.getReaderFeatures());
    }

    public static <T> T parseObject(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, typeReference, CONFIG.getReaderFeatures());
    }

    public static JSONObject parseObject(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json);
    }

    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseArray(json, clazz, CONFIG.getReaderFeatures());
    }

    public static JSONArray parseArray(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseArray(json);
    }

    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, new TypeReference<Map<String, Object>>() {
        }, CONFIG.getReaderFeatures());
    }

    public static <K, V> Map<K, V> parseMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, new TypeReference<Map<K, V>>() {
        }, CONFIG.getReaderFeatures());
    }

    public static <T> T convert(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String s) {
            return parseObject(s, clazz);
        }
        return JSON.to(clazz, obj);
    }

    public static JSONObject toJsonObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String s) {
            return parseObject(s);
        }
        return (JSONObject) JSON.toJSON(obj);
    }

    public static boolean isJson(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            JSON.parse(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isJsonObject(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            JSON.parseObject(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isJsonArray(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            JSON.parseArray(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
