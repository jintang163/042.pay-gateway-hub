package com.payhub.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.IdGenerator;
import com.payhub.risk.dto.SandboxTestRequest;
import com.payhub.risk.dto.SandboxTestResultVO;
import com.payhub.risk.entity.SandboxTestRecord;
import com.payhub.risk.mapper.SandboxTestRecordMapper;
import com.payhub.risk.service.SandboxTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SandboxTestServiceImpl extends ServiceImpl<SandboxTestRecordMapper, SandboxTestRecord> implements SandboxTestService {

    private static final String SCENE_SUCCESS = "SUCCESS";
    private static final String SCENE_FAIL = "FAIL";
    private static final String SCENE_TIMEOUT = "TIMEOUT";
    private static final String SCENE_DUPLICATE_NOTIFY = "DUPLICATE_NOTIFY";
    private static final String SCENE_SIGN_ERROR = "SIGN_ERROR";
    private static final String SCENE_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";

    @Override
    public SandboxTestResultVO executeTest(SandboxTestRequest request) {
        LocalDateTime startTime = LocalDateTime.now();
        String testId = IdGenerator.generateIdStr();

        SandboxTestRecord record = new SandboxTestRecord();
        record.setTestId(testId);
        record.setMerchantNo(request.getMerchantNo());
        record.setTestScene(request.getTestScene());
        record.setTestName(request.getTestName());
        record.setPayChannel(request.getPayChannel());
        record.setPayType(request.getPayType());
        record.setPayAmount(request.getPayAmount());
        record.setTestParams(JSON.toJSONString(request));
        record.setStartTime(startTime);

        Integer expectResult = getExpectResult(request.getTestScene());
        record.setExpectResult(expectResult);

        Map<String, Object> responseData = new HashMap<>();
        Integer actualResult;
        String errorMsg = null;
        String notifyResult = null;

        try {
            Thread.sleep(getSimulateDelay(request.getTestScene()));

            switch (request.getTestScene()) {
                case SCENE_SUCCESS:
                    actualResult = 1;
                    responseData.put("code", 200);
                    responseData.put("message", "success");
                    responseData.put("orderNo", "SANDBOX_" + testId);
                    responseData.put("payUrl", "https://sandbox.example.com/pay/" + testId);
                    notifyResult = simulateNotify(request.getNotifyUrl(), true, false);
                    break;
                case SCENE_FAIL:
                    actualResult = 0;
                    responseData.put("code", 400);
                    responseData.put("message", "支付失败");
                    errorMsg = "模拟支付失败场景";
                    break;
                case SCENE_TIMEOUT:
                    actualResult = 0;
                    responseData.put("code", 504);
                    responseData.put("message", "timeout");
                    errorMsg = "模拟支付超时场景";
                    break;
                case SCENE_DUPLICATE_NOTIFY:
                    actualResult = 1;
                    responseData.put("code", 200);
                    responseData.put("message", "success");
                    responseData.put("orderNo", "SANDBOX_" + testId);
                    notifyResult = simulateNotify(request.getNotifyUrl(), true, true);
                    break;
                case SCENE_SIGN_ERROR:
                    actualResult = 0;
                    responseData.put("code", 401);
                    responseData.put("message", "sign error");
                    errorMsg = "模拟签名错误场景";
                    break;
                case SCENE_AMOUNT_MISMATCH:
                    actualResult = 0;
                    responseData.put("code", 402);
                    responseData.put("message", "amount mismatch");
                    errorMsg = "模拟金额不匹配场景";
                    break;
                default:
                    throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的测试场景：" + request.getTestScene());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            actualResult = 0;
            errorMsg = "测试执行被中断";
            log.error("沙箱测试被中断", e);
        }

        LocalDateTime endTime = LocalDateTime.now();
        long costTime = java.time.Duration.between(startTime, endTime).toMillis();

        record.setActualResult(actualResult);
        record.setResponseData(JSON.toJSONString(responseData));
        record.setNotifyResult(notifyResult);
        record.setErrorMsg(errorMsg);
        record.setEndTime(endTime);
        record.setCostTime(costTime);

        this.save(record);

        boolean success = expectResult.equals(actualResult);

        return SandboxTestResultVO.builder()
                .testId(testId)
                .merchantNo(request.getMerchantNo())
                .testScene(request.getTestScene())
                .testName(request.getTestName())
                .payChannel(request.getPayChannel())
                .payType(request.getPayType())
                .payAmount(request.getPayAmount())
                .expectResult(expectResult)
                .actualResult(actualResult)
                .success(success)
                .responseData(JSON.toJSONString(responseData))
                .notifyResult(notifyResult)
                .errorMsg(errorMsg)
                .costTime(costTime)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }

    @Override
    public IPage<SandboxTestResultVO> listTestRecords(Long current, Long size, Map<String, Object> params) {
        LambdaQueryWrapper<SandboxTestRecord> wrapper = new LambdaQueryWrapper<>();
        if (params.get("merchantNo") != null) {
            wrapper.eq(SandboxTestRecord::getMerchantNo, params.get("merchantNo"));
        }
        if (params.get("testScene") != null) {
            wrapper.eq(SandboxTestRecord::getTestScene, params.get("testScene"));
        }
        if (params.get("success") != null) {
            wrapper.eq(SandboxTestRecord::getActualResult, params.get("success"));
        }
        wrapper.orderByDesc(SandboxTestRecord::getStartTime);

        IPage<SandboxTestRecord> page = this.page(new Page<>(current, size), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    public SandboxTestResultVO getTestRecord(String testId) {
        LambdaQueryWrapper<SandboxTestRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SandboxTestRecord::getTestId, testId);
        wrapper.last("LIMIT 1");
        SandboxTestRecord record = this.getOne(wrapper);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "测试记录不存在");
        }
        return convertToVO(record);
    }

    @Override
    public List<Map<String, Object>> listTestScenes() {
        List<Map<String, Object>> scenes = new ArrayList<>();
        scenes.add(buildScene(SCENE_SUCCESS, "支付成功", "模拟正常支付成功流程，包含异步通知"));
        scenes.add(buildScene(SCENE_FAIL, "支付失败", "模拟支付失败的情况"));
        scenes.add(buildScene(SCENE_TIMEOUT, "支付超时", "模拟支付请求超时的情况"));
        scenes.add(buildScene(SCENE_DUPLICATE_NOTIFY, "重复通知", "模拟支付成功后发送多次异步通知"));
        scenes.add(buildScene(SCENE_SIGN_ERROR, "签名错误", "模拟响应签名验证失败的情况"));
        scenes.add(buildScene(SCENE_AMOUNT_MISMATCH, "金额不匹配", "模拟支付金额与订单金额不一致的情况"));
        return scenes;
    }

    private Map<String, Object> buildScene(String code, String name, String description) {
        Map<String, Object> scene = new HashMap<>();
        scene.put("code", code);
        scene.put("name", name);
        scene.put("description", description);
        return scene;
    }

    private Integer getExpectResult(String scene) {
        if (SCENE_SUCCESS.equals(scene) || SCENE_DUPLICATE_NOTIFY.equals(scene)) {
            return 1;
        }
        return 0;
    }

    private long getSimulateDelay(String scene) {
        if (SCENE_TIMEOUT.equals(scene)) {
            return 5000L;
        }
        return 500L;
    }

    private String simulateNotify(String notifyUrl, boolean success, boolean duplicate) {
        if (StrUtil.isBlank(notifyUrl)) {
            return "未配置回调地址，跳过通知模拟";
        }
        StringBuilder result = new StringBuilder();
        int times = duplicate ? 3 : 1;
        for (int i = 1; i <= times; i++) {
            result.append(String.format("第%d次通知[%s]: %s; ", i, notifyUrl, success ? "SUCCESS" : "FAIL"));
        }
        return result.toString();
    }

    private SandboxTestResultVO convertToVO(SandboxTestRecord record) {
        boolean success = record.getExpectResult().equals(record.getActualResult());
        return SandboxTestResultVO.builder()
                .testId(record.getTestId())
                .merchantNo(record.getMerchantNo())
                .testScene(record.getTestScene())
                .testName(record.getTestName())
                .payChannel(record.getPayChannel())
                .payType(record.getPayType())
                .payAmount(record.getPayAmount())
                .expectResult(record.getExpectResult())
                .actualResult(record.getActualResult())
                .success(success)
                .responseData(record.getResponseData())
                .notifyResult(record.getNotifyResult())
                .errorMsg(record.getErrorMsg())
                .costTime(record.getCostTime())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .build();
    }
}
