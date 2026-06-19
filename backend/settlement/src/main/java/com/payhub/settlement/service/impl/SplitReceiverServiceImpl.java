package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.settlement.dto.SplitReceiverBatchImportItem;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (SplitReceiverVerifyStatusEnum.VERIFYING.getCode().equals(receiver.getVerifyStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "接收方正在认证中，请稍后再试");
        }
        if (SplitReceiverVerifyStatusEnum.VERIFIED.getCode().equals(receiver.getVerifyStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "接收方已认证，无需重复认证");
        }

        String verifyRequestId = OrderNoGenerator.generateWithPrefix("VR");
        LocalDateTime verifyTime = LocalDateTime.now();
        Integer verifyStatus;
        String verifyFailReason = null;
        String verifyResult;

        String bankCardNo = receiver.getBankCardNo();
        if (bankCardNo != null && bankCardNo.endsWith("0000")) {
            verifyStatus = SplitReceiverVerifyStatusEnum.VERIFIED.getCode();
            verifyResult = "认证成功";
        } else if (bankCardNo != null && bankCardNo.endsWith("9999")) {
            verifyStatus = SplitReceiverVerifyStatusEnum.FAILED.getCode();
            verifyFailReason = "银行卡四要素核验失败，信息不匹配";
            verifyResult = "认证失败";
        } else {
            verifyStatus = SplitReceiverVerifyStatusEnum.VERIFYING.getCode();
            verifyResult = "认证处理中";
        }

        receiver.setVerifyStatus(verifyStatus);
        receiver.setVerifyChannel(request.getVerifyChannel());
        receiver.setVerifyTime(verifyTime);
        receiver.setVerifyRequestId(verifyRequestId);
        receiver.setVerifyFailReason(verifyFailReason);
        receiver.setOperatorId(operatorId);
        receiver.setOperatorName(operatorName);
        this.updateById(receiver);

        SplitReceiverVerifyLog verifyLog = new SplitReceiverVerifyLog();
        verifyLog.setLogNo(OrderNoGenerator.generateWithPrefix("VL"));
        verifyLog.setMerchantNo(merchantNo);
        verifyLog.setReceiverNo(request.getReceiverNo());
        verifyLog.setVerifyChannel(request.getVerifyChannel());
        verifyLog.setVerifyRequestId(verifyRequestId);
        verifyLog.setIdCardName(receiver.getIdCardName());
        verifyLog.setIdCardNo(receiver.getIdCardNo());
        verifyLog.setBankCardNo(receiver.getBankCardNo());
        verifyLog.setBankPhone(receiver.getBankPhone());
        verifyLog.setVerifyStatus(verifyStatus);
        verifyLog.setVerifyResult(verifyResult);
        verifyLog.setVerifyFailReason(verifyFailReason);
        verifyLog.setVerifyTime(verifyTime);
        verifyLog.setOperatorId(operatorId);
        verifyLog.setOperatorName(operatorName);
        verifyLogMapper.insert(verifyLog);

        log.info("分账接收方实名认证完成: receiverNo={}, verifyStatus={}, verifyRequestId={}", request.getReceiverNo(), verifyStatus, verifyRequestId);
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
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> failDetails = new ArrayList<>();
        Set<String> existingKeys = new HashSet<>();

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
        log.info("分账接收方批量导入完成: merchantNo={}, successCount={}, failCount={}", merchantNo, successCount, failCount);
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
