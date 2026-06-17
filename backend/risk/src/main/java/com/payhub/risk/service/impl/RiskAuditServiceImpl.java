package com.payhub.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.utils.IdGenerator;
import com.payhub.common.utils.SmsUtil;
import com.payhub.common.utils.SnowflakeIdUtil;
import com.payhub.risk.dto.RiskAuditRequest;
import com.payhub.risk.dto.SmsVerifyRequest;
import com.payhub.risk.entity.RiskAuditRecord;
import com.payhub.risk.entity.RiskControlLog;
import com.payhub.risk.enums.AuditStatusEnum;
import com.payhub.risk.mapper.RiskAuditRecordMapper;
import com.payhub.risk.mapper.RiskControlLogMapper;
import com.payhub.risk.service.RiskAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RiskAuditServiceImpl extends ServiceImpl<RiskAuditRecordMapper, RiskAuditRecord> implements RiskAuditService {

    private static final String SMS_CODE_KEY_PREFIX = "risk:sms_code:";
    private static final long SMS_CODE_EXPIRE_MINUTES = 5;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RiskControlLogMapper riskControlLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskAuditRecord createAudit(Long riskLogId, String auditType, Integer auditLevel) {
        if (riskLogId == null) {
            throw new BusinessException("风控日志ID不能为空");
        }

        RiskControlLog riskLog = riskControlLogMapper.selectById(riskLogId);
        if (riskLog == null) {
            throw new BusinessException("风控日志不存在");
        }

        RiskAuditRecord existRecord = getByRiskLogId(riskLogId);
        if (existRecord != null) {
            return existRecord;
        }

        RiskAuditRecord record = new RiskAuditRecord();
        record.setAuditNo("AUD" + SnowflakeIdUtil.nextIdStr());
        record.setRiskLogId(riskLogId);
        record.setMerchantNo(riskLog.getMerchantNo());
        record.setOrderNo(riskLog.getOrderNo());
        record.setAuditType(auditType != null ? auditType : "MANUAL");
        record.setAuditLevel(auditLevel != null ? auditLevel : 2);
        record.setAuditStatus(AuditStatusEnum.PENDING.getCode());
        record.setRiskLevelBefore(riskLog.getRiskLevel());
        record.setSmsVerified(0);
        this.save(record);

        LambdaUpdateWrapper<RiskControlLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(RiskControlLog::getId, riskLogId)
                .set(RiskControlLog::getAuditStatus, AuditStatusEnum.PENDING.getCode())
                .set(RiskControlLog::getAuditId, record.getId());
        riskControlLogMapper.update(null, updateWrapper);

        log.info("创建人工审核记录成功，auditNo：{}，riskLogId：{}", record.getAuditNo(), riskLogId);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskAuditRecord audit(RiskAuditRequest request, String auditUserId, String auditUserName) {
        if (request == null || request.getRiskLogId() == null) {
            throw new BusinessException("审核参数不能为空");
        }
        if (StrUtil.isBlank(request.getAuditResult())) {
            throw new BusinessException("审核结果不能为空");
        }

        RiskAuditRecord record = getByRiskLogId(request.getRiskLogId());
        if (record == null) {
            throw new BusinessException("审核记录不存在");
        }
        if (!AuditStatusEnum.PENDING.getCode().equals(record.getAuditStatus())) {
            throw new BusinessException("该记录已审核，无需重复审核");
        }

        Integer newStatus = "APPROVED".equalsIgnoreCase(request.getAuditResult())
                ? AuditStatusEnum.APPROVED.getCode()
                : AuditStatusEnum.REJECTED.getCode();

        record.setAuditStatus(newStatus);
        record.setAuditResult(request.getAuditResult());
        record.setAuditRemark(request.getAuditRemark());
        record.setAuditUserId(auditUserId);
        record.setAuditUserName(auditUserName);
        record.setAuditTime(LocalDateTime.now());
        record.setRiskLevelAfter(AuditStatusEnum.APPROVED.getCode().equals(newStatus) ? 0 : record.getRiskLevelBefore());
        this.updateById(record);

        LambdaUpdateWrapper<RiskControlLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(RiskControlLog::getId, request.getRiskLogId())
                .set(RiskControlLog::getAuditStatus, newStatus);
        riskControlLogMapper.update(null, updateWrapper);

        log.info("审核完成，auditId：{}，结果：{}，审核人：{}", record.getId(), request.getAuditResult(), auditUserName);
        return record;
    }

    @Override
    public IPage<RiskAuditRecord> listPage(Long current, Long size, Map<String, Object> params) {
        LambdaQueryWrapper<RiskAuditRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAuditRecord::getDeleted, 0);

        if (params != null) {
            if (params.get("auditNo") != null && StrUtil.isNotBlank(params.get("auditNo").toString())) {
                wrapper.like(RiskAuditRecord::getAuditNo, params.get("auditNo").toString());
            }
            if (params.get("merchantNo") != null && StrUtil.isNotBlank(params.get("merchantNo").toString())) {
                wrapper.eq(RiskAuditRecord::getMerchantNo, params.get("merchantNo").toString());
            }
            if (params.get("orderNo") != null && StrUtil.isNotBlank(params.get("orderNo").toString())) {
                wrapper.eq(RiskAuditRecord::getOrderNo, params.get("orderNo").toString());
            }
            if (params.get("auditStatus") != null) {
                wrapper.eq(RiskAuditRecord::getAuditStatus, Integer.parseInt(params.get("auditStatus").toString()));
            }
            if (params.get("auditLevel") != null) {
                wrapper.eq(RiskAuditRecord::getAuditLevel, Integer.parseInt(params.get("auditLevel").toString()));
            }
            if (params.get("auditType") != null && StrUtil.isNotBlank(params.get("auditType").toString())) {
                wrapper.eq(RiskAuditRecord::getAuditType, params.get("auditType").toString());
            }
            if (params.get("auditUserId") != null && StrUtil.isNotBlank(params.get("auditUserId").toString())) {
                wrapper.eq(RiskAuditRecord::getAuditUserId, params.get("auditUserId").toString());
            }
        }

        wrapper.orderByDesc(RiskAuditRecord::getCreatedAt);
        return this.page(new Page<>(current, size), wrapper);
    }

    @Override
    public RiskAuditRecord getByRiskLogId(Long riskLogId) {
        if (riskLogId == null) {
            return null;
        }
        LambdaQueryWrapper<RiskAuditRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAuditRecord::getRiskLogId, riskLogId)
                .eq(RiskAuditRecord::getDeleted, 0);
        return this.getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String sendSmsCode(Long auditId, String mobile) {
        if (auditId == null) {
            throw new BusinessException("审核ID不能为空");
        }
        if (StrUtil.isBlank(mobile)) {
            throw new BusinessException("手机号不能为空");
        }

        RiskAuditRecord record = this.getById(auditId);
        if (record == null || record.getDeleted() == 1) {
            throw new BusinessException("审核记录不存在");
        }

        String code = SmsUtil.generateSmsCode();
        String key = SMS_CODE_KEY_PREFIX + auditId;
        redisTemplate.opsForValue().set(key, code, SMS_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        SmsUtil.sendSms(mobile, code);
        log.info("发送短信验证码，auditId：{}，mobile：{}，code：{}", auditId, mobile, code);

        record.setSmsMobile(mobile);
        this.updateById(record);

        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifySmsCode(SmsVerifyRequest request) {
        if (request == null) {
            throw new BusinessException("验证参数不能为空");
        }
        if (StrUtil.isBlank(request.getMobile())) {
            throw new BusinessException("手机号不能为空");
        }
        if (StrUtil.isBlank(request.getCode())) {
            throw new BusinessException("验证码不能为空");
        }
        if (request.getRiskLogId() == null) {
            throw new BusinessException("风控日志ID不能为空");
        }

        RiskAuditRecord record = getByRiskLogId(request.getRiskLogId());
        if (record == null) {
            throw new BusinessException("审核记录不存在");
        }

        String key = SMS_CODE_KEY_PREFIX + record.getId();
        Object cachedCodeObj = redisTemplate.opsForValue().get(key);
        if (cachedCodeObj == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }

        boolean valid = SmsUtil.verifySmsCode(request.getMobile(), request.getCode(), cachedCodeObj.toString());
        if (valid) {
            record.setSmsVerified(1);
            record.setSmsVerifyTime(LocalDateTime.now());
            this.updateById(record);
            redisTemplate.delete(key);
            log.info("短信验证码验证通过，auditId：{}，mobile：{}", record.getId(), request.getMobile());
        } else {
            log.warn("短信验证码验证失败，auditId：{}，mobile：{}，输入code：{}，缓存code：{}",
                    record.getId(), request.getMobile(), request.getCode(), cachedCodeObj);
        }

        return valid;
    }
}
