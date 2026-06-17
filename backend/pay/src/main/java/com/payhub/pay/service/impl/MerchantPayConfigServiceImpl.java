package com.payhub.pay.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.pay.dto.PayConfigSaveRequest;
import com.payhub.pay.dto.PayConfigVO;
import com.payhub.pay.entity.MerchantPayConfig;
import com.payhub.pay.mapper.MerchantPayConfigMapper;
import com.payhub.pay.service.MerchantPayConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MerchantPayConfigServiceImpl extends ServiceImpl<MerchantPayConfigMapper, MerchantPayConfig> implements MerchantPayConfigService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(PayConfigSaveRequest request) {
        MerchantPayConfig config;
        if (request.getId() != null) {
            config = this.getById(request.getId());
            if (config == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
            }
        } else {
            LambdaQueryWrapper<MerchantPayConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MerchantPayConfig::getMerchantNo, request.getMerchantNo())
                    .eq(MerchantPayConfig::getPayChannel, request.getPayChannel())
                    .eq(MerchantPayConfig::getPayType, request.getPayType())
                    .eq(MerchantPayConfig::getChannelCode, request.getChannelCode())
                    .last("LIMIT 1");
            MerchantPayConfig existConfig = this.getOne(wrapper);
            if (existConfig != null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "该配置已存在");
            }
            config = new MerchantPayConfig();
        }

        config.setMerchantNo(request.getMerchantNo());
        config.setPayChannel(request.getPayChannel());
        config.setPayType(request.getPayType());
        config.setChannelCode(request.getChannelCode());
        config.setFeeRate(request.getFeeRate());
        config.setMinFee(request.getMinFee());
        config.setMaxFee(request.getMaxFee());
        config.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        config.setPriority(request.getPriority());
        config.setWhitelistIps(request.getWhitelistIps());
        config.setRemark(request.getRemark());

        this.saveOrUpdate(config);
        log.info("支付配置保存成功: id={}, merchantNo={}", config.getId(), request.getMerchantNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        MerchantPayConfig config = this.getById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        this.removeById(id);
        log.info("支付配置删除成功: id={}", id);
    }

    @Override
    public PayConfigVO getConfigById(Long id) {
        MerchantPayConfig config = this.getById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        return convertToVO(config);
    }

    @Override
    public List<PayConfigVO> listByMerchantNo(String merchantNo) {
        LambdaQueryWrapper<MerchantPayConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantPayConfig::getMerchantNo, merchantNo);
        List<MerchantPayConfig> configs = this.list(wrapper);
        return configs.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public IPage<PayConfigVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<MerchantPayConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<MerchantPayConfig> wrapper = new LambdaQueryWrapper<>();
        if (params != null) {
            if (params.get("merchantNo") != null) {
                wrapper.eq(MerchantPayConfig::getMerchantNo, params.get("merchantNo"));
            }
            if (params.get("payChannel") != null) {
                wrapper.eq(MerchantPayConfig::getPayChannel, params.get("payChannel"));
            }
            if (params.get("payType") != null) {
                wrapper.eq(MerchantPayConfig::getPayType, params.get("payType"));
            }
            if (params.get("status") != null) {
                wrapper.eq(MerchantPayConfig::getStatus, params.get("status"));
            }
        }
        wrapper.orderByDesc(MerchantPayConfig::getPriority, MerchantPayConfig::getId);
        IPage<MerchantPayConfig> configPage = this.page(page, wrapper);
        return configPage.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleConfig(Long id) {
        MerchantPayConfig config = this.getById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        config.setStatus(config.getStatus() == 1 ? 0 : 1);
        this.updateById(config);
        log.info("支付配置状态切换成功: id={}, status={}", id, config.getStatus());
    }

    private PayConfigVO convertToVO(MerchantPayConfig config) {
        PayConfigVO vo = BeanUtil.copyProperties(config, PayConfigVO.class);
        vo.setStatusDesc(config.getStatus() == 1 ? "启用" : "禁用");
        return vo;
    }
}
