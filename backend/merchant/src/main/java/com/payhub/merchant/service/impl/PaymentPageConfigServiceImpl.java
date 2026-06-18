package com.payhub.merchant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.merchant.dto.PaymentPageConfigSaveRequest;
import com.payhub.merchant.dto.PaymentPageConfigVO;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.entity.PaymentPageConfig;
import com.payhub.merchant.mapper.MerchantInfoMapper;
import com.payhub.merchant.mapper.PaymentPageConfigMapper;
import com.payhub.merchant.service.PaymentPageConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
public class PaymentPageConfigServiceImpl extends ServiceImpl<PaymentPageConfigMapper, PaymentPageConfig> implements PaymentPageConfigService {

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Value("${payhub.payment-page.base-url:/h5/payment}")
    private String paymentPageBaseUrl;

    private static final String DEFAULT_TEMPLATE = "DEFAULT";
    private static final String DEFAULT_PRIMARY_COLOR = "#1677ff";
    private static final String DEFAULT_BACKGROUND_COLOR = "#f5f7fa";
    private static final String DEFAULT_TEXT_COLOR = "#333333";
    private static final String DEFAULT_BUTTON_COLOR = "#1677ff";
    private static final String DEFAULT_BUTTON_TEXT_COLOR = "#ffffff";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentPageConfigVO saveConfig(PaymentPageConfigSaveRequest request) {
        MerchantInfo merchant = merchantInfoMapper.selectOne(
                new LambdaQueryWrapper<MerchantInfo>()
                        .eq(MerchantInfo::getMerchantNo, request.getMerchantNo())
                        .last("LIMIT 1")
        );
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }

        PaymentPageConfig existConfig = this.getOne(
                new LambdaQueryWrapper<PaymentPageConfig>()
                        .eq(PaymentPageConfig::getMerchantNo, request.getMerchantNo())
                        .last("LIMIT 1")
        );

        PaymentPageConfig config;
        if (existConfig != null) {
            config = existConfig;
        } else {
            config = new PaymentPageConfig();
            config.setMerchantNo(request.getMerchantNo());
        }

        if (StrUtil.isNotBlank(request.getPageTitle())) {
            config.setPageTitle(request.getPageTitle());
        } else if (StrUtil.isBlank(config.getPageTitle())) {
            config.setPageTitle(merchant.getMerchantName() + " - 收银台");
        }
        config.setLogoUrl(request.getLogoUrl());
        config.setPrimaryColor(StrUtil.isNotBlank(request.getPrimaryColor()) ? request.getPrimaryColor() : DEFAULT_PRIMARY_COLOR);
        config.setSecondaryColor(request.getSecondaryColor());
        config.setBackgroundColor(StrUtil.isNotBlank(request.getBackgroundColor()) ? request.getBackgroundColor() : DEFAULT_BACKGROUND_COLOR);
        config.setTextColor(StrUtil.isNotBlank(request.getTextColor()) ? request.getTextColor() : DEFAULT_TEXT_COLOR);
        config.setButtonColor(StrUtil.isNotBlank(request.getButtonColor()) ? request.getButtonColor() : DEFAULT_BUTTON_COLOR);
        config.setButtonTextColor(StrUtil.isNotBlank(request.getButtonTextColor()) ? request.getButtonTextColor() : DEFAULT_BUTTON_TEXT_COLOR);
        config.setTemplateCode(StrUtil.isNotBlank(request.getTemplateCode()) ? request.getTemplateCode() : DEFAULT_TEMPLATE);
        config.setCustomCss(request.getCustomCss());
        config.setFooterText(request.getFooterText());
        config.setReturnUrl(request.getReturnUrl());
        config.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        if (existConfig != null) {
            this.updateById(config);
            log.info("支付页面配置更新成功: merchantNo={}, id={}", request.getMerchantNo(), config.getId());
        } else {
            this.save(config);
            log.info("支付页面配置创建成功: merchantNo={}, id={}", request.getMerchantNo(), config.getId());
        }

