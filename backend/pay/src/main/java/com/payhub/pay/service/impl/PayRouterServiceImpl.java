package com.payhub.pay.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payhub.pay.entity.MerchantPayConfig;
import com.payhub.pay.service.MerchantPayConfigService;
import com.payhub.pay.service.PayRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class PayRouterServiceImpl implements PayRouterService {

    @Autowired
    private MerchantPayConfigService merchantPayConfigService;

    @Override
    public MerchantPayConfig selectChannel(String merchantNo, String payChannel, String payType, BigDecimal amount, String clientIp) {
        LambdaQueryWrapper<MerchantPayConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantPayConfig::getMerchantNo, merchantNo)
                .eq(MerchantPayConfig::getPayChannel, payChannel)
                .eq(MerchantPayConfig::getPayType, payType)
                .eq(MerchantPayConfig::getStatus, 1);

        List<MerchantPayConfig> configs = merchantPayConfigService.list(wrapper);
        if (configs == null || configs.isEmpty()) {
            return null;
        }

        configs = configs.stream()
                .filter(config -> isIpWhitelisted(config.getWhitelistIps(), clientIp))
                .sorted(Comparator.comparing(MerchantPayConfig::getPriority, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MerchantPayConfig::getFeeRate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (configs.isEmpty()) {
            return null;
        }

        MerchantPayConfig selected = configs.get(0);
        log.info("智能路由选择结果: merchantNo={}, payChannel={}, payType={}, selectedChannel={}",
                merchantNo, payChannel, payType, selected.getChannelCode());
        return selected;
    }

    private boolean isIpWhitelisted(String whitelistIps, String clientIp) {
        if (StrUtil.isBlank(whitelistIps)) {
            return true;
        }
        if (StrUtil.isBlank(clientIp)) {
            return false;
        }
        String[] ips = whitelistIps.split(",");
        for (String ip : ips) {
            if (ip.trim().equals(clientIp.trim())) {
                return true;
            }
            if (ip.trim().endsWith(".*")) {
                String prefix = ip.trim().substring(0, ip.trim().length() - 2);
                if (clientIp.trim().startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }
}
