package com.payhub.settlement.verify.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payhub.common.context.SandboxContext;
import com.payhub.common.enums.SandboxSceneEnum;
import com.payhub.settlement.verify.IdCardVerifyResult;
import com.payhub.settlement.verify.IdCardVerifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service("idCardVerifyService")
@ConditionalOnProperty(name = "payhub.verify.idcard.provider", havingValue = "sandbox", matchIfMissing = true)
public class SandboxIdCardVerifyServiceImpl implements IdCardVerifyService {

    @Override
    public IdCardVerifyResult verifySecondGen(String idCardName, String idCardNo, String verifyRequestId) {
        return doVerify(idCardName, idCardNo, "SECOND_GEN", verifyRequestId);
    }

    @Override
    public IdCardVerifyResult verifyThirdGen(String idCardName, String idCardNo, String verifyRequestId) {
        return doVerify(idCardName, idCardNo, "THIRD_GEN", verifyRequestId);
    }

    @Override
    public IdCardVerifyResult verifyWithLiveness(String idCardName, String idCardNo, String faceImageBase64, String verifyRequestId) {
        return doVerify(idCardName, idCardNo, "LIVENESS", verifyRequestId);
    }

    private IdCardVerifyResult doVerify(String idCardName, String idCardNo, String level, String verifyRequestId) {
        JSONObject requestJson = new JSONObject();
        requestJson.set("idCardName", idCardName);
        requestJson.set("idCardNo", idCardNo);
        requestJson.set("verifyLevel", level);
        requestJson.set("verifyRequestId", verifyRequestId);
        requestJson.set("provider", "SANDBOX");
        String requestData = JSONUtil.toJsonStr(requestJson);

        IdCardVerifyResult result = new IdCardVerifyResult();
        result.setRequestData(requestData);
        result.setVerifyLevel(level);
        result.setTransactionId("SB" + IdUtil.getSnowflakeNextIdStr());

        String scene = SandboxContext.getScene();
        boolean sandboxForceFail = SandboxSceneEnum.VERIFY_FAIL.getCode().equalsIgnoreCase(scene);
        boolean sandboxForceException = SandboxSceneEnum.VERIFY_EXCEPTION.getCode().equalsIgnoreCase(scene);

        if (sandboxForceException) {
            log.warn("[SANDBOX][身份证核验]强制异常场景: verifyRequestId={}, level={}", verifyRequestId, level);
            result.setSuccess(false);
            result.setFailCode("E999");
            result.setFailReason("沙箱强制异常场景");
        } else if (sandboxForceFail) {
            log.warn("[SANDBOX][身份证核验]强制失败场景: verifyRequestId={}, level={}", verifyRequestId, level);
            result.setSuccess(false);
            result.setFailCode("E001");
            result.setFailReason("身份证信息与公安库不匹配");
        } else if (idCardNo.endsWith("9999")) {
            log.warn("[SANDBOX][身份证核验]身份证号以9999结尾 -> 核验失败: verifyRequestId={}, idCardNo={}", verifyRequestId, idCardNo);
            result.setSuccess(false);
            result.setFailCode("E002");
            result.setFailReason("身份证号码无效或已过期");
        } else if (idCardNo.endsWith("8888")) {
            log.warn("[SANDBOX][身份证核验]身份证号以8888结尾 -> 核验处理中: verifyRequestId={}, idCardNo={}", verifyRequestId, idCardNo);
            result.setSuccess(false);
            result.setFailCode("E100");
            result.setFailReason("核验处理中，请稍后查询");
        } else {
            log.info("[SANDBOX][身份证核验]核验成功: verifyRequestId={}, level={}, idCardNo={}", verifyRequestId, level, idCardNo);
            result.setSuccess(true);
        }

        JSONObject responseJson = new JSONObject();
        responseJson.set("success", result.getSuccess());
        responseJson.set("failCode", result.getFailCode());
        responseJson.set("failReason", result.getFailReason());
        responseJson.set("transactionId", result.getTransactionId());
        responseJson.set("verifyLevel", level);
        responseJson.set("provider", "SANDBOX");
        result.setResponseData(JSONUtil.toJsonStr(responseJson));

        return result;
    }
}
