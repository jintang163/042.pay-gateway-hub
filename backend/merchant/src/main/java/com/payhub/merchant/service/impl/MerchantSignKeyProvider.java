package com.payhub.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payhub.common.crypto.SignKeyProvider;
import com.payhub.common.enums.SignTypeEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.Sm4Util;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.mapper.MerchantInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantSignKeyProvider implements SignKeyProvider {

    private final MerchantInfoMapper merchantInfoMapper;

    @Override
    public String getSignKey(String merchantNo, String signType) {
        MerchantInfo merchant = getMerchantInfo(merchantNo);

        SignTypeEnum signTypeEnum = SignTypeEnum.getByCode(signType);
        if (signTypeEnum == null) {
            log.warn("不支持的签名类型, signType: {}", signType);
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的签名类型");
        }

        String signKey;
        switch (signTypeEnum) {
            case MD5:
                signKey = Sm4Util.decrypt(merchant.getApiKeyMd5());
                break;
            case RSA:
                signKey = Sm4Util.decrypt(merchant.getApiKeyRsaPrivate());
                break;
            case SM2:
                signKey = Sm4Util.decrypt(merchant.getApiKeySm2Private());
                break;
            default:
                throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的签名类型");
        }

        if (signKey == null || signKey.isEmpty()) {
            log.warn("商户签名密钥为空, merchantNo: {}, signType: {}", merchantNo, signType);
            throw new BusinessException(ResultCode.SIGN_VERIFY_ERROR, "商户签名密钥未配置");
        }

        return signKey;
    }

    @Override
    public String getPublicKey(String merchantNo, String signType) {
        MerchantInfo merchant = getMerchantInfo(merchantNo);

        SignTypeEnum signTypeEnum = SignTypeEnum.getByCode(signType);
        if (signTypeEnum == null) {
            log.warn("不支持的签名类型, signType: {}", signType);
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的签名类型");
        }

        String publicKey;
        switch (signTypeEnum) {
            case RSA:
                publicKey = merchant.getApiKeyRsaPublic();
                break;
            case SM2:
                publicKey = merchant.getApiKeySm2Public();
                break;
            default:
                throw new BusinessException(ResultCode.PARAM_ERROR, "该签名类型不支持公钥");
        }

        if (publicKey == null || publicKey.isEmpty()) {
            log.warn("商户公钥为空, merchantNo: {}, signType: {}", merchantNo, signType);
            throw new BusinessException(ResultCode.SIGN_VERIFY_ERROR, "商户公钥未配置");
        }

        return publicKey;
    }

    private MerchantInfo getMerchantInfo(String merchantNo) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, merchantNo)
                .last("LIMIT 1");
        MerchantInfo merchant = merchantInfoMapper.selectOne(wrapper);

        if (merchant == null) {
            log.warn("商户不存在, merchantNo: {}", merchantNo);
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }
        return merchant;
    }
}
