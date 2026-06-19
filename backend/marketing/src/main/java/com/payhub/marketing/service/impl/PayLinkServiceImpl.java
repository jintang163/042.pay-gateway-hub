package com.payhub.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.marketing.dto.PayLinkSaveRequest;
import com.payhub.marketing.dto.PayLinkVO;
import com.payhub.marketing.entity.PayLink;
import com.payhub.marketing.enums.PayLinkStatusEnum;
import com.payhub.marketing.mapper.PayLinkMapper;
import com.payhub.marketing.service.PayLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class PayLinkServiceImpl implements PayLinkService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private PayLinkMapper payLinkMapper;

    @Override
    public IPage<PayLinkVO> listPage(Long current, Long size, String merchantNo, Map<String, Object> params) {
        LambdaQueryWrapper<PayLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(merchantNo), PayLink::getMerchantNo, merchantNo);
        if (params != null) {
            wrapper.eq(params.get("linkCode") != null, PayLink::getLinkCode, params.get("linkCode"));
            wrapper.eq(params.get("status") != null, PayLink::getStatus, params.get("status"));
            wrapper.like(params.get("title") != null, PayLink::getTitle, params.get("title"));
            if (params.get("startTime") != null) {
                wrapper.ge(PayLink::getCreatedAt, params.get("startTime"));
            }
            if (params.get("endTime") != null) {
                wrapper.le(PayLink::getCreatedAt, params.get("endTime"));
            }
        }
        wrapper.orderByDesc(PayLink::getCreatedAt);
        IPage<PayLink> page = payLinkMapper.selectPage(new Page<>(current, size), wrapper);
        return page.convert(this::toVO);
    }

    @Override
    public PayLinkVO getByLinkCode(String linkCode) {
        PayLink link = getByLinkCodeEntity(linkCode);
        return toVO(link);
    }

    @Override
    public void saveLink(PayLinkSaveRequest request) {
        PayLink link;
        if (request.getId() != null) {
            link = payLinkMapper.selectById(request.getId());
            if (link == null) {
                throw new BusinessException("支付链接不存在");
            }
        } else {
            link = new PayLink();
            link.setLinkCode(OrderNoGenerator.generateWithPrefix("LK"));
            link.setUsedCount(0);
            link.setStatus(PayLinkStatusEnum.ACTIVE.getCode());
        }
        link.setMerchantNo(request.getMerchantNo());
        link.setTitle(request.getTitle());
        link.setFixedAmount(request.getFixedAmount());
        link.setAmountEditable(request.getAmountEditable());
        link.setMinAmount(request.getMinAmount());
        link.setMaxAmount(request.getMaxAmount());
        link.setPayChannel(request.getPayChannel());
        link.setProductSubject(request.getProductSubject());
        link.setProductDetail(request.getProductDetail());
        link.setNotifyUrl(request.getNotifyUrl());
        link.setRedirectUrl(request.getRedirectUrl());
        if (StringUtils.hasText(request.getExpireTime())) {
            link.setExpireTime(LocalDateTime.parse(request.getExpireTime(), DTF));
        }
        link.setSingleUse(request.getSingleUse());
        link.setMaxUseCount(request.getMaxUseCount());
        link.setRemark(request.getRemark());
        if (request.getId() != null) {
            payLinkMapper.updateById(link);
        } else {
            payLinkMapper.insert(link);
        }
    }

    @Override
    public void toggleStatus(Long id) {
        PayLink link = payLinkMapper.selectById(id);
        if (link == null) {
            throw new BusinessException("支付链接不存在");
        }
        if (link.getStatus().equals(PayLinkStatusEnum.ACTIVE.getCode())) {
            link.setStatus(PayLinkStatusEnum.DISABLED.getCode());
        } else if (link.getStatus().equals(PayLinkStatusEnum.DISABLED.getCode())) {
            link.setStatus(PayLinkStatusEnum.ACTIVE.getCode());
        } else {
            throw new BusinessException("当前状态不允许切换");
        }
        payLinkMapper.updateById(link);
    }

    @Override
    public void deleteLink(Long id) {
        payLinkMapper.deleteById(id);
    }

    @Override
    public PayLinkVO resolveLink(String linkCode) {
        PayLink link = getByLinkCodeEntity(linkCode);
        if (!PayLinkStatusEnum.ACTIVE.getCode().equals(link.getStatus())) {
            PayLinkStatusEnum statusEnum = PayLinkStatusEnum.getByCode(link.getStatus());
            throw new BusinessException("链接已" + (statusEnum != null ? statusEnum.getDesc() : "失效"));
        }
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())) {
            link.setStatus(PayLinkStatusEnum.EXPIRED.getCode());
            payLinkMapper.updateById(link);
            throw new BusinessException("链接已过期");
        }
        if (link.getSingleUse() && link.getUsedCount() >= 1) {
            link.setStatus(PayLinkStatusEnum.EXHAUSTED.getCode());
            payLinkMapper.updateById(link);
            throw new BusinessException("链接已被使用");
        }
        if (link.getMaxUseCount() != null && link.getUsedCount() >= link.getMaxUseCount()) {
            link.setStatus(PayLinkStatusEnum.EXHAUSTED.getCode());
            payLinkMapper.updateById(link);
            throw new BusinessException("链接已达最大使用次数");
        }
        return toVO(link);
    }

    private PayLink getByLinkCodeEntity(String linkCode) {
        LambdaQueryWrapper<PayLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayLink::getLinkCode, linkCode);
        PayLink link = payLinkMapper.selectOne(wrapper);
        if (link == null) {
            throw new BusinessException("支付链接不存在");
        }
        return link;
    }

    private PayLinkVO toVO(PayLink link) {
        PayLinkVO vo = new PayLinkVO();
        vo.setId(link.getId());
        vo.setLinkCode(link.getLinkCode());
        vo.setMerchantNo(link.getMerchantNo());
        vo.setTitle(link.getTitle());
        vo.setFixedAmount(link.getFixedAmount());
        vo.setAmountEditable(link.getAmountEditable());
        vo.setMinAmount(link.getMinAmount());
        vo.setMaxAmount(link.getMaxAmount());
        vo.setPayChannel(link.getPayChannel());
        vo.setProductSubject(link.getProductSubject());
        vo.setProductDetail(link.getProductDetail());
        vo.setNotifyUrl(link.getNotifyUrl());
        vo.setRedirectUrl(link.getRedirectUrl());
        vo.setExpireTime(link.getExpireTime());
        vo.setSingleUse(link.getSingleUse());
        vo.setMaxUseCount(link.getMaxUseCount());
        vo.setUsedCount(link.getUsedCount());
        vo.setStatus(link.getStatus());
        PayLinkStatusEnum statusEnum = PayLinkStatusEnum.getByCode(link.getStatus());
        vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "未知");
        vo.setRemark(link.getRemark());
        vo.setCreatedAt(link.getCreatedAt());
        vo.setUpdatedAt(link.getUpdatedAt());
        return vo;
    }
}
