package com.payhub.settlement.verify.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payhub.channel.client.HttpUtil;
import com.payhub.settlement.verify.IdCardVerifyResult;
import com.payhub.settlement.verify.IdCardVerifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service("thirdPartyIdCardVerifyService")
@ConditionalOnProperty(name = "payhub.verify.idcard.provider", havingValue = "thirdparty")
public class ThirdPartyIdCardVerifyServiceImpl implements IdCardVerifyService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${payhub.verify.idcard.url:}")
    private String url;

    @Value("${payhub.verify.idcard.appId:}")
    private String appId;

    @Value("${payhub.verify.idcard.appSecret:}")
    private String appSecret;

    @Override
    public IdCardVerifyResult verifySecondGen(String idCardName, String idCardNo, String verifyRequestId) {
        return doVerify(idCardName, idCardNo, null, "SECOND_GEN", verifyRequestId);
    }

    @Override
    public IdCardVerifyResult verifyThirdGen(String idCardName, String idCardNo, String verifyRequestId) {
        return doVerify(idCardName, idCardNo, null, "THIRD_GEN", verifyRequestId);
    }

    @Override
    public IdCardVerifyResult verifyWithLiveness(String idCardName, String idCardNo, String faceImageBase64, String verifyRequestId) {
        return doVerify(idCardName, idCardNo, faceImageBase64, "LIVENESS", verifyRequestId);
    }

    private IdCardVerifyResult doVerify(String idCardName, String idCardNo, String faceImageBase64, String level, String verifyRequestId) {
        JSONObject requestJson = new JSONObject();
        requestJson.set("appId", appId);
        requestJson.set("idCardName", idCardName);
        requestJson.set("idCardNo", idCardNo);
        requestJson.set("verifyLevel", level);
        requestJson.set("verifyRequestId", verifyRequestId);
        requestJson.set("timestamp", LocalDateTime.now().format(FORMATTER));
        if (faceImageBase64 != null) {
            requestJson.set("faceImage", faceImageBase64);
        }
        String requestData = JSONUtil.toJsonStr(requestJson);

        IdCardVerifyResult result = new IdCardVerifyResult();
        result.setRequestData(requestData);
        result.setVerifyLevel(level);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=utf-8");
        headers.put("App-Id", appId);
        headers.put("App-Secret", appSecret);

        String responseData;
        try {
            responseData = HttpUtil.postJson(url, requestData, headers);
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("第三方身份证核验请求异常: verifyRequestId={}, level={}", verifyRequestId, level, e);
            result.setSuccess(false);
            result.setFailCode("E999");
            result.setFailReason("第三方核验请求异常: " + e.getMessage());
            JSONObject errorResp = new JSONObject();
            errorResp.set("success", false);
            errorResp.set("failCode", "E999");
            errorResp.set("failReason", "第三方核验请求异常: " + e.getMessage());
            errorResp.set("transactionId", "ERROR_" + verifyRequestId);
            result.setResponseData(JSONUtil.toJsonStr(errorResp));
            return result;
        }

        result.setResponseData(responseData);

        try {
            JSONObject respJson = JSONUtil.parseObj(responseData);
            result.setSuccess(respJson.getBool("success", false));
            result.setFailCode(respJson.getStr("failCode"));
            result.setFailReason(respJson.getStr("failReason"));
            result.setTransactionId(respJson.getStr("transactionId"));
        } catch (Exception e) {
            log.warn("解析第三方身份证核验响应失败: verifyRequestId={}, responseData={}", verifyRequestId, responseData);
            result.setSuccess(false);
            result.setFailCode("E998");
            result.setFailReason("解析第三方响应失败");
        }

        log.info("第三方身份证核验完成: verifyRequestId={}, level={}, success={}", verifyRequestId, level, result.getSuccess());
        return result;
    }
}
