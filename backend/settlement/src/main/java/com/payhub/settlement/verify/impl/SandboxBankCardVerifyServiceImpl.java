package com.payhub.settlement.verify.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payhub.settlement.verify.BankCardVerifyResult;
import com.payhub.settlement.verify.BankCardVerifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service("bankCardVerifyService")
public class SandboxBankCardVerifyServiceImpl implements BankCardVerifyService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public BankCardVerifyResult verifyFourElements(String idCardName, String idCardNo, String bankCardNo, String bankPhone, String verifyRequestId) {
        JSONObject requestJson = new JSONObject();
        requestJson.set("idCardName", idCardName);
        requestJson.set("idCardNo", idCardNo);
        requestJson.set("bankCardNo", bankCardNo);
        requestJson.set("bankPhone", bankPhone);
        requestJson.set("verifyRequestId", verifyRequestId);
        requestJson.set("provider", "SANDBOX");
        requestJson.set("timestamp", LocalDateTime.now().format(FORMATTER));
        String requestData = JSONUtil.toJsonStr(requestJson);

        BankCardVerifyResult result = new BankCardVerifyResult();
        result.setRequestData(requestData);

        Boolean success;
        String failCode = null;
        String failReason = null;

        if (bankCardNo != null && bankCardNo.endsWith("0000")) {
            success = true;
        } else if (bankCardNo != null && bankCardNo.endsWith("9999")) {
            success = false;
            failCode = "E001";
            failReason = "银行卡四要素核验失败，信息不匹配";
        } else {
            success = true;
        }

        result.setSuccess(success);
        result.setFailCode(failCode);
        result.setFailReason(failReason);

        JSONObject responseJson = new JSONObject();
        responseJson.set("success", success);
        responseJson.set("failCode", failCode);
        responseJson.set("failReason", failReason);
        responseJson.set("transactionId", "SANDBOX_" + verifyRequestId);
        responseJson.set("responseTimestamp", LocalDateTime.now().format(FORMATTER));
        responseJson.set("provider", "SANDBOX");
        String responseData = JSONUtil.toJsonStr(responseJson);
        result.setResponseData(responseData);

        log.info("沙箱银行卡四要素核验完成: verifyRequestId={}, success={}, failCode={}", verifyRequestId, success, failCode);
        return result;
    }
}
