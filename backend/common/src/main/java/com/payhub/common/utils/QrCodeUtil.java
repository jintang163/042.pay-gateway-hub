package com.payhub.common.utils;

import cn.hutool.core.util.StrUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class QrCodeUtil {

    private static final String DEFAULT_FORMAT = "PNG";
    private static final int DEFAULT_SIZE = 300;
    private static final int DEFAULT_MARGIN = 2;

    public static BufferedImage generateImage(String content) {
        return generateImage(content, DEFAULT_SIZE);
    }

    public static BufferedImage generateImage(String content, int size) {
        return generateImage(content, size, size, DEFAULT_MARGIN);
    }

    public static BufferedImage generateImage(String content, int width, int height, int margin) {
        if (StrUtil.isBlank(content)) {
            throw new IllegalArgumentException("二维码内容不能为空");
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, margin);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (WriterException e) {
            log.error("生成二维码失败: content={}", content, e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    public static String generateBase64(String content) {
        return generateBase64(content, DEFAULT_SIZE);
    }

    public static String generateBase64(String content, int size) {
        return generateBase64(content, size, size, DEFAULT_MARGIN);
    }

    public static String generateBase64(String content, int width, int height, int margin) {
        BufferedImage image = generateImage(content, width, height, margin);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, DEFAULT_FORMAT, baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.error("生成二维码 base64 失败: content={}", content, e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }
}
