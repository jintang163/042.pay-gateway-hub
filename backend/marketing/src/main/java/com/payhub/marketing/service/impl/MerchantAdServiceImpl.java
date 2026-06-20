package com.payhub.marketing.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.utils.JsonUtils;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.common.utils.SnowflakeIdUtil;
import com.payhub.marketing.dto.*;
import com.payhub.marketing.entity.AdClickLog;
import com.payhub.marketing.entity.AdStatsDaily;
import com.payhub.marketing.entity.MerchantAd;
import com.payhub.marketing.enums.AdPositionEnum;
import com.payhub.marketing.enums.AdStatusEnum;
import com.payhub.marketing.mapper.AdClickLogMapper;
import com.payhub.marketing.mapper.AdStatsDailyMapper;
import com.payhub.marketing.mapper.MerchantAdMapper;
import com.payhub.marketing.service.MerchantAdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MerchantAdServiceImpl implements MerchantAdService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Autowired
    private MerchantAdMapper merchantAdMapper;

    @Autowired
    private AdClickLogMapper adClickLogMapper;

    @Autowired
    private AdStatsDailyMapper adStatsDailyMapper;

    @Override
    public IPage<MerchantAdVO> listPage(Long current, Long size, String merchantNo, Map<String, Object> params) {
        LambdaQueryWrapper<MerchantAd> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(merchantNo), MerchantAd::getMerchantNo, merchantNo);
        if (params != null) {
            wrapper.eq(params.get("adCode") != null, MerchantAd::getAdCode, params.get("adCode"));
            wrapper.eq(params.get("status") != null, MerchantAd::getStatus, params.get("status"));
            wrapper.eq(params.get("position") != null, MerchantAd::getPosition, params.get("position"));
            wrapper.like(params.get("adTitle") != null, MerchantAd::getAdTitle, (String) params.get("adTitle"));
        }
        wrapper.orderByDesc(MerchantAd::getSortOrder, MerchantAd::getCreatedAt);
        IPage<MerchantAd> page = merchantAdMapper.selectPage(new Page<>(current, size), wrapper);
        return page.convert(this::toVO);
    }

    @Override
    public MerchantAdVO getByAdCode(String adCode) {
        LambdaQueryWrapper<MerchantAd> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantAd::getAdCode, adCode);
        MerchantAd ad = merchantAdMapper.selectOne(wrapper);
        return ad == null ? null : toVO(ad);
    }

    @Override
    public MerchantAdVO getById(Long id) {
        MerchantAd ad = merchantAdMapper.selectById(id);
        return ad == null ? null : toVO(ad);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAd(MerchantAdSaveRequest request) {
        MerchantAd ad;
        if (request.getId() != null) {
            ad = merchantAdMapper.selectById(request.getId());
            if (ad == null) {
                throw new BusinessException("广告不存在");
            }
        } else {
            ad = new MerchantAd();
            ad.setAdCode(OrderNoGenerator.generateWithPrefix("AD"));
            ad.setClickCount(0);
            ad.setImpressionCount(0);
            ad.setTotalCost(BigDecimal.ZERO);
        }
        ad.setMerchantNo(request.getMerchantNo());
        ad.setAdTitle(request.getAdTitle());
        ad.setAdDescription(request.getAdDescription());
        ad.setAdImageUrl(request.getAdImageUrl());
        ad.setTargetUrl(request.getTargetUrl());
        ad.setPosition(request.getPosition());
        ad.setCpcPrice(request.getCpcPrice() == null ? BigDecimal.ZERO : request.getCpcPrice());
        ad.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        ad.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getStartTime())) {
            ad.setStartTime(LocalDateTime.parse(request.getStartTime(), DTF));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            ad.setEndTime(LocalDateTime.parse(request.getEndTime(), DTF));
        }
        ad.setDailyBudget(request.getDailyBudget() == null ? BigDecimal.ZERO : request.getDailyBudget());
        ad.setRemark(request.getRemark());
        if (request.getId() != null) {
            merchantAdMapper.updateById(ad);
            log.info("广告更新成功, adCode={}, merchantNo={}", ad.getAdCode(), request.getMerchantNo());
        } else {
            merchantAdMapper.insert(ad);
            log.info("广告创建成功, adCode={}, merchantNo={}", ad.getAdCode(), request.getMerchantNo());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        MerchantAd ad = merchantAdMapper.selectById(id);
        if (ad == null) {
            throw new BusinessException("广告不存在");
        }
        if (AdStatusEnum.ONLINE.getCode().equals(ad.getStatus())) {
            ad.setStatus(AdStatusEnum.OFFLINE.getCode());
        } else if (AdStatusEnum.OFFLINE.getCode().equals(ad.getStatus())) {
            if (ad.getEndTime() != null && ad.getEndTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException("广告已过期，请延长结束时间后再上架");
            }
            ad.setStatus(AdStatusEnum.ONLINE.getCode());
        }
        merchantAdMapper.updateById(ad);
        log.info("广告状态切换成功, adCode={}, newStatus={}", ad.getAdCode(), ad.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAd(Long id) {
        MerchantAd ad = merchantAdMapper.selectById(id);
        if (ad == null) {
            throw new BusinessException("广告不存在");
        }
        merchantAdMapper.deleteById(id);
        log.info("广告删除成功, adCode={}", ad.getAdCode());
    }

    @Override
    public List<AdDisplayVO> listDisplayAds(String merchantNo, String position, Integer limit) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<MerchantAd> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantAd::getMerchantNo, merchantNo);
        wrapper.eq(MerchantAd::getPosition, position);
        wrapper.eq(MerchantAd::getStatus, AdStatusEnum.ONLINE.getCode());
        wrapper.and(w -> w.isNull(MerchantAd::getStartTime).or().le(MerchantAd::getStartTime, now));
        wrapper.and(w -> w.isNull(MerchantAd::getEndTime).or().ge(MerchantAd::getEndTime, now));
        wrapper.orderByDesc(MerchantAd::getSortOrder, MerchantAd::getCreatedAt);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        List<MerchantAd> ads = merchantAdMapper.selectList(wrapper);
        if (ads.isEmpty()) {
            return Collections.emptyList();
        }
        return ads.stream().filter(this::checkDailyBudget).map(ad -> {
            AdDisplayVO vo = new AdDisplayVO();
            vo.setAdCode(ad.getAdCode());
            vo.setAdTitle(ad.getAdTitle());
            vo.setAdDescription(ad.getAdDescription());
            vo.setAdImageUrl(ad.getAdImageUrl());
            vo.setTargetUrl(ad.getTargetUrl());
            vo.setPosition(ad.getPosition());
            vo.setCpcPrice(ad.getCpcPrice());
            return vo;
        }).collect(Collectors.toList());
    }

    private boolean checkDailyBudget(MerchantAd ad) {
        if (ad.getDailyBudget() == null || ad.getDailyBudget().compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<AdStatsDaily> w = new LambdaQueryWrapper<>();
        w.eq(AdStatsDaily::getStatsDate, today).eq(AdStatsDaily::getAdCode, ad.getAdCode());
        AdStatsDaily todayStats = adStatsDailyMapper.selectOne(w);
        if (todayStats == null) {
            return true;
        }
        return todayStats.getTotalCost() == null
                || todayStats.getTotalCost().compareTo(ad.getDailyBudget()) < 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordImpression(List<String> adCodes) {
        if (adCodes == null || adCodes.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (String adCode : new HashSet<>(adCodes)) {
            if (StrUtil.isBlank(adCode)) continue;
            LambdaQueryWrapper<MerchantAd> adWrapper = new LambdaQueryWrapper<>();
            adWrapper.eq(MerchantAd::getAdCode, adCode);
            MerchantAd ad = merchantAdMapper.selectOne(adWrapper);
            if (ad != null) {
                merchantAdMapper.update(null, new LambdaUpdateWrapper<MerchantAd>()
                        .eq(MerchantAd::getAdCode, adCode)
                        .setSql("impression_count = impression_count + 1"));
                upsertDailyStatsImpression(today, ad);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordImpression(AdImpressionReportRequest request) {
        if (request == null || request.getCodes() == null || request.getCodes().isEmpty()) {
            return;
        }
        recordImpression(request.getCodes());
        log.info("广告曝光上报, merchantNo={}, orderNo={}, position={}, payAmount={}, codes={}, deviceId={}, ip={}",
                request.getMerchantNo(), request.getOrderNo(), request.getPosition(),
                request.getPayAmount(), request.getCodes(), request.getDeviceId(), request.getClientIp());
    }

    private void upsertDailyStatsImpression(LocalDate today, MerchantAd ad) {
        try {
            AdStatsDaily stats = new AdStatsDaily();
            stats.setStatsDate(today);
            stats.setAdCode(ad.getAdCode());
            stats.setMerchantNo(ad.getMerchantNo());
            stats.setPosition(ad.getPosition());
            stats.setImpressionCount(1);
            stats.setClickCount(0);
            stats.setValidClickCount(0);
            stats.setInvalidClickCount(0);
            stats.setTotalCost(BigDecimal.ZERO);
            stats.setCtr(BigDecimal.ZERO);
            stats.setAvgCpc(BigDecimal.ZERO);
            stats.setOrderCount(0);
            stats.setOrderAmount(BigDecimal.ZERO);
            adStatsDailyMapper.insert(stats);
        } catch (DuplicateKeyException e) {
            adStatsDailyMapper.update(null, new LambdaUpdateWrapper<AdStatsDaily>()
                    .eq(AdStatsDaily::getStatsDate, today)
                    .eq(AdStatsDaily::getAdCode, ad.getAdCode())
                    .setSql("impression_count = impression_count + 1"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdClickReportResult reportClick(AdClickReportRequest request, HttpServletRequest httpRequest) {
        LambdaQueryWrapper<MerchantAd> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantAd::getAdCode, request.getAdCode());
        MerchantAd ad = merchantAdMapper.selectOne(wrapper);
        if (ad == null) {
            throw new BusinessException("广告不存在");
        }
        String clickNo = SnowflakeIdUtil.generateStrId("CLK");
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest == null ? "" : httpRequest.getHeader("User-Agent");
        LocalDateTime clickTime = LocalDateTime.now();
        LocalDate clickDate = clickTime.toLocalDate();

        boolean valid = true;
        String invalidReason = null;
        BigDecimal cpcPrice = ad.getCpcPrice() == null ? BigDecimal.ZERO : ad.getCpcPrice();
        BigDecimal costAmount = BigDecimal.ZERO;

        if (AdStatusEnum.OFFLINE.getCode().equals(ad.getStatus())) {
            valid = false;
            invalidReason = "广告已下架";
        }
        if (valid && ad.getEndTime() != null && clickTime.isAfter(ad.getEndTime())) {
            valid = false;
            invalidReason = "广告已过期";
        }
        if (valid && isDuplicateClick(ad.getAdCode(), clientIp, clickDate)) {
            valid = false;
            invalidReason = "短时间重复点击(防作弊)";
        }
        if (valid) {
            if (!checkDailyBudget(ad)) {
                valid = false;
                invalidReason = "已超出当日预算";
            }
        }
        if (valid) {
            costAmount = cpcPrice;
        }

        AdClickLog log = new AdClickLog();
        log.setClickNo(clickNo);
        log.setAdCode(ad.getAdCode());
        log.setMerchantNo(ad.getMerchantNo());
        log.setOrderNo(request.getOrderNo());
        log.setPayAmount(request.getPayAmount());
        log.setPosition(request.getPosition() == null ? ad.getPosition() : request.getPosition());
        log.setCpcPrice(cpcPrice);
        log.setCostAmount(costAmount);
        log.setUserAgent(userAgent);
        log.setClientIp(clientIp);
        log.setDeviceId(request.getDeviceId());
        log.setRefererUrl(request.getRefererUrl());
        log.setTargetUrl(ad.getTargetUrl());
        log.setClickTime(clickTime);
        log.setClickDate(clickDate);
        log.setStatus(valid ? 1 : 0);
        log.setInvalidReason(invalidReason);
        adClickLogMapper.insert(log);

        merchantAdMapper.update(null, new LambdaUpdateWrapper<MerchantAd>()
                .eq(MerchantAd::getAdCode, ad.getAdCode())
                .setSql("click_count = click_count + 1")
                .setSql(valid ? "total_cost = total_cost + " + costAmount.toPlainString() : null));

        upsertDailyStatsClick(clickDate, ad, valid, costAmount, request.getPayAmount());

        return AdClickReportResult.builder()
                .clickNo(clickNo)
                .adCode(ad.getAdCode())
                .targetUrl(ad.getTargetUrl())
                .cpcPrice(cpcPrice)
                .costAmount(costAmount)
                .valid(valid)
                .invalidReason(invalidReason)
                .build();
    }

    private boolean isDuplicateClick(String adCode, String clientIp, LocalDate date) {
        if (StrUtil.isBlank(clientIp)) {
            return false;
        }
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        LambdaQueryWrapper<AdClickLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdClickLog::getAdCode, adCode)
                .eq(AdClickLog::getClientIp, clientIp)
                .ge(AdClickLog::getClickTime, fiveMinutesAgo);
        Long count = adClickLogMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private void upsertDailyStatsClick(LocalDate date, MerchantAd ad, boolean valid, BigDecimal cost, BigDecimal payAmount) {
        LambdaQueryWrapper<AdStatsDaily> w = new LambdaQueryWrapper<>();
        w.eq(AdStatsDaily::getStatsDate, date).eq(AdStatsDaily::getAdCode, ad.getAdCode());
        AdStatsDaily exist = adStatsDailyMapper.selectOne(w);
        if (exist == null) {
            AdStatsDaily stats = new AdStatsDaily();
            stats.setStatsDate(date);
            stats.setAdCode(ad.getAdCode());
            stats.setMerchantNo(ad.getMerchantNo());
            stats.setPosition(ad.getPosition());
            stats.setImpressionCount(0);
            stats.setClickCount(1);
            stats.setValidClickCount(valid ? 1 : 0);
            stats.setInvalidClickCount(valid ? 0 : 1);
            stats.setTotalCost(cost);
            stats.setCtr(BigDecimal.ZERO);
            stats.setAvgCpc(valid ? cost : BigDecimal.ZERO);
            stats.setOrderCount(payAmount != null ? 1 : 0);
            stats.setOrderAmount(payAmount == null ? BigDecimal.ZERO : payAmount);
            adStatsDailyMapper.insert(stats);
        } else {
            LambdaUpdateWrapper<AdStatsDaily> uw = new LambdaUpdateWrapper<>();
            uw.eq(AdStatsDaily::getStatsDate, date).eq(AdStatsDaily::getAdCode, ad.getAdCode())
                    .setSql("click_count = click_count + 1");
            if (valid) {
                uw.setSql("valid_click_count = valid_click_count + 1");
                uw.setSql("total_cost = total_cost + " + cost.toPlainString());
            } else {
                uw.setSql("invalid_click_count = invalid_click_count + 1");
            }
            if (payAmount != null) {
                uw.setSql("order_count = order_count + 1");
                uw.setSql("order_amount = order_amount + " + payAmount.toPlainString());
            }
            adStatsDailyMapper.update(null, uw);

            AdStatsDaily updated = adStatsDailyMapper.selectById(exist.getId());
            BigDecimal newCtr = BigDecimal.ZERO;
            BigDecimal newAvgCpc = BigDecimal.ZERO;
            if (updated.getImpressionCount() != null && updated.getImpressionCount() > 0) {
                newCtr = new BigDecimal(updated.getValidClickCount() == null ? 0 : updated.getValidClickCount())
                        .multiply(ONE_HUNDRED)
                        .divide(new BigDecimal(updated.getImpressionCount()), 4, RoundingMode.HALF_UP);
            }
            int validClicks = updated.getValidClickCount() == null ? 0 : updated.getValidClickCount();
            if (validClicks > 0 && updated.getTotalCost() != null) {
                newAvgCpc = updated.getTotalCost().divide(new BigDecimal(validClicks), 4, RoundingMode.HALF_UP);
            }
            LambdaUpdateWrapper<AdStatsDaily> ctrUw = new LambdaUpdateWrapper<>();
            ctrUw.eq(AdStatsDaily::getId, updated.getId())
                    .set(AdStatsDaily::getCtr, newCtr)
                    .set(AdStatsDaily::getAvgCpc, newAvgCpc);
            adStatsDailyMapper.update(null, ctrUw);
        }
    }

    @Override
    public AdStatsOverviewVO getStatsOverview(String merchantNo, String startDate, String endDate, String adCode, String position) {
        LocalDate s = StrUtil.isNotBlank(startDate) ? LocalDate.parse(startDate) : LocalDate.now().minusDays(29);
        LocalDate e = StrUtil.isNotBlank(endDate) ? LocalDate.parse(endDate) : LocalDate.now();

        LambdaQueryWrapper<AdStatsDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(merchantNo), AdStatsDaily::getMerchantNo, merchantNo);
        wrapper.eq(StrUtil.isNotBlank(adCode), AdStatsDaily::getAdCode, adCode);
        wrapper.eq(StrUtil.isNotBlank(position), AdStatsDaily::getPosition, position);
        wrapper.ge(AdStatsDaily::getStatsDate, s);
        wrapper.le(AdStatsDaily::getStatsDate, e);
        wrapper.orderByAsc(AdStatsDaily::getStatsDate);
        List<AdStatsDaily> list = adStatsDailyMapper.selectList(wrapper);

        AdStatsOverviewVO vo = new AdStatsOverviewVO();
        vo.setMerchantNo(merchantNo);
        vo.setStartDate(s);
        vo.setEndDate(e);

        int totalImpression = 0, totalClick = 0, totalValid = 0, totalInvalid = 0, totalOrder = 0;
        BigDecimal totalCost = BigDecimal.ZERO, totalOrderAmount = BigDecimal.ZERO;
        Map<String, AdItemStatsVO> adMap = new LinkedHashMap<>();
        Map<LocalDate, AdDailyStatsVO> dailyMap = new LinkedHashMap<>();

        for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
            AdDailyStatsVO dv = new AdDailyStatsVO();
            dv.setStatsDate(d);
            dv.setImpressionCount(0);
            dv.setClickCount(0);
            dv.setValidClickCount(0);
            dv.setInvalidClickCount(0);
            dv.setTotalCost(BigDecimal.ZERO);
            dv.setCtr(BigDecimal.ZERO);
            dv.setAvgCpc(BigDecimal.ZERO);
            dv.setOrderCount(0);
            dv.setOrderAmount(BigDecimal.ZERO);
            dailyMap.put(d, dv);
        }

        for (AdStatsDaily row : list) {
            totalImpression += row.getImpressionCount() == null ? 0 : row.getImpressionCount();
            totalClick += row.getClickCount() == null ? 0 : row.getClickCount();
            totalValid += row.getValidClickCount() == null ? 0 : row.getValidClickCount();
            totalInvalid += row.getInvalidClickCount() == null ? 0 : row.getInvalidClickCount();
            totalOrder += row.getOrderCount() == null ? 0 : row.getOrderCount();
            totalCost = totalCost.add(row.getTotalCost() == null ? BigDecimal.ZERO : row.getTotalCost());
            totalOrderAmount = totalOrderAmount.add(row.getOrderAmount() == null ? BigDecimal.ZERO : row.getOrderAmount());

            AdDailyStatsVO dv = dailyMap.get(row.getStatsDate());
            if (dv != null) {
                dv.setImpressionCount(dv.getImpressionCount() + (row.getImpressionCount() == null ? 0 : row.getImpressionCount()));
                dv.setClickCount(dv.getClickCount() + (row.getClickCount() == null ? 0 : row.getClickCount()));
                dv.setValidClickCount(dv.getValidClickCount() + (row.getValidClickCount() == null ? 0 : row.getValidClickCount()));
                dv.setInvalidClickCount(dv.getInvalidClickCount() + (row.getInvalidClickCount() == null ? 0 : row.getInvalidClickCount()));
                dv.setTotalCost(dv.getTotalCost().add(row.getTotalCost() == null ? BigDecimal.ZERO : row.getTotalCost()));
                dv.setOrderCount(dv.getOrderCount() + (row.getOrderCount() == null ? 0 : row.getOrderCount()));
                dv.setOrderAmount(dv.getOrderAmount().add(row.getOrderAmount() == null ? BigDecimal.ZERO : row.getOrderAmount()));
                if (dv.getImpressionCount() > 0) {
                    dv.setCtr(new BigDecimal(dv.getValidClickCount()).multiply(ONE_HUNDRED)
                            .divide(new BigDecimal(dv.getImpressionCount()), 4, RoundingMode.HALF_UP));
                }
                if (dv.getValidClickCount() > 0) {
                    dv.setAvgCpc(dv.getTotalCost().divide(new BigDecimal(dv.getValidClickCount()), 4, RoundingMode.HALF_UP));
                }
            }

            AdItemStatsVO iv = adMap.computeIfAbsent(row.getAdCode(), k -> {
                AdItemStatsVO v = new AdItemStatsVO();
                v.setAdCode(row.getAdCode());
                v.setPosition(row.getPosition());
                AdPositionEnum positionEnum = AdPositionEnum.getByCode(row.getPosition());
                v.setPositionDesc(positionEnum == null ? row.getPosition() : positionEnum.getDesc());
                v.setImpressionCount(0);
                v.setClickCount(0);
                v.setValidClickCount(0);
                v.setInvalidClickCount(0);
                v.setTotalCost(BigDecimal.ZERO);
                v.setOrderCount(0);
                v.setOrderAmount(BigDecimal.ZERO);
                LambdaQueryWrapper<MerchantAd> adWrapper = new LambdaQueryWrapper<>();
                adWrapper.eq(MerchantAd::getAdCode, row.getAdCode());
                MerchantAd ad = merchantAdMapper.selectOne(adWrapper);
                if (ad != null) {
                    v.setAdTitle(ad.getAdTitle());
                    v.setStatus(ad.getStatus());
                    AdStatusEnum statusEnum = AdStatusEnum.getByCode(ad.getStatus());
                    v.setStatusDesc(statusEnum == null ? String.valueOf(ad.getStatus()) : statusEnum.getDesc());
                    v.setCpcPrice(ad.getCpcPrice());
                }
                return v;
            });
            iv.setImpressionCount(iv.getImpressionCount() + (row.getImpressionCount() == null ? 0 : row.getImpressionCount()));
            iv.setClickCount(iv.getClickCount() + (row.getClickCount() == null ? 0 : row.getClickCount()));
            iv.setValidClickCount(iv.getValidClickCount() + (row.getValidClickCount() == null ? 0 : row.getValidClickCount()));
            iv.setInvalidClickCount(iv.getInvalidClickCount() + (row.getInvalidClickCount() == null ? 0 : row.getInvalidClickCount()));
            iv.setTotalCost(iv.getTotalCost().add(row.getTotalCost() == null ? BigDecimal.ZERO : row.getTotalCost()));
            iv.setOrderCount(iv.getOrderCount() + (row.getOrderCount() == null ? 0 : row.getOrderCount()));
            iv.setOrderAmount(iv.getOrderAmount().add(row.getOrderAmount() == null ? BigDecimal.ZERO : row.getOrderAmount()));
        }

        for (AdItemStatsVO iv : adMap.values()) {
            if (iv.getImpressionCount() > 0) {
                iv.setCtr(new BigDecimal(iv.getValidClickCount()).multiply(ONE_HUNDRED)
                        .divide(new BigDecimal(iv.getImpressionCount()), 4, RoundingMode.HALF_UP));
            } else {
                iv.setCtr(BigDecimal.ZERO);
            }
            if (iv.getValidClickCount() > 0) {
                iv.setAvgCpc(iv.getTotalCost().divide(new BigDecimal(iv.getValidClickCount()), 4, RoundingMode.HALF_UP));
            } else {
                iv.setAvgCpc(BigDecimal.ZERO);
            }
        }

        vo.setTotalImpression(totalImpression);
        vo.setTotalClick(totalClick);
        vo.setTotalValidClick(totalValid);
        vo.setTotalInvalidClick(totalInvalid);
        vo.setTotalCost(totalCost);
        vo.setTotalOrder(totalOrder);
        vo.setTotalOrderAmount(totalOrderAmount);
        if (totalImpression > 0) {
            vo.setOverallCtr(new BigDecimal(totalValid).multiply(ONE_HUNDRED)
                    .divide(new BigDecimal(totalImpression), 4, RoundingMode.HALF_UP));
        } else {
            vo.setOverallCtr(BigDecimal.ZERO);
        }
        if (totalValid > 0) {
            vo.setOverallAvgCpc(totalCost.divide(new BigDecimal(totalValid), 4, RoundingMode.HALF_UP));
        } else {
            vo.setOverallAvgCpc(BigDecimal.ZERO);
        }

        vo.setDailyStats(new ArrayList<>(dailyMap.values()));
        vo.setAdStats(new ArrayList<>(adMap.values()));

        log.info("广告统计查询, merchantNo={}, startDate={}, endDate={}, totalImpression={}, totalClick={}, totalCost={}",
                merchantNo, s, e, totalImpression, totalClick, totalCost);
        return vo;
    }

    private MerchantAdVO toVO(MerchantAd ad) {
        MerchantAdVO vo = new MerchantAdVO();
        vo.setId(ad.getId());
        vo.setAdCode(ad.getAdCode());
        vo.setMerchantNo(ad.getMerchantNo());
        vo.setAdTitle(ad.getAdTitle());
        vo.setAdDescription(ad.getAdDescription());
        vo.setAdImageUrl(ad.getAdImageUrl());
        vo.setTargetUrl(ad.getTargetUrl());
        vo.setPosition(ad.getPosition());
        AdPositionEnum positionEnum = AdPositionEnum.getByCode(ad.getPosition());
        vo.setPositionDesc(positionEnum == null ? ad.getPosition() : positionEnum.getDesc());
        vo.setCpcPrice(ad.getCpcPrice());
        vo.setSortOrder(ad.getSortOrder());
        vo.setStatus(ad.getStatus());
        AdStatusEnum statusEnum = AdStatusEnum.getByCode(ad.getStatus());
        vo.setStatusDesc(statusEnum == null ? String.valueOf(ad.getStatus()) : statusEnum.getDesc());
        vo.setStartTime(ad.getStartTime());
        vo.setEndTime(ad.getEndTime());
        vo.setDailyBudget(ad.getDailyBudget());
        vo.setClickCount(ad.getClickCount());
        vo.setImpressionCount(ad.getImpressionCount());
        vo.setTotalCost(ad.getTotalCost());
        if (ad.getImpressionCount() != null && ad.getImpressionCount() > 0) {
            vo.setCtr(new BigDecimal(ad.getClickCount() == null ? 0 : ad.getClickCount())
                    .multiply(ONE_HUNDRED)
                    .divide(new BigDecimal(ad.getImpressionCount()), 4, RoundingMode.HALF_UP));
        }
        vo.setOperatorId(ad.getOperatorId());
        vo.setOperatorName(ad.getOperatorName());
        vo.setRemark(ad.getRemark());
        vo.setCreatedAt(ad.getCreatedAt());
        vo.setUpdatedAt(ad.getUpdatedAt());
        return vo;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "X-Real-IP"};
        for (String h : headers) {
            String ip = request.getHeader(h);
            if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
            }
        }
        return request.getRemoteAddr();
    }
}
