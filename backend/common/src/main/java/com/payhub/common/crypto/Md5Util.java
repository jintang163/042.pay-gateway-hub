package com.payhub.common.crypto;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class Md5Util {

    private Md5Util() {
    }

    public static String md5(String data) {
        return DigestUtils.md5Hex(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String md5(byte[] data) {
        return DigestUtils.md5Hex(data);
    }

    public static String md5(InputStream inputStream) {
        try {
            return DigestUtils.md5Hex(inputStream);
        } catch (IOException e) {
            log.error("MD5计算流失败", e);
            throw new RuntimeException("MD5计算流失败", e);
        }
    }

    public static String md5File(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return md5(fis);
        } catch (IOException e) {
            log.error("MD5计算文件失败: {}", filePath, e);
            throw new RuntimeException("MD5计算文件失败", e);
        }
    }

    public static String md5File(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            return md5(fis);
        } catch (IOException e) {
            log.error("MD5计算文件失败: {}", file.getPath(), e);
            throw new RuntimeException("MD5计算文件失败", e);
        }
    }

    public static String md5WithSalt(String data, String salt) {
        return md5(data + salt);
    }
}
