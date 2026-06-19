package com.payhub.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.annotation.SandboxMode;
import com.payhub.common.context.SandboxContext;
import com.payhub.common.enums.SandboxSceneEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.IdGenerator;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.pay.dto.UnifiedOrderRequest;
import com.payhub.pay.dto.UnifiedOrderResponse;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.service.PayOrderService;
import com.payhub.risk.dto.SandboxTestRequest;
import com.payhub.risk.dto.SandboxTestResultVO;
import com.payhub.risk.entity.SandboxTestRecord;
import com.payhub.risk.mapper.SandboxTestRecordMapper;
import com.payhub.risk.service.SandboxTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SandboxTestServiceImpl extends ServiceImpl<SandboxTestRecordMapper, SandboxTestRecord> implements SandboxTestService {

    @Autowired(required = false)
    private PayOrderService payOrderService;

    @Override
    @SandboxMode
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

        SandboxSceneEnum sceneEnum = SandboxSceneEnum.getByCode(request.getTestScene());
        SandboxContext.setScene(sceneEnum.getCode());

        Integer expectResult = sceneEnum == SandboxSceneEnum.SUCCESS
                || sceneEnum == SandboxSceneEnum.REPEAT_NOTIFY
                || sceneEnum == SandboxSceneEnum.REFUND_SUCCESS ? 1 : 0;
        record.setExpectResult(expectResult);

        Map<String, Object> responseData = new HashMap<>();
        Integer actualResult;
        String errorMsg = null;
        String notifyResult = null;

        try {
            if (payOrderService != null && isPayTestScene(sceneEnum)) {
                actualResult = executeRealPayTest(request, sceneEnum, responseData);
                if (sceneEnum == SandboxSceneEnum.REPEAT_NOTIFY || sceneEnum == SandboxSceneEnum.SUCCESS) {
                    notifyResult = "已触发模拟异步通知，场景: " + sceneEnum.getName();
                }
            } else {
                actualResult = executeSimulatedTest(sceneEnum, responseData, request);
                notifyResult = (sceneEnum == SandboxSceneEnum.SUCCESS || sceneEnum == SandboxSceneEnum.REPEAT_NOTIFY)
                        ? "模拟通知已发送（演示模式）" : null;
            }

            if (expectResult == 0 && actualResult == 0) {
                errorMsg = sceneEnum.getDescription();
            }
        } catch (Exception e) {
            actualResult = 0;
            errorMsg = "测试执行异常: " + e.getMessage();
            log.error("沙箱测试执行异常, testId={}, scene={}", testId, request.getTestScene(), e);
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

    private boolean isPayTestScene(SandboxSceneEnum scene) {
        return scene == SandboxSceneEnum.SUCCESS
                || scene == SandboxSceneEnum.FAILED
                || scene == SandboxSceneEnum.TIMEOUT
                || scene == SandboxSceneEnum.INSUFFICIENT_BALANCE
                || scene == SandboxSceneEnum.REPEAT_NOTIFY
                || scene == SandboxSceneEnum.SIGN_ERROR
                || scene == SandboxSceneEnum.AMOUNT_MISMATCH
                || scene == SandboxSceneEnum.CHANNEL_ERROR;
    }

    private Integer executeRealPayTest(SandboxTestRequest request, SandboxSceneEnum sceneEnum,
                                       Map<String, Object> responseData) {
        try {
            UnifiedOrderRequest orderRequest = new UnifiedOrderRequest();
            orderRequest.setMerchantNo(request.getMerchantNo());
            orderRequest.setMerchantOrderNo("SB" + System.currentTimeMillis());
            orderRequest.setPayChannel(request.getPayChannel());
            orderRequest.setPayType(request.getPayType());
            orderRequest.setPayAmount(request.getPayAmount());
            orderRequest.setProductSubject(request.getTestName());
            orderRequest.setProductDetail("沙箱测试订单 - " + sceneEnum.getName());
            orderRequest.setNotifyUrl(request.getNotifyUrl());
            orderRequest.setClientIp("127.0.0.1");

            UnifiedOrderResponse orderResponse = payOrderService.unifiedOrder(orderRequest);
            responseData.put("orderNo", orderResponse.getOrderNo());
            responseData.put("payType", orderResponse.getPayType());
            responseData.put("payStatus", orderResponse.getPayStatus());
            if (orderResponse.getPayParams() != null) {
                responseData.put("payParams", JSON.parse(orderResponse.getPayParams()));
            }

            boolean isSuccess = orderResponse.isSuccess()
                    && !"TIMEOUT".equals(orderResponse.getCode())
                    && !"INSUFFICIENT_BALANCE".equals(orderResponse.getCode())
                    && !"PAY_FAIL".equals(orderResponse.getCode())
                    && !"SYSTEM_ERROR".equals(orderResponse.getCode());

            if (isSuccess && (sceneEnum == SandboxSceneEnum.SUCCESS || sceneEnum == SandboxSceneEnum.REPEAT_NOTIFY)) {
                PayOrder order = payOrderService.getOrderDetail(orderResponse.getOrderNo(), request.getMerchantNo());
                if (order != null) {
                    payOrderService.simulateAsyncNotifyAfterDelay(order);
                }
            }

            responseData.put("channelResponse", orderResponse.getMsg());
            responseData.put("channelCode", orderResponse.getCode());
            return isSuccess ? 1 : 0;
        } catch (BusinessException e) {
            responseData.put("errorCode", e.getCode());
            responseData.put("errorMsg", e.getMessage());
            return 0;
        } catch (Exception e) {
            responseData.put("errorMsg", e.getMessage());
            return 0;
        }
    }

    private Integer executeSimulatedTest(SandboxSceneEnum sceneEnum, Map<String, Object> responseData,
                                          SandboxTestRequest request) {
        long delay = sceneEnum == SandboxSceneEnum.TIMEOUT ? 3000L : 500L;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        switch (sceneEnum) {
            case SUCCESS:
            case REPEAT_NOTIFY:
            case REFUND_SUCCESS:
                responseData.put("code", 200);
                responseData.put("message", "success");
                responseData.put("orderNo", "SANDBOX_" + OrderNoGenerator.generate());
                responseData.put("payUrl", "https://sandbox.example.com/pay/" + IdGenerator.generateIdStr());
                return 1;
            case FAILED:
            case REFUND_FAILED:
                responseData.put("code", 400);
                responseData.put("message", "支付失败");
                return 0;
            case TIMEOUT:
                responseData.put("code", 504);
                responseData.put("message", "timeout");
                return 0;
            case INSUFFICIENT_BALANCE:
                responseData.put("code", 402);
                responseData.put("message", "余额不足");
                return 0;
            case SIGN_ERROR:
                responseData.put("code", 401);
                responseData.put("message", "签名错误");
                return 0;
            case AMOUNT_MISMATCH:
                responseData.put("code", 402);
                responseData.put("message", "金额不匹配");
                return 0;
            case CHANNEL_ERROR:
                responseData.put("code", 500);
                responseData.put("message", "通道系统异常");
                return 0;
            default:
                responseData.put("code", 500);
                responseData.put("message", "未知场景");
                return 0;
        }
    }

    @Override
    public IPage<SandboxTestResultVO> listTestRecords(Long current, Long size, Map<String, Object> params) {
        LambdaQueryWrapper<SandboxTestRecord> wrapper = new LambdaQueryWrapper<>();
        if (params != null && params.get("merchantNo") != null && StrUtil.isNotBlank(params.get("merchantNo").toString())) {
            wrapper.eq(SandboxTestRecord::getMerchantNo, params.get("merchantNo"));
        }
        if (params != null && params.get("testScene") != null && StrUtil.isNotBlank(params.get("testScene").toString())) {
            wrapper.eq(SandboxTestRecord::getTestScene, params.get("testScene"));
        }
        if (params != null && params.get("success") != null) {
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
        for (SandboxSceneEnum sceneEnum : SandboxSceneEnum.values()) {
            Map<String, Object> scene = new HashMap<>();
            scene.put("code", sceneEnum.getCode());
            scene.put("name", sceneEnum.getName());
            scene.put("description", sceneEnum.getDescription());
            scenes.add(scene);
        }
        return scenes;
    }

    private SandboxTestResultVO convertToVO(SandboxTestRecord record) {
        boolean success = record.getExpectResult() != null
                && record.getExpectResult().equals(record.getActualResult());
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