        return convertToVO(config, merchant);
    }

    @Override
    public PaymentPageConfigVO getByMerchantNo(String merchantNo) {
        PaymentPageConfig config = this.getOne(
                new LambdaQueryWrapper<PaymentPageConfig>()
                        .eq(PaymentPageConfig::getMerchantNo, merchantNo)
                        .last("LIMIT 1")
        );
        if (config == null) {
            return getDefaultConfig(merchantNo);
        }
        MerchantInfo merchant = merchantInfoMapper.selectOne(
                new LambdaQueryWrapper<MerchantInfo>()
                        .eq(MerchantInfo::getMerchantNo, merchantNo)
                        .last("LIMIT 1")
        );
        return convertToVO(config, merchant);
    }

    @Override
    public PaymentPageConfigVO getById(Long id) {
        PaymentPageConfig config = this.getBaseMapper().selectById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.PAYMENT_PAGE_CONFIG_NOT_EXIST);
        }
        MerchantInfo merchant = merchantInfoMapper.selectOne(
                new LambdaQueryWrapper<MerchantInfo>()
                        .eq(MerchantInfo::getMerchantNo, config.getMerchantNo())
                        .last("LIMIT 1")
        );
        return convertToVO(config, merchant);
    }

    @Override
    public IPage<PaymentPageConfigVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<PaymentPageConfig> page = new Page<>(current, size);
        IPage<PaymentPageConfig> configPage = this.baseMapper.selectPageList(page, params);

        return configPage.convert(config -> {
            MerchantInfo merchant = merchantInfoMapper.selectOne(
                    new LambdaQueryWrapper<MerchantInfo>()
                            .eq(MerchantInfo::getMerchantNo, config.getMerchantNo())
                            .last("LIMIT 1")
            );
            return convertToVO(config, merchant);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        PaymentPageConfig config = this.getBaseMapper().selectById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.PAYMENT_PAGE_CONFIG_NOT_EXIST);
        }
        config.setStatus(status);
        this.updateById(config);
        log.info("支付页面配置状态更新: id={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        PaymentPageConfig config = this.getBaseMapper().selectById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.PAYMENT_PAGE_CONFIG_NOT_EXIST);
        }
        this.removeById(id);
        log.info("支付页面配置删除: id={}", id);
    }

    @Override
    public PaymentPageConfigVO getPublicConfig(String merchantNo) {
        PaymentPageConfig config = this.getOne(
                new LambdaQueryWrapper<PaymentPageConfig>()
                        .eq(PaymentPageConfig::getMerchantNo, merchantNo)
                        .eq(PaymentPageConfig::getStatus, 1)
                        .last("LIMIT 1")
        );
        if (config == null) {
            return getDefaultConfig(merchantNo);
        }
        MerchantInfo merchant = merchantInfoMapper.selectOne(
                new LambdaQueryWrapper<MerchantInfo>()
                        .eq(MerchantInfo::getMerchantNo, merchantNo)
                        .last("LIMIT 1")
        );
        PaymentPageConfigVO vo = convertToVO(config, merchant);
        vo.setStatus(null);
        vo.setStatusDesc(null);
        vo.setCreatedAt(null);
        vo.setUpdatedAt(null);
        return vo;
    }

    private PaymentPageConfigVO convertToVO(PaymentPageConfig config, MerchantInfo merchant) {
        PaymentPageConfigVO vo = BeanUtil.copyProperties(config, PaymentPageConfigVO.class);
        if (merchant != null) {
            vo.setMerchantName(merchant.getMerchantName());
        }
        vo.setStatusDesc(config.getStatus() == 1 ? "启用" : "禁用");
        vo.setPageUrl(paymentPageBaseUrl + "/" + config.getMerchantNo());
        return vo;
    }

    private PaymentPageConfigVO getDefaultConfig(String merchantNo) {
        MerchantInfo merchant = merchantInfoMapper.selectOne(
                new LambdaQueryWrapper<MerchantInfo>()
                        .eq(MerchantInfo::getMerchantNo, merchantNo)
                        .last("LIMIT 1")
        );
        PaymentPageConfigVO vo = new PaymentPageConfigVO();
        vo.setMerchantNo(merchantNo);
        if (merchant != null) {
            vo.setMerchantName(merchant.getMerchantName());
            vo.setPageTitle(merchant.getMerchantName() + " - 收银台");
        } else {
            vo.setPageTitle("收银台");
        }
        vo.setTemplateCode(DEFAULT_TEMPLATE);
        vo.setPrimaryColor(DEFAULT_PRIMARY_COLOR);
        vo.setBackgroundColor(DEFAULT_BACKGROUND_COLOR);
        vo.setTextColor(DEFAULT_TEXT_COLOR);
        vo.setButtonColor(DEFAULT_BUTTON_COLOR);
        vo.setButtonTextColor(DEFAULT_BUTTON_TEXT_COLOR);
        vo.setStatus(1);
        vo.setStatusDesc("启用");
        vo.setPageUrl(paymentPageBaseUrl + "/" + merchantNo);
        return vo;
    }
}
