package com.payhub.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.risk.dto.RiskListSaveRequest;
import com.payhub.risk.dto.RiskListVO;
import com.payhub.risk.entity.RiskBlacklist;
import com.payhub.risk.mapper.RiskBlacklistMapper;
import com.payhub.risk.service.RiskBlacklistService;
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
public class RiskBlacklistServiceImpl extends ServiceImpl<RiskBlacklistMapper, RiskBlacklist> implements RiskBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "risk:blacklist:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public IPage<RiskListVO> listPage(Long current, Long size, Map<String, Object> params) {
        LambdaQueryWrapper<RiskBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskBlacklist::getDeleted, 0);

        if (params != null) {
            if (params.get("listType") != null && StrUtil.isNotBlank(params.get("listType").toString())) {
                wrapper.eq(RiskBlacklist::getListType, params.get("listType").toString());
            }
            if (params.get("listValue") != null && StrUtil.isNotBlank(params.get("listValue").toString())) {
                wrapper.like(RiskBlacklist::getListValue, params.get("listValue").toString());
            }
            if (params.get("status") != null) {
                wrapper.eq(RiskBlacklist::getStatus, Integer.parseInt(params.get("status").toString()));
            }
            if (params.get("riskLevel") != null) {
                wrapper.eq(RiskBlacklist::getRiskLevel, Integer.parseInt(params.get("riskLevel").toString()));
            }
        }

        wrapper.orderByDesc(RiskBlacklist::getCreatedAt);

        IPage<RiskBlacklist> page = this.page(new Page<>(current, size), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToBlacklist(RiskListSaveRequest request) {
        if (StrUtil.isBlank(request.getListType())) {
            throw new BusinessException("名单类型不能为空");
        }
        if (StrUtil.isBlank(request.getListValue())) {
            throw new BusinessException("名单值不能为空");
        }

        RiskBlacklist exist = this.getOne(new LambdaQueryWrapper<RiskBlacklist>()
                .eq(RiskBlacklist::getListType, request.getListType())
                .eq(RiskBlacklist::getListValue, request.getListValue())
                .eq(RiskBlacklist::getDeleted, 0));
        if (exist != null) {
            throw new BusinessException("该黑名单已存在");
        }

        RiskBlacklist blacklist = new RiskBlacklist();
        blacklist.setListType(request.getListType());
        blacklist.setListValue(request.getListValue());
        blacklist.setListSource(request.getListSource() != null ? request.getListSource() : "MANUAL");
        blacklist.setRiskLevel(request.getRiskLevel() != null ? request.getRiskLevel() : 3);
        blacklist.setReason(request.getReason());
        blacklist.setStatus(1);
        blacklist.setExpireTime(request.getExpireTime());
        blacklist.setHitCount(0);
        this.save(blacklist);

        String cacheKey = BLACKLIST_KEY_PREFIX + request.getListType() + ":" + request.getListValue();
        redisTemplate.opsForValue().set(cacheKey, blacklist, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        log.info("添加黑名单成功，类型：{}，值：{}", request.getListType(), request.getListValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFromBlacklist(Long id) {
        RiskBlacklist blacklist = this.getById(id);
        if (blacklist == null || blacklist.getDeleted() == 1) {
            throw new BusinessException("黑名单不存在");
        }

        this.removeById(id);

        String cacheKey = BLACKLIST_KEY_PREFIX + blacklist.getListType() + ":" + blacklist.getListValue();
        redisTemplate.delete(cacheKey);

        log.info("移除黑名单成功，ID：{}，类型：{}，值：{}", id, blacklist.getListType(), blacklist.getListValue());
    }

    @Override
    public boolean checkInList(String listType, String listValue) {
        return getByTypeAndValue(listType, listValue) != null;
    }

    @Override
    public RiskBlacklist getByTypeAndValue(String listType, String listValue) {
        if (StrUtil.isBlank(listType) || StrUtil.isBlank(listValue)) {
            return null;
        }

        String cacheKey = BLACKLIST_KEY_PREFIX + listType + ":" + listValue;
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null && cachedObj instanceof RiskBlacklist) {
            RiskBlacklist cached = (RiskBlacklist) cachedObj;
            if (cached.getStatus() == null || cached.getStatus() != 1) {
                return null;
            }
            if (cached.getExpireTime() != null && cached.getExpireTime().isBefore(LocalDateTime.now())) {
                redisTemplate.delete(cacheKey);
                return null;
            }
            incrementHitCount(cached);
            return cached;
        }

        LambdaQueryWrapper<RiskBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskBlacklist::getListType, listType)
                .eq(RiskBlacklist::getListValue, listValue)
                .eq(RiskBlacklist::getStatus, 1)
                .eq(RiskBlacklist::getDeleted, 0)
                .and(w -> w.isNull(RiskBlacklist::getExpireTime)
                        .or()
                        .gt(RiskBlacklist::getExpireTime, LocalDateTime.now()));
        RiskBlacklist record = this.getOne(wrapper);

        if (record != null) {
            redisTemplate.opsForValue().set(cacheKey, record, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            incrementHitCount(record);
        }

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementHitCount(RiskBlacklist record) {
        if (record == null || record.getId() == null) {
            return;
        }
        try {
            record.setHitCount(record.getHitCount() == null ? 1 : record.getHitCount() + 1);
            record.setLastHitTime(LocalDateTime.now());
            this.updateById(record);
        } catch (Exception e) {
            log.warn("更新黑名单命中次数失败，id: {}", record.getId(), e);
        }
    }

    private RiskListVO convertToVO(RiskBlacklist entity) {
        RiskListVO vo = new RiskListVO();
        vo.setId(entity.getId());
        vo.setListType(entity.getListType());
        vo.setListValue(entity.getListValue());
        vo.setListSource(entity.getListSource());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setReason(entity.getReason());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(entity.getStatus() != null && entity.getStatus() == 1 ? "启用" : "禁用");
        vo.setExpireTime(entity.getExpireTime());
        vo.setHitCount(entity.getHitCount());
        vo.setLastHitTime(entity.getLastHitTime());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
