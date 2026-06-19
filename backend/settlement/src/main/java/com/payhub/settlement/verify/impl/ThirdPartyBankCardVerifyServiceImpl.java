package com.payhub.settlement.verify.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payhub.channel.client.HttpUtil;
import com.payhub.settlement.verify.BankCardVerifyResult;
import com.payhub.settlement.verify.BankCardVerifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service("thirdPartyBankCardVerifyService")
@ConditionalOnProperty(name = "payhub.verify.bankcard.provider", havingValue = "thirdparty")
public class ThirdPartyBankCardVerifyServiceImpl implements BankCardVerifyService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${payhub.verify.bankcard.url:}")
    private String url;

    @Value("${payhub.verify.bankcard.appId:}")
    private String appId;

    @Value("${payhub.verify.bankcard.appSecret:}")
    private String appSecret;

    @Override
    public BankCardVerifyResult verifyFourElements(String idCardName, String idCardNo, String bankCardNo, String bankPhone, String verifyRequestId) {
        JSONObject requestJson = new JSONObject();
        requestJson.set("appId", appId);
        requestJson.set("idCardName", idCardName);
        requestJson.set("idCardNo", idCardNo);
        requestJson.set("bankCardNo", bankCardNo);
        requestJson.set("bankPhone", bankPhone);
        requestJson.set("verifyRequestId", verifyRequestId);
        requestJson.set("provider", "THIRD_PARTY");
        requestJson.set("timestamp", LocalDateTime.now().format(FORMATTER));
        String requestData = JSONUtil.toJsonStr(requestJson);

        BankCardVerifyResult result = new BankCardVerifyResult();
        result.setRequestData(requestData);

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
            log.error("第三方银行卡四要素核验请求异常: verifyRequestId={}", verifyRequestId, e);
            result.setSuccess(false);
            result.setFailCode("E999");
            result.setFailReason("第三方核验请求异常: " + e.getMessage());
            JSONObject errorResp = new JSONObject();
            errorResp.set("success", false);
            errorResp.set("failCode", "E999");
            errorResp.set("failReason", "第三方核验请求异常: " + e.getMessage());
            errorResp.set("transactionId", "ERROR_" + verifyRequestId);
            errorResp.set("responseTimestamp", LocalDateTime.now().format(FORMATTER));
            errorResp.set("provider", "THIRD_PARTY");
            result.setResponseData(JSONUtil.toJsonStr(errorResp));
            return result;
        }

        result.setResponseData(responseData);

        try {
            JSONObject respJson = JSONUtil.parseObj(responseData);
            Boolean success = respJson.getBool("success", false);
            result.setSuccess(success);
            result.setFailCode(respJson.getStr("failCode"));
            result.setFailReason(respJson.getStr("failReason"));
        } catch (Exception e) {
            log.warn("解析第三方银行卡四要素核验响应失败: verifyRequestId={}, responseData={}", verifyRequestId, responseData);
            result.setSuccess(false);
            result.setFailCode("E998");
            result.setFailReason("解析第三方响应失败");
        }

        log.info("第三方银行卡四要素核验完成: verifyRequestId={}, success={}", verifyRequestId, result.getSuccess());
        return result;
    }
}
