package com.payhub.merchant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.*;
import com.payhub.merchant.dto.*;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.mapper.MerchantInfoMapper;
import com.payhub.merchant.service.MerchantInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MerchantInfoServiceImpl extends ServiceImpl<MerchantInfoMapper, MerchantInfo> implements MerchantInfoService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String SMS_CODE_CACHE_KEY = "payhub:sms:reset_api_key:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String apply(MerchantApplyRequest request) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getBusinessLicenseNo, request.getBusinessLicenseNo());
        Long count = this.count(wrapper);
        if (count > 0) {
            throw new BusinessException(ResultCode.MERCHANT_APPLY_EXIST);
        }

        String merchantNo = SnowflakeIdUtil.generateMerchantNo();

        MerchantInfo merchant = new MerchantInfo();
        merchant.setMerchantNo(merchantNo);
        merchant.setMerchantName(request.getMerchantName());
        merchant.setBusinessLicenseNo(request.getBusinessLicenseNo());
        merchant.setLegalPersonName(request.getLegalPersonName());
        merchant.setLegalPersonIdNo(Sm4Util.encrypt(request.getLegalPersonIdNo()));
        merchant.setContactPhone(request.getContactPhone());
        merchant.setContactEmail(request.getContactEmail());
        merchant.setSettlementBankName(request.getSettlementBankName());
        merchant.setSettlementBankAccount(Sm4Util.encrypt(request.getSettlementBankAccount()));
        merchant.setSettlementAccountName(request.getSettlementAccountName());
        merchant.setAuditStatus(0);
        merchant.setStatus(1);

        this.save(merchant);

        log.info("商户入驻申请提交成功: merchantNo={}, merchantName={}", merchantNo, request.getMerchantName());
        return merchantNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(MerchantAuditRequest request) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, request.getMerchantNo())
                .last("LIMIT 1");
        MerchantInfo merchant = this.getOne(wrapper);
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }

        merchant.setAuditStatus(request.getAuditStatus());
        merchant.setAuditRemark(request.getAuditRemark());

        if (request.getAuditStatus() == 1) {
            String md5Key = SignUtil.generateMd5Key();
            SignUtil.RsaKeyPair rsaKeyPair = SignUtil.generateRsaKeyPair();

            merchant.setApiKeyMd5(Sm4Util.encrypt(md5Key));
            merchant.setApiKeyRsaPublic(rsaKeyPair.getPublicKey());
            merchant.setApiKeyRsaPrivate(Sm4Util.encrypt(rsaKeyPair.getPrivateKey()));
        }

        this.updateById(merchant);

        log.info("商户审核完成: merchantNo={}, auditStatus={}", request.getMerchantNo(), request.getAuditStatus());
    }

    @Override
    public MerchantVO getByMerchantNo(String merchantNo) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, merchantNo)
                .last("LIMIT 1");
        MerchantInfo merchant = this.getOne(wrapper);
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }
        return convertToVO(merchant);
    }

    @Override
    public IPage<MerchantVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<MerchantInfo> page = new Page<>(current, size);
        IPage<MerchantInfo> infoPage = this.baseMapper.selectPageList(page, params);

        return infoPage.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyVO resetApiKey(ApiKeyResetRequest request) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, request.getMerchantNo())
                .last("LIMIT 1");
        MerchantInfo merchant = this.getOne(wrapper);
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }

        String cacheKey = SMS_CODE_CACHE_KEY + request.getMerchantNo();
        String cachedCode = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null || !cachedCode.equals(request.getSmsCode())) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }
        stringRedisTemplate.delete(cacheKey);

        String md5Key = SignUtil.generateMd5Key();
        SignUtil.RsaKeyPair rsaKeyPair = SignUtil.generateRsaKeyPair();
        SignUtil.Sm2KeyPair sm2KeyPair = SignUtil.generateSm2KeyPair();

        merchant.setApiKeyMd5(Sm4Util.encrypt(md5Key));
        merchant.setApiKeyRsaPublic(rsaKeyPair.getPublicKey());
        merchant.setApiKeyRsaPrivate(Sm4Util.encrypt(rsaKeyPair.getPrivateKey()));

        this.updateById(merchant);

        ApiKeyVO vo = new ApiKeyVO();
        vo.setMerchantNo(request.getMerchantNo());
        vo.setMd5Key(md5Key);
        vo.setRsaPublicKey(rsaKeyPair.getPublicKey());
        vo.setRsaPrivateKey(rsaKeyPair.getPrivateKey());
        vo.setSm2PublicKey(sm2KeyPair.getPublicKey());
        vo.setSm2PrivateKey(sm2KeyPair.getPrivateKey());

        log.info("商户密钥重置成功: merchantNo={}, signType={}", request.getMerchantNo(), request.getSignType());
        return vo;
    }

    @Override
    public boolean testCallback(String merchantNo, String url) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, merchantNo)
                .last("LIMIT 1");
        MerchantInfo merchant = this.getOne(wrapper);
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }
        if (merchant.getAuditStatus() != 1) {
            throw new BusinessException(ResultCode.MERCHANT_AUDIT_PENDING);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("testId", IdUtil.fastSimpleUUID());
        params.put("timestamp", System.currentTimeMillis());
        params.put("amount", 100);
        params.put("orderNo", "TEST" + System.currentTimeMillis());

        String signType = "MD5";
        String md5Key = Sm4Util.decrypt(merchant.getApiKeyMd5());
        String rsaPublicKey = merchant.getApiKeyRsaPublic();
        String rsaPrivateKey = Sm4Util.decrypt(merchant.getApiKeyRsaPrivate());

        String sign = SignUtil.sign(params, signType, md5Key, rsaPrivateKey, null);
        params.put("sign", sign);
        params.put("signType", signType);

        log.info("回调测试请求: url={}, params={}", url, params);
        String respStr = HttpUtil.postJson(url, params);
        log.info("回调测试响应: url={}, resp={}", url, respStr);

        if (StrUtil.isBlank(respStr)) {
            throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调请求无响应");
        }

        try {
            JSONObject resp = JSON.parseObject(respStr);
            String respSign = resp.getString("sign");
            String respSignType = resp.getString("signType");
            if (StrUtil.isBlank(respSign)) {
                throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调响应缺少签名");
            }

            Map<String, Object> respParams = new HashMap<>();
            for (String key : resp.keySet()) {
                if (!"sign".equals(key) && !"signType".equals(key)) {
                    respParams.put(key, resp.get(key));
                }
            }

            String currentSignType = StrUtil.isNotBlank(respSignType) ? respSignType : signType;
            boolean verifyResult = SignUtil.verify(respParams, currentSignType, respSign, md5Key, rsaPublicKey, null);

            if (!verifyResult) {
                throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调响应签名验证失败");
            }

            log.info("回调测试成功: merchantNo={}, url={}", merchantNo, url);
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("回调测试异常", e);
            throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调响应解析失败: " + e.getMessage());
        }
    }

    private MerchantVO convertToVO(MerchantInfo merchant) {
        MerchantVO vo = BeanUtil.copyProperties(merchant, MerchantVO.class);
        vo.setSettlementBankAccount(maskBankAccount(merchant.getSettlementBankAccount()));
        vo.setAuditStatusDesc(getAuditStatusDesc(merchant.getAuditStatus()));
        vo.setStatusDesc(merchant.getStatus() == 1 ? "启用" : "禁用");
        return vo;
    }

    private String maskBankAccount(String account) {
        if (StrUtil.isBlank(account)) {
            return "";
        }
        String decrypted = Sm4Util.decrypt(account);
        if (decrypted.length() <= 8) {
            return decrypted;
        }
        return decrypted.substring(0, 4) + "****" + decrypted.substring(decrypted.length() - 4);
    }

    private String getAuditStatusDesc(Integer auditStatus) {
        if (auditStatus == null) {
            return "";
        }
        switch (auditStatus) {
            case 0:
                return "待审核";
            case 1:
                return "审核通过";
            case 2:
                return "审核拒绝";
            default:
                return "";
        }
    }
}
