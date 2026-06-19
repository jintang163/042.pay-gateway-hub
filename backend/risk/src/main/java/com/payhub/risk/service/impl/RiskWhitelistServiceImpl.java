package com.payhub.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.risk.dto.RiskListSaveRequest;
import com.payhub.risk.dto.RiskListVO;
import com.payhub.risk.entity.RiskWhitelist;
import com.payhub.risk.mapper.RiskWhitelistMapper;
import com.payhub.risk.service.RiskWhitelistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RiskWhitelistServiceImpl extends ServiceImpl<RiskWhitelistMapper, RiskWhitelist> implements RiskWhitelistService {

    private static final String WHITELIST_KEY_PREFIX = "risk:whitelist:";
    private static final String GLOBAL_MERCHANT = "GLOBAL";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToWhitelist(RiskListSaveRequest request) {
        if (StrUtil.isBlank(request.getListType())) {
            throw new BusinessException("名单类型不能为空");
        }
        if (StrUtil.isBlank(request.getListValue())) {
            throw new BusinessException("名单值不能为空");
        }

        String merchantNo = request.getMerchantNo();

        RiskWhitelist exist = this.getOne(new LambdaQueryWrapper<RiskWhitelist>()
                .eq(StrUtil.isNotBlank(merchantNo), RiskWhitelist::getMerchantNo, merchantNo)
                .isNull(StrUtil.isBlank(merchantNo), RiskWhitelist::getMerchantNo)
                .eq(RiskWhitelist::getListType, request.getListType())
                .eq(RiskWhitelist::getListValue, request.getListValue())
                .eq(RiskWhitelist::getDeleted, 0));
        if (exist != null) {
            throw new BusinessException("该白名单已存在");
        }

        RiskWhitelist whitelist = new RiskWhitelist();
        whitelist.setMerchantNo(merchantNo);
        whitelist.setListType(request.getListType());
        whitelist.setListValue(request.getListValue());
        whitelist.setListSource(request.getListSource());
        whitelist.setBypassRules(request.getBypassRules());
        whitelist.setReason(request.getReason());
        whitelist.setStatus(1);
        whitelist.setExpireTime(request.getExpireTime());
        this.save(whitelist);

        String cacheKey = buildCacheKey(merchantNo, request.getListType(), request.getListValue());
        redisTemplate.opsForValue().set(cacheKey, whitelist, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        log.info("添加白名单成功，商户：{}，类型：{}，值：{}", merchantNo, request.getListType(), request.getListValue());
    }

    private String buildCacheKey(String merchantNo, String listType, String listValue) {
        String merchantKey = StrUtil.isNotBlank(merchantNo) ? merchantNo : GLOBAL_MERCHANT;
        return WHITELIST_KEY_PREFIX + merchantKey + ":" + listType + ":" + listValue;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFromWhitelist(Long id) {
        RiskWhitelist whitelist = this.getById(id);
        if (whitelist == null || whitelist.getDeleted() == 1) {
            throw new BusinessException("白名单不存在");
        }

        this.removeById(id);

        String cacheKey = buildCacheKey(whitelist.getMerchantNo(), whitelist.getListType(), whitelist.getListValue());
        redisTemplate.delete(cacheKey);

        log.info("移除白名单成功，ID：{}，商户：{}，类型：{}，值：{}",
                id, whitelist.getMerchantNo(), whitelist.getListType(), whitelist.getListValue());
    }

    @Override
    public RiskWhitelist isWhitelisted(String listType, String listValue) {
        return isWhitelisted(null, listType, listValue);
    }

    @Override
    public RiskWhitelist isWhitelisted(String merchantNo, String listType, String listValue) {
        if (StrUtil.isBlank(listType) || StrUtil.isBlank(listValue)) {
            return null;
        }

        RiskWhitelist whitelist = queryWhitelistFromCache(merchantNo, listType, listValue);
        if (whitelist == null && StrUtil.isNotBlank(merchantNo)) {
            whitelist = queryWhitelistFromCache(null, listType, listValue);
        }

        return whitelist;
    }

    private RiskWhitelist queryWhitelistFromCache(String merchantNo, String listType, String listValue) {
        String cacheKey = buildCacheKey(merchantNo, listType, listValue);
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null && cachedObj instanceof RiskWhitelist) {
            RiskWhitelist cached = (RiskWhitelist) cachedObj;
            if (cached.getStatus() != null && cached.getStatus() != 1) {
                return null;
            }
            if (cached.getExpireTime() != null && cached.getExpireTime().isBefore(LocalDateTime.now())) {
                redisTemplate.delete(cacheKey);
                return null;
            }
            return cached;
        }

        RiskWhitelist whitelist = this.getOne(new LambdaQueryWrapper<RiskWhitelist>()
                .eq(StrUtil.isNotBlank(merchantNo), RiskWhitelist::getMerchantNo, merchantNo)
                .isNull(StrUtil.isBlank(merchantNo), RiskWhitelist::getMerchantNo)
                .eq(RiskWhitelist::getListType, listType)
                .eq(RiskWhitelist::getListValue, listValue)
                .eq(RiskWhitelist::getDeleted, 0));

        if (whitelist != null) {
            if (whitelist.getStatus() != null && whitelist.getStatus() != 1) {
                return null;
            }
            if (whitelist.getExpireTime() != null && whitelist.getExpireTime().isBefore(LocalDateTime.now())) {
                return null;
            }
            redisTemplate.opsForValue().set(cacheKey, whitelist, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }

        return whitelist;
    }

    @Override
    public boolean checkInList(String listType, String listValue) {
        return checkInList(null, listType, listValue);
    }

    @Override
    public boolean checkInList(String merchantNo, String listType, String listValue) {
        return isWhitelisted(merchantNo, listType, listValue) != null;
    }

    @Override
    public RiskWhitelist getByTypeAndValue(String listType, String listValue) {
        return getByTypeAndValue(null, listType, listValue);
    }

    @Override
    public RiskWhitelist getByTypeAndValue(String merchantNo, String listType, String listValue) {
        return isWhitelisted(merchantNo, listType, listValue);
    }

    @Override
    public boolean checkWhitelistBypass(String listType, String listValue, String ruleCode) {
        return checkWhitelistBypass(null, listType, listValue, ruleCode);
    }

    @Override
    public boolean checkWhitelistBypass(String merchantNo, String listType, String listValue, String ruleCode) {
        RiskWhitelist whitelist = this.isWhitelisted(merchantNo, listType, listValue);
        if (whitelist == null) {
            return false;
        }

        String bypassRules = whitelist.getBypassRules();
        if (StrUtil.isBlank(bypassRules)) {
            return true;
        }
        if ("*".equals(bypassRules.trim())) {
            return true;
        }
        if (StrUtil.isBlank(ruleCode)) {
            return false;
        }

        List<String> ruleList = Arrays.asList(bypassRules.split(","));
        return ruleList.stream().anyMatch(r -> r.trim().equals(ruleCode.trim()));
    }

    @Override
    public IPage<RiskListVO> listPage(Long current, Long size, Map<String, Object> params) {
        LambdaQueryWrapper<RiskWhitelist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskWhitelist::getDeleted, 0);

        if (params != null) {
            if (params.get("merchantNo") != null && StrUtil.isNotBlank(params.get("merchantNo").toString())) {
                wrapper.eq(RiskWhitelist::getMerchantNo, params.get("merchantNo").toString());
            }
            if (params.get("listType") != null && StrUtil.isNotBlank(params.get("listType").toString())) {
                wrapper.eq(RiskWhitelist::getListType, params.get("listType").toString());
            }
            if (params.get("listValue") != null && StrUtil.isNotBlank(params.get("listValue").toString())) {
                wrapper.like(RiskWhitelist::getListValue, params.get("listValue").toString());
            }
            if (params.get("status") != null) {
                wrapper.eq(RiskWhitelist::getStatus, Integer.parseInt(params.get("status").toString()));
            }
        }

        wrapper.orderByDesc(RiskWhitelist::getCreatedAt);

        IPage<RiskWhitelist> page = this.page(new Page<>(current, size), wrapper);
        return page.convert(this::convertToVO);
    }

    private RiskListVO convertToVO(RiskWhitelist entity) {
        RiskListVO vo = new RiskListVO();
        vo.setId(entity.getId());
        vo.setMerchantNo(entity.getMerchantNo());
        vo.setListType(entity.getListType());
        vo.setListValue(entity.getListValue());
        vo.setListSource(entity.getListSource());
        vo.setReason(entity.getReason());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(entity.getStatus() != null && entity.getStatus() == 1 ? "启用" : "禁用");
        vo.setExpireTime(entity.getExpireTime());
        vo.setBypassRules(entity.getBypassRules());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
