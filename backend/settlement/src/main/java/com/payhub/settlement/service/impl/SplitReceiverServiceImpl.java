package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.settlement.dto.SplitReceiverBatchImportItem;
import com.payhub.settlement.dto.SplitReceiverIdCardVerifyRequest;
import com.payhub.settlement.dto.SplitReceiverSaveRequest;
import com.payhub.settlement.dto.SplitReceiverVO;
import com.payhub.settlement.dto.SplitReceiverVerifyLogVO;
import com.payhub.settlement.dto.SplitReceiverVerifyRequest;
import com.payhub.settlement.entity.SplitReceiver;
import com.payhub.settlement.entity.SplitReceiverVerifyLog;
import com.payhub.settlement.enums.SplitReceiverTypeEnum;
import com.payhub.settlement.enums.SplitReceiverVerifyStatusEnum;
import com.payhub.settlement.enums.VerifyChannelEnum;
import com.payhub.settlement.mapper.SplitReceiverMapper;
import com.payhub.settlement.mapper.SplitReceiverVerifyLogMapper;
import com.payhub.settlement.service.SplitReceiverService;
import com.payhub.settlement.verify.BankCardVerifyResult;
import com.payhub.settlement.verify.BankCardVerifyService;
import com.payhub.settlement.verify.IdCardVerifyResult;
import com.payhub.settlement.verify.IdCardVerifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class SplitReceiverServiceImpl extends ServiceImpl<SplitReceiverMapper, SplitReceiver> implements SplitReceiverService {

    @Autowired
    private SplitReceiverVerifyLogMapper verifyLogMapper;

    @Autowired
    @Qualifier("bankCardVerifyService")
    private BankCardVerifyService bankCardVerifyService;

    @Autowired(required = false)
    @Qualifier("idCardVerifyService")
    private IdCardVerifyService idCardVerifyService;

    @Override
    public IPage<SplitReceiverVO> listPage(Long current, Long size, String merchantNo, Map<String, Object> params) {
        Page<SplitReceiver> page = new Page<>(current, size);
        LambdaQueryWrapper<SplitReceiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SplitReceiver::getMerchantNo, merchantNo);
        if (params != null) {
            if (params.get("receiverNo") != null && StrUtil.isNotBlank(params.get("receiverNo").toString())) {
                wrapper.like(SplitReceiver::getReceiverNo, params.get("receiverNo"));
            }
            if (params.get("receiverName") != null && StrUtil.isNotBlank(params.get("receiverName").toString())) {
                wrapper.like(SplitReceiver::getReceiverName, params.get("receiverName"));
            }
            if (params.get("receiverType") != null) {
                wrapper.eq(SplitReceiver::getReceiverType, params.get("receiverType"));
            }
            if (params.get("verifyStatus") != null) {
                wrapper.eq(SplitReceiver::getVerifyStatus, params.get("verifyStatus"));
            }
            if (params.get("status") != null) {
                wrapper.eq(SplitReceiver::getStatus, params.get("status"));
            }
            if (params.get("idCardNo") != null && StrUtil.isNotBlank(params.get("idCardNo").toString())) {
                wrapper.like(SplitReceiver::getIdCardNo, params.get("idCardNo"));
            }
            if (params.get("bankCardNo") != null && StrUtil.isNotBlank(params.get("bankCardNo").toString())) {
                wrapper.like(SplitReceiver::getBankCardNo, params.get("bankCardNo"));
            }
        }
        wrapper.orderByDesc(SplitReceiver::getId);
        IPage<SplitReceiver> receiverPage = this.page(page, wrapper);
        return receiverPage.convert(this::convertToVO);
    }

    @Override
    public SplitReceiverVO getByReceiverNo(String receiverNo) {
        LambdaQueryWrapper<SplitReceiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SplitReceiver::getReceiverNo, receiverNo);
        SplitReceiver receiver = this.getOne(wrapper);
        if (receiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账接收方不存在");
        }
        return convertToVO(receiver);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveReceiver(SplitReceiverSaveRequest request, String merchantNo, String operatorId, String operatorName) {
        SplitReceiver receiver;
        if (StrUtil.isNotBlank(request.getReceiverNo())) {
            LambdaQueryWrapper<SplitReceiver> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SplitReceiver::getReceiverNo, request.getReceiverNo())
                    .eq(SplitReceiver::getMerchantNo, merchantNo);
            receiver = this.getOne(wrapper);
            if (receiver == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "分账接收方不存在");
            }
        } else {
            receiver = new SplitReceiver();
            receiver.setReceiverNo(OrderNoGenerator.generateWithPrefix("RE"));
            receiver.setMerchantNo(merchantNo);
            receiver.setVerifyStatus(SplitReceiverVerifyStatusEnum.UNVERIFIED.getCode());
        }

        receiver.setReceiverName(request.getReceiverName());
        receiver.setReceiverType(request.getReceiverType());
        receiver.setIdCardNo(request.getIdCardNo());
        receiver.setIdCardName(request.getIdCardName());
        receiver.setBankCardNo(request.getBankCardNo());
        receiver.setBankPhone(request.getBankPhone());
        receiver.setBankName(request.getBankName());
        receiver.setBankBranchName(request.getBankBranchName());
        receiver.setContactName(request.getContactName());
        receiver.setContactPhone(request.getContactPhone());
        receiver.setContactEmail(request.getContactEmail());
        receiver.setRemark(request.getRemark());
        receiver.setStatus(request.getStatus());
        receiver.setOperatorId(operatorId);
        receiver.setOperatorName(operatorName);

        this.saveOrUpdate(receiver);
        log.info("分账接收方保存成功: id={}, receiverNo={}, merchantNo={}", receiver.getId(), receiver.getReceiverNo(), merchantNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        SplitReceiver receiver = this.getById(id);
        if (receiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账接收方不存在");
        }
        receiver.setStatus(receiver.getStatus() == 1 ? 0 : 1);
        this.updateById(receiver);
        log.info("分账接收方状态切换成功: id={}, status={}", id, receiver.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReceiver(Long id) {
        SplitReceiver receiver = this.getById(id);
        if (receiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账接收方不存在");
        }
        this.removeById(id);
        log.info("分账接收方删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyReceiver(SplitReceiverVerifyRequest request, String merchantNo, String operatorId, String operatorName) {
        LambdaQueryWrapper<SplitReceiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SplitReceiver::getReceiverNo, request.getReceiverNo())
                .eq(SplitReceiver::getMerchantNo, merchantNo);
        SplitReceiver receiver = this.getOne(wrapper);
        if (receiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账接收方不存在");
        }
        doVerifyReceiverInternal(receiver, request.getVerifyChannel(), operatorId, operatorName);
    }

    private void doVerifyReceiverInternal(SplitReceiver receiver, Integer verifyChannel, String operatorId, String operatorName) {
        if (SplitReceiverVerifyStatusEnum.VERIFYING.getCode().equals(receiver.getVerifyStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "接收方正在认证中，请稍后再试");
        }
        if (SplitReceiverVerifyStatusEnum.VERIFIED.getCode().equals(receiver.getVerifyStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "接收方已认证，无需重复认证");
        }

        String verifyRequestId = OrderNoGenerator.generateWithPrefix("VR");
        LocalDateTime verifyTime = LocalDateTime.now();

        BankCardVerifyResult verifyResult = bankCardVerifyService.verifyFourElements(
                receiver.getIdCardName(),
                receiver.getIdCardNo(),
                receiver.getBankCardNo(),
                receiver.getBankPhone(),
                verifyRequestId
        );

        Integer verifyStatus;
        String verifyResultStr;
        if (Boolean.TRUE.equals(verifyResult.getSuccess())) {
            verifyStatus = SplitReceiverVerifyStatusEnum.VERIFIED.getCode();
            verifyResultStr = "认证成功";
        } else {
            verifyStatus = SplitReceiverVerifyStatusEnum.FAILED.getCode();
            verifyResultStr = "认证失败";
        }

        receiver.setVerifyStatus(verifyStatus);
        receiver.setVerifyChannel(verifyChannel);
        receiver.setVerifyTime(verifyTime);
        receiver.setVerifyRequestId(verifyRequestId);
        receiver.setVerifyFailCode(verifyResult.getFailCode());
        receiver.setVerifyFailReason(verifyResult.getFailReason());
        receiver.setOperatorId(operatorId);
        receiver.setOperatorName(operatorName);
        this.updateById(receiver);

        SplitReceiverVerifyLog verifyLog = new SplitReceiverVerifyLog();
        verifyLog.setLogNo(OrderNoGenerator.generateWithPrefix("VL"));
        verifyLog.setMerchantNo(receiver.getMerchantNo());
        verifyLog.setReceiverNo(receiver.getReceiverNo());
        verifyLog.setVerifyChannel(verifyChannel);
        verifyLog.setVerifyRequestId(verifyRequestId);
        verifyLog.setIdCardName(receiver.getIdCardName());
        verifyLog.setIdCardNo(receiver.getIdCardNo());
        verifyLog.setBankCardNo(receiver.getBankCardNo());
        verifyLog.setBankPhone(receiver.getBankPhone());
        verifyLog.setVerifyStatus(verifyStatus);
        verifyLog.setVerifyResult(verifyResultStr);
        verifyLog.setVerifyFailCode(verifyResult.getFailCode());
        verifyLog.setVerifyFailReason(verifyResult.getFailReason());
        verifyLog.setVerifyTime(verifyTime);
        verifyLog.setRequestData(verifyResult.getRequestData());
        verifyLog.setResponseData(verifyResult.getResponseData());
        verifyLog.setOperatorId(operatorId);
        verifyLog.setOperatorName(operatorName);
        verifyLogMapper.insert(verifyLog);

        log.info("分账接收方实名认证完成: receiverNo={}, verifyStatus={}, verifyRequestId={}", receiver.getReceiverNo(), verifyStatus, verifyRequestId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyIdCard(SplitReceiverIdCardVerifyRequest request, String merchantNo, String operatorId, String operatorName) {
        LambdaQueryWrapper<SplitReceiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SplitReceiver::getReceiverNo, request.getReceiverNo())
                .eq(SplitReceiver::getMerchantNo, merchantNo);
        SplitReceiver receiver = this.getOne(wrapper);
        if (receiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账接收方不存在");
        }
        doVerifyIdCardInternal(receiver, request.getVerifyChannel(), request.getFaceImageBase64(), operatorId, operatorName);
    }

    private void doVerifyIdCardInternal(SplitReceiver receiver, Integer verifyChannel, String faceImageBase64,
                                        String operatorId, String operatorName) {
        if (idCardVerifyService == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "身份证核验服务未启用");
        }
        if (SplitReceiverVerifyStatusEnum.VERIFYING.getCode().equals(receiver.getIdCardVerifyStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "身份证正在认证中，请稍后再试");
        }
        if (SplitReceiverVerifyStatusEnum.VERIFIED.getCode().equals(receiver.getIdCardVerifyStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "身份证已认证，无需重复认证");
        }

        String verifyRequestId = OrderNoGenerator.generateWithPrefix("IV");
        LocalDateTime verifyTime = LocalDateTime.now();

        VerifyChannelEnum channelEnum = VerifyChannelEnum.getByCode(verifyChannel);
        if (channelEnum == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的认证渠道");
        }

        IdCardVerifyResult verifyResult;
        switch (channelEnum) {
            case ID_CARD_SECOND_GEN:
                verifyResult = idCardVerifyService.verifySecondGen(
                        receiver.getIdCardName(), receiver.getIdCardNo(), verifyRequestId);
                break;
            case ID_CARD_THIRD_GEN:
                verifyResult = idCardVerifyService.verifyThirdGen(
                        receiver.getIdCardName(), receiver.getIdCardNo(), verifyRequestId);
                break;
            case ID_CARD_LIVENESS:
                if (StrUtil.isBlank(faceImageBase64)) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "活体检测需要人脸图片");
                }
                verifyResult = idCardVerifyService.verifyWithLiveness(
                        receiver.getIdCardName(), receiver.getIdCardNo(), faceImageBase64, verifyRequestId);
                break;
            default:
                throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的身份证认证渠道");
        }

        Integer verifyStatus;
        if (Boolean.TRUE.equals(verifyResult.getSuccess())) {
            verifyStatus = SplitReceiverVerifyStatusEnum.VERIFIED.getCode();
        } else {
            verifyStatus = SplitReceiverVerifyStatusEnum.FAILED.getCode();
        }

        receiver.setIdCardVerifyStatus(verifyStatus);
        receiver.setIdCardVerifyChannel(verifyChannel);
        receiver.setIdCardVerifyTime(verifyTime);
        receiver.setIdCardVerifyRequestId(verifyRequestId);
        receiver.setIdCardVerifyLevel(verifyResult.getVerifyLevel());
        receiver.setIdCardVerifyFailCode(verifyResult.getFailCode());
        receiver.setIdCardVerifyFailReason(verifyResult.getFailReason());
        receiver.setOperatorId(operatorId);
        receiver.setOperatorName(operatorName);
        this.updateById(receiver);

        SplitReceiverVerifyLog verifyLog = new SplitReceiverVerifyLog();
        verifyLog.setLogNo(OrderNoGenerator.generateWithPrefix("VL"));
        verifyLog.setMerchantNo(receiver.getMerchantNo());
        verifyLog.setReceiverNo(receiver.getReceiverNo());
        verifyLog.setVerifyChannel(verifyChannel);
        verifyLog.setVerifyRequestId(verifyRequestId);
        verifyLog.setIdCardName(receiver.getIdCardName());
        verifyLog.setIdCardNo(receiver.getIdCardNo());
        verifyLog.setBankCardNo(receiver.getBankCardNo());
        verifyLog.setBankPhone(receiver.getBankPhone());
        verifyLog.setVerifyStatus(verifyStatus);
        verifyLog.setVerifyResult(Boolean.TRUE.equals(verifyResult.getSuccess()) ? "认证成功" : "认证失败");
        verifyLog.setVerifyFailCode(verifyResult.getFailCode());
        verifyLog.setVerifyFailReason(verifyResult.getFailReason());
        verifyLog.setVerifyTime(verifyTime);
        verifyLog.setRequestData(verifyResult.getRequestData());
        verifyLog.setResponseData(verifyResult.getResponseData());
        verifyLog.setOperatorId(operatorId);
        verifyLog.setOperatorName(operatorName);
        verifyLogMapper.insert(verifyLog);

        log.info("分账接收方身份证认证完成: receiverNo={}, verifyChannel={}, verifyStatus={}",
                receiver.getReceiverNo(), verifyChannel, verifyStatus);
    }

    @Override
    public SplitReceiver checkReceiverVerified(String receiverNo, String merchantNo) {
        LambdaQueryWrapper<SplitReceiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SplitReceiver::getReceiverNo, receiverNo)
                .eq(SplitReceiver::getMerchantNo, merchantNo);
        SplitReceiver receiver = this.getOne(wrapper);
        if (receiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账接收方不存在");
        }
        if (!SplitReceiverVerifyStatusEnum.VERIFIED.getCode().equals(receiver.getVerifyStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分账接收方未完成实名认证");
        }
        if (receiver.getStatus() == null || receiver.getStatus() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分账接收方已被禁用");
        }
        return receiver;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchImport(List<SplitReceiverBatchImportItem> items, String merchantNo, String operatorId, String operatorName) {
        return batchImport(items, false, merchantNo, operatorId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchImport(List<SplitReceiverBatchImportItem> items, Boolean autoVerify, String merchantNo, String operatorId, String operatorName) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> failDetails = new ArrayList<>();
        List<Map<String, Object>> importedReceivers = new ArrayList<>();
        Set<String> existingKeys = new HashSet<>();
        List<SplitReceiver> toVerifyReceivers = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            SplitReceiverBatchImportItem item = items.get(i);
            int rowNum = i + 1;
            try {
                StringBuilder errorMsg = new StringBuilder();

                if (StrUtil.isBlank(item.getReceiverName())) {
                    errorMsg.append("接收方名称不能为空; ");
                }
                if (item.getReceiverType() == null) {
                    errorMsg.append("接收方类型不能为空; ");
                }
                if (StrUtil.isBlank(item.getIdCardNo())) {
                    errorMsg.append("证件号码不能为空; ");
                }
                if (StrUtil.isBlank(item.getIdCardName())) {
                    errorMsg.append("证件姓名不能为空; ");
                }
                if (StrUtil.isBlank(item.getBankCardNo())) {
                    errorMsg.append("银行卡号不能为空; ");
                }
                if (StrUtil.isBlank(item.getBankPhone())) {
                    errorMsg.append("预留手机号不能为空; ");
                }
                if (StrUtil.isBlank(item.getBankName())) {
                    errorMsg.append("开户银行不能为空; ");
                }

                if (errorMsg.length() > 0) {
                    failCount++;
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("rowNum", rowNum);
                    detail.put("receiverName", item.getReceiverName());
                    detail.put("reason", errorMsg.toString());
                    failDetails.add(detail);
                    continue;
                }

                String uniqueKey = item.getIdCardNo() + "_" + item.getBankCardNo();
                if (existingKeys.contains(uniqueKey)) {
                    failCount++;
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("rowNum", rowNum);
                    detail.put("receiverName", item.getReceiverName());
                    detail.put("reason", "导入文件中存在重复的接收方（证件号+银行卡号）");
                    failDetails.add(detail);
                    continue;
                }

                LambdaQueryWrapper<SplitReceiver> existWrapper = new LambdaQueryWrapper<>();
                existWrapper.eq(SplitReceiver::getMerchantNo, merchantNo)
                        .eq(SplitReceiver::getIdCardNo, item.getIdCardNo())
                        .eq(SplitReceiver::getBankCardNo, item.getBankCardNo());
                Long existCount = this.count(existWrapper);
                if (existCount > 0) {
                    failCount++;
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("rowNum", rowNum);
                    detail.put("receiverName", item.getReceiverName());
                    detail.put("reason", "系统中已存在相同的接收方（证件号+银行卡号）");
                    failDetails.add(detail);
                    continue;
                }

                existingKeys.add(uniqueKey);

                SplitReceiver receiver = new SplitReceiver();
                receiver.setReceiverNo(OrderNoGenerator.generateWithPrefix("RE"));
                receiver.setMerchantNo(merchantNo);
                receiver.setReceiverName(item.getReceiverName());
                receiver.setReceiverType(item.getReceiverType());
                receiver.setIdCardNo(item.getIdCardNo());
                receiver.setIdCardName(item.getIdCardName());
                receiver.setBankCardNo(item.getBankCardNo());
                receiver.setBankPhone(item.getBankPhone());
                receiver.setBankName(item.getBankName());
                receiver.setBankBranchName(item.getBankBranchName());
                receiver.setContactName(item.getContactName());
                receiver.setContactPhone(item.getContactPhone());
                receiver.setContactEmail(item.getContactEmail());
                receiver.setRemark(item.getRemark());
                receiver.setStatus(1);
                receiver.setVerifyStatus(SplitReceiverVerifyStatusEnum.UNVERIFIED.getCode());
                receiver.setOperatorId(operatorId);
                receiver.setOperatorName(operatorName);
                this.save(receiver);
                successCount++;

                Map<String, Object> imported = new HashMap<>();
                imported.put("receiverNo", receiver.getReceiverNo());
                imported.put("receiverName", receiver.getReceiverName());
                importedReceivers.add(imported);

                if (Boolean.TRUE.equals(autoVerify)) {
                    toVerifyReceivers.add(receiver);
                }
            } catch (Exception e) {
                failCount++;
                Map<String, Object> detail = new HashMap<>();
                detail.put("rowNum", rowNum);
                detail.put("receiverName", item.getReceiverName());
                detail.put("reason", "系统异常: " + e.getMessage());
                failDetails.add(detail);
            }
        }

        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failDetails", failDetails);
        result.put("importedReceivers", importedReceivers);

        if (Boolean.TRUE.equals(autoVerify) && CollUtil.isNotEmpty(toVerifyReceivers)) {
            int verifySuccessCount = 0;
            int verifyFailCount = 0;
            List<Map<String, Object>> verifyFailDetails = new ArrayList<>();
            for (SplitReceiver receiver : toVerifyReceivers) {
                try {
                    doVerifyReceiverInternal(receiver, VerifyChannelEnum.BANK_CARD_FOUR.getCode(), operatorId, operatorName);
                    verifySuccessCount++;
                } catch (Exception e) {
                    verifyFailCount++;
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("receiverNo", receiver.getReceiverNo());
                    detail.put("receiverName", receiver.getReceiverName());
                    detail.put("reason", e.getMessage());
                    verifyFailDetails.add(detail);
                }
            }
            result.put("verifySuccessCount", verifySuccessCount);
            result.put("verifyFailCount", verifyFailCount);
            result.put("verifyFailDetails", verifyFailDetails);
        }

        log.info("分账接收方批量导入完成: merchantNo={}, successCount={}, failCount={}, autoVerify={}", merchantNo, successCount, failCount, autoVerify);
        return result;
    }

    @Override
    public Map<String, Object> batchImportWithFile(MultipartFile file, Boolean autoVerify, String merchantNo, String operatorId, String operatorName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件名不能为空");
        }
        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls") && !lowerName.endsWith(".csv")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持 .xlsx/.xls/.csv 格式文件");
        }

        List<SplitReceiverBatchImportItem> items = new ArrayList<>();
        try (InputStream is = file.getInputStream()) {
            ExcelReader reader;
            if (lowerName.endsWith(".csv")) {
                reader = ExcelUtil.getReader(is);
            } else {
                reader = ExcelUtil.getReader(is, 0);
            }

            List<Map<String, Object>> rows = reader.readAll();
            if (CollUtil.isEmpty(rows)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "文件内容为空");
            }

            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                SplitReceiverBatchImportItem item = new SplitReceiverBatchImportItem();
                item.setReceiverName(getCellStr(row, "接收方名称"));
                String typeStr = getCellStr(row, "接收方类型");
                if (StrUtil.isNotBlank(typeStr)) {
                    if ("1".equals(typeStr) || "个人".equals(typeStr)) {
                        item.setReceiverType(1);
                    } else if ("2".equals(typeStr) || "企业".equals(typeStr)) {
                        item.setReceiverType(2);
                    }
                }
                item.setIdCardName(getCellStr(row, "证件姓名"));
                item.setIdCardNo(getCellStr(row, "证件号码"));
                item.setBankCardNo(getCellStr(row, "银行卡号"));
                item.setBankPhone(getCellStr(row, "预留手机号"));
                item.setBankName(getCellStr(row, "开户银行"));
                item.setBankBranchName(getCellStr(row, "开户支行"));
                item.setContactName(getCellStr(row, "联系人"));
                item.setContactPhone(getCellStr(row, "联系电话"));
                item.setContactEmail(getCellStr(row, "联系邮箱"));
                item.setRemark(getCellStr(row, "备注"));
                items.add(item);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析批量导入文件失败: merchantNo={}, fileName={}", merchantNo, originalFilename, e);
            throw new BusinessException(ResultCode.PARAM_ERROR, "解析文件失败: " + e.getMessage());
        }

        return batchImport(items, autoVerify, merchantNo, operatorId, operatorName);
    }

    private String getCellStr(Map<String, Object> row, String key) {
        if (row == null || !row.containsKey(key)) {
            return null;
        }
        Object val = row.get(key);
        return val != null ? val.toString().trim() : null;
    }

    @Override
    public Map<String, Object> batchVerifyReceiver(List<String> receiverNos, String merchantNo, String operatorId, String operatorName) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> failDetails = new ArrayList<>();

        if (CollUtil.isEmpty(receiverNos)) {
            result.put("successCount", 0);
            result.put("failCount", 0);
            result.put("failDetails", failDetails);
            return result;
        }

        for (String receiverNo : receiverNos) {
            try {
                LambdaQueryWrapper<SplitReceiver> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SplitReceiver::getReceiverNo, receiverNo)
                        .eq(SplitReceiver::getMerchantNo, merchantNo);
                SplitReceiver receiver = this.getOne(wrapper);
                if (receiver == null) {
                    failCount++;
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("receiverNo", receiverNo);
                    detail.put("reason", "分账接收方不存在");
                    failDetails.add(detail);
                    continue;
                }
                doVerifyReceiverInternal(receiver, VerifyChannelEnum.SANDBOX.getCode(), operatorId, operatorName);
                successCount++;
            } catch (BusinessException e) {
                failCount++;
                Map<String, Object> detail = new HashMap<>();
                detail.put("receiverNo", receiverNo);
                detail.put("reason", e.getMessage());
                failDetails.add(detail);
            } catch (Exception e) {
                failCount++;
                Map<String, Object> detail = new HashMap<>();
                detail.put("receiverNo", receiverNo);
                detail.put("reason", "系统异常: " + e.getMessage());
                failDetails.add(detail);
            }
        }

        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failDetails", failDetails);
        log.info("分账接收方批量认证完成: merchantNo={}, successCount={}, failCount={}", merchantNo, successCount, failCount);
        return result;
    }

    @Override
    public IPage<SplitReceiverVerifyLogVO> listVerifyLogs(Long current, Long size, String merchantNo, Map<String, Object> params) {
        Page<SplitReceiverVerifyLog> page = new Page<>(current, size);
        LambdaQueryWrapper<SplitReceiverVerifyLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SplitReceiverVerifyLog::getMerchantNo, merchantNo);
        if (params != null) {
            if (params.get("receiverNo") != null && StrUtil.isNotBlank(params.get("receiverNo").toString())) {
                wrapper.eq(SplitReceiverVerifyLog::getReceiverNo, params.get("receiverNo"));
            }
            if (params.get("verifyChannel") != null) {
                wrapper.eq(SplitReceiverVerifyLog::getVerifyChannel, params.get("verifyChannel"));
            }
            if (params.get("verifyStatus") != null) {
                wrapper.eq(SplitReceiverVerifyLog::getVerifyStatus, params.get("verifyStatus"));
            }
            if (params.get("verifyRequestId") != null && StrUtil.isNotBlank(params.get("verifyRequestId").toString())) {
                wrapper.like(SplitReceiverVerifyLog::getVerifyRequestId, params.get("verifyRequestId"));
            }
        }
        wrapper.orderByDesc(SplitReceiverVerifyLog::getId);
        IPage<SplitReceiverVerifyLog> logPage = verifyLogMapper.selectPage(page, wrapper);
        return logPage.convert(this::convertToVerifyLogVO);
    }

    @Override
    public List<SplitReceiverVO> listAvailableReceivers(String merchantNo) {
        LambdaQueryWrapper<SplitReceiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SplitReceiver::getMerchantNo, merchantNo)
                .eq(SplitReceiver::getStatus, 1)
                .eq(SplitReceiver::getVerifyStatus, SplitReceiverVerifyStatusEnum.VERIFIED.getCode());
        wrapper.orderByDesc(SplitReceiver::getId);
        List<SplitReceiver> receivers = this.list(wrapper);
        return receivers.stream().map(this::convertToVO).collect(java.util.stream.Collectors.toList());
    }

    private SplitReceiverVO convertToVO(SplitReceiver receiver) {
        SplitReceiverVO vo = BeanUtil.copyProperties(receiver, SplitReceiverVO.class);
        SplitReceiverTypeEnum typeEnum = SplitReceiverTypeEnum.getByCode(receiver.getReceiverType());
        if (typeEnum != null) {
            vo.setReceiverTypeDesc(typeEnum.getDesc());
        }
        SplitReceiverVerifyStatusEnum verifyStatusEnum = SplitReceiverVerifyStatusEnum.getByCode(receiver.getVerifyStatus());
        if (verifyStatusEnum != null) {
            vo.setVerifyStatusDesc(verifyStatusEnum.getDesc());
        }
        VerifyChannelEnum channelEnum = VerifyChannelEnum.getByCode(receiver.getVerifyChannel());
        if (channelEnum != null) {
            vo.setVerifyChannelDesc(channelEnum.getDesc());
        }
        SplitReceiverVerifyStatusEnum idCardVerifyStatusEnum = SplitReceiverVerifyStatusEnum.getByCode(receiver.getIdCardVerifyStatus());
        if (idCardVerifyStatusEnum != null) {
            vo.setIdCardVerifyStatusDesc(idCardVerifyStatusEnum.getDesc());
        }
        VerifyChannelEnum idCardVerifyChannelEnum = VerifyChannelEnum.getByCode(receiver.getIdCardVerifyChannel());
        if (idCardVerifyChannelEnum != null) {
            vo.setIdCardVerifyChannelDesc(idCardVerifyChannelEnum.getDesc());
        }
        vo.setStatusDesc(receiver.getStatus() != null && receiver.getStatus() == 1 ? "启用" : "禁用");
        return vo;
    }

    private SplitReceiverVerifyLogVO convertToVerifyLogVO(SplitReceiverVerifyLog log) {
        SplitReceiverVerifyLogVO vo = BeanUtil.copyProperties(log, SplitReceiverVerifyLogVO.class);
        VerifyChannelEnum channelEnum = VerifyChannelEnum.getByCode(log.getVerifyChannel());
        if (channelEnum != null) {
            vo.setVerifyChannelDesc(channelEnum.getDesc());
        }
        SplitReceiverVerifyStatusEnum verifyStatusEnum = SplitReceiverVerifyStatusEnum.getByCode(log.getVerifyStatus());
        if (verifyStatusEnum != null) {
            vo.setVerifyStatusDesc(verifyStatusEnum.getDesc());
        }
        return vo;
    }
}
