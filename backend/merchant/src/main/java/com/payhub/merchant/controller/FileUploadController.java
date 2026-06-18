package com.payhub.merchant.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    @Value("${payhub.upload.path:./uploads}")
    private String uploadPath;

    @Value("${payhub.upload.max-size:5242880}")
    private long maxFileSize;

    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
    ));

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    ));

    @PostMapping("/upload/image")
    public Result<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bizType", defaultValue = "common") String bizType,
            HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择要上传的文件");
        }

        String contentType = file.getContentType();
        if (StrUtil.isBlank(contentType) || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "只支持上传图片文件");
        }

        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "文件大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件名不能为空");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不支持的图片格式");
        }

        try {
            String dateDir = DateUtil.format(new Date(), "yyyyMMdd");
            String saveDir = uploadPath + File.separator + bizType + File.separator + dateDir;
            File dir = new File(saveDir);
            if (!dir.exists() && !dir.mkdirs()) {
                log.error("创建上传目录失败: {}", saveDir);
                throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "上传目录创建失败");
            }

            String fileName = IdUtil.simpleUUID() + extension;
            String filePath = saveDir + File.separator + fileName;
            File destFile = new File(filePath);
            file.transferTo(destFile);

            String fileUrl = "/uploads/" + bizType + "/" + dateDir + "/" + fileName;

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("fileName", fileName);
            result.put("originalName", originalFilename);
            result.put("size", String.valueOf(file.getSize()));

            log.info("图片上传成功: bizType={}, url={}, size={}", bizType, fileUrl, file.getSize());
            return Result.success(result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "文件上传失败：" + e.getMessage());
        }
    }
}
