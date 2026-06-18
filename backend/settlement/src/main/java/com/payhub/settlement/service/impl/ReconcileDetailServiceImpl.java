package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.enums.PayChannelEnum;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.settlement.dto.ReconcileDetailVO;
import com.payhub.settlement.dto.ReconcileSummaryVO;
import com.payhub.settlement.entity.ReconcileDetail;
import com.payhub.settlement.enums.ReconcileDiffTypeEnum;
import com.payhub.settlement.enums.ReconcileHandleStatusEnum;
import com.payhub.settlement.mapper.ReconcileDetailMapper;
import com.payhub.settlement.service.ReconcileDetailService;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class ReconcileDetailServiceImpl extends ServiceImpl<ReconcileDetailMapper, ReconcileDetail> implements ReconcileDetailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public IPage<ReconcileDetailVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<ReconcileDetail> page = new Page<>(current, size);
        LambdaQueryWrapper<ReconcileDetail> wrapper = buildQueryWrapper(params);
        wrapper.orderByDesc(ReconcileDetail::getReconcileDate, ReconcileDetail::getId);
        IPage<ReconcileDetail> recordPage = this.page(page, wrapper);
        return recordPage.convert(this::convertToVO);
    }

    @Override
    public List<ReconcileDetailVO> listByReconcileNo(String reconcileNo) {
        if (StrUtil.isBlank(reconcileNo)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ReconcileDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileDetail::getReconcileNo, reconcileNo);
        wrapper.orderByDesc(ReconcileDetail::getId);
        List<ReconcileDetail> list = this.list(wrapper);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<ReconcileDetailVO> result = new ArrayList<>();
        for (ReconcileDetail detail : list) {
            result.add(convertToVO(detail));
        }
        return result;
    }

    @Override
    public ReconcileSummaryVO getSummary(String reconcileNo) {
        if (StrUtil.isBlank(reconcileNo)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "对账单号不能为空");
        }
        LambdaQueryWrapper<ReconcileDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileDetail::getReconcileNo, reconcileNo);
        List<ReconcileDetail> details = this.list(wrapper);
        return buildSummary(details, reconcileNo);
    }

    @Override
    public ReconcileSummaryVO getSummaryByDateAndChannel(LocalDate reconcileDate, String payChannel) {
        if (reconcileDate == null || StrUtil.isBlank(payChannel)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "对账日期和支付渠道不能为空");
        }
        LambdaQueryWrapper<ReconcileDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileDetail::getReconcileDate, reconcileDate)
                .eq(ReconcileDetail::getPayChannel, payChannel);
        List<ReconcileDetail> details = this.list(wrapper);
        ReconcileSummaryVO summary = buildSummary(details, null);
        summary.setReconcileDate(reconcileDate.toString());
        summary.setPayChannel(payChannel);
        return summary;
    }

    @Override
    public void handleDetail(Long detailId, Integer handleStatus, String handleRemark, String handleUserId, String handleUserName) {
        if (detailId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "差异明细ID不能为空");
        }
        if (handleStatus == null || ReconcileHandleStatusEnum.getByCode(handleStatus) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的处理状态");
        }
        ReconcileDetail detail = this.getById(detailId);
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "差异明细不存在");
        }
        detail.setHandleStatus(handleStatus);
        detail.setHandleRemark(handleRemark);
        detail.setHandleUserId(handleUserId);
        detail.setHandleUserName(handleUserName);
        detail.setHandleTime(LocalDateTime.now());
        this.updateById(detail);
        log.info("处理对账差异明细成功, detailId:{}, handleStatus:{}", detailId, handleStatus);
    }

    @Override
    public void exportDetails(String reconcileNo, HttpServletResponse response) {
        Map<String, Object> params = new HashMap<>();
        params.put("reconcileNo", reconcileNo);
        exportDetailsByCondition(params, response);
    }

    @Override
    public void exportDetailsByCondition(Map<String, Object> params, HttpServletResponse response) {
        LambdaQueryWrapper<ReconcileDetail> wrapper = buildQueryWrapper(params);
        wrapper.orderByDesc(ReconcileDetail::getReconcileDate, ReconcileDetail::getId);
        List<ReconcileDetail> details = this.list(wrapper);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=reconcile_details_" + System.currentTimeMillis() + ".csv");

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF');
            writer.println("差异单号,对账单号,对账日期,支付渠道,差异类型,平台订单号,商户号,渠道交易号,本地金额(分),渠道金额(分),差异金额(分),本地状态,渠道状态,本地支付时间,渠道支付时间,处理状态,处理备注,处理人,处理时间,创建时间");

            for (ReconcileDetail detail : details) {
                ReconcileDetailVO vo = convertToVO(detail);
                writer.print(escapeCsv(vo.getDetailNo()) + ",");
                writer.print(escapeCsv(vo.getReconcileNo()) + ",");
                writer.print(escapeCsv(String.valueOf(vo.getReconcileDate())) + ",");
                writer.print(escapeCsv(vo.getPayChannelDesc()) + ",");
                writer.print(escapeCsv(vo.getDiffTypeDesc()) + ",");
                writer.print(escapeCsv(vo.getOrderNo()) + ",");
                writer.print(escapeCsv(vo.getMerchantNo()) + ",");
                writer.print(escapeCsv(vo.getChannelTradeNo()) + ",");
                writer.print(escapeCsv(vo.getLocalAmount() != null ? vo.getLocalAmount().toString() : "") + ",");
                writer.print(escapeCsv(vo.getChannelAmount() != null ? vo.getChannelAmount().toString() : "") + ",");
                writer.print(escapeCsv(vo.getDiffAmount() != null ? vo.getDiffAmount().toString() : "") + ",");
                writer.print(escapeCsv(vo.getLocalStatusDesc()) + ",");
                writer.print(escapeCsv(vo.getChannelStatus()) + ",");
                writer.print(escapeCsv(vo.getLocalPayTime() != null ? vo.getLocalPayTime().format(DATE_FORMATTER) : "") + ",");
                writer.print(escapeCsv(vo.getChannelPayTime() != null ? vo.getChannelPayTime().format(DATE_FORMATTER) : "") + ",");
                writer.print(escapeCsv(vo.getHandleStatusDesc()) + ",");
                writer.print(escapeCsv(vo.getHandleRemark()) + ",");
                writer.print(escapeCsv(vo.getHandleUserName()) + ",");
                writer.print(escapeCsv(vo.getHandleTime() != null ? vo.getHandleTime().format(DATE_FORMATTER) : "") + ",");
                writer.println(escapeCsv(vo.getCreatedAt() != null ? vo.getCreatedAt().format(DATE_FORMATTER) : ""));
            }
            writer.flush();
        } catch (Exception e) {
            log.error("导出对账差异明细失败", e);
            throw new BusinessException(ResultCode.FAIL, "导出失败: " + e.getMessage());
        }
    }

    private LambdaQueryWrapper<ReconcileDetail> buildQueryWrapper(Map<String, Object> params) {
        LambdaQueryWrapper<ReconcileDetail> wrapper = new LambdaQueryWrapper<>();
        if (params != null) {
            if (params.get("reconcileNo") != null && StrUtil.isNotBlank(params.get("reconcileNo").toString())) {
                wrapper.eq(ReconcileDetail::getReconcileNo, params.get("reconcileNo"));
            }
            if (params.get("reconcileDate") != null) {
                wrapper.eq(ReconcileDetail::getReconcileDate, params.get("reconcileDate"));
            }
            if (params.get("payChannel") != null && StrUtil.isNotBlank(params.get("payChannel").toString())) {
                wrapper.eq(ReconcileDetail::getPayChannel, params.get("payChannel"));
            }
            if (params.get("diffType") != null) {
                wrapper.eq(ReconcileDetail::getDiffType, params.get("diffType"));
            }
            if (params.get("handleStatus") != null) {
                wrapper.eq(ReconcileDetail::getHandleStatus, params.get("handleStatus"));
            }
            if (params.get("orderNo") != null && StrUtil.isNotBlank(params.get("orderNo").toString())) {
                wrapper.eq(ReconcileDetail::getOrderNo, params.get("orderNo"));
            }
            if (params.get("merchantNo") != null && StrUtil.isNotBlank(params.get("merchantNo").toString())) {
                wrapper.eq(ReconcileDetail::getMerchantNo, params.get("merchantNo"));
            }
            if (params.get("channelTradeNo") != null && StrUtil.isNotBlank(params.get("channelTradeNo").toString())) {
                wrapper.eq(ReconcileDetail::getChannelTradeNo, params.get("channelTradeNo"));
            }
        }
        return wrapper;
    }

    private ReconcileSummaryVO buildSummary(List<ReconcileDetail> details, String reconcileNo) {
        ReconcileSummaryVO summary = new ReconcileSummaryVO();
        summary.setReconcileNo(reconcileNo);

        int matchCount = 0;
        int mismatchCount = 0;

        ReconcileSummaryVO.LongFundSummary longFund = new ReconcileSummaryVO.LongFundSummary();
        longFund.setCount(0);
        longFund.setTotalAmount(BigDecimal.ZERO);

        ReconcileSummaryVO.ShortFundSummary shortFund = new ReconcileSummaryVO.ShortFundSummary();
        shortFund.setCount(0);
        shortFund.setTotalAmount(BigDecimal.ZERO);

        ReconcileSummaryVO.AmountMismatchSummary amountMismatch = new ReconcileSummaryVO.AmountMismatchSummary();
        amountMismatch.setCount(0);
        amountMismatch.setTotalDiffAmount(BigDecimal.ZERO);

        ReconcileSummaryVO.StatusMismatchSummary statusMismatch = new ReconcileSummaryVO.StatusMismatchSummary();
        statusMismatch.setCount(0);

        if (CollUtil.isNotEmpty(details)) {
            for (ReconcileDetail detail : details) {
                mismatchCount++;
                ReconcileDiffTypeEnum diffTypeEnum = ReconcileDiffTypeEnum.getByCode(detail.getDiffType());
                if (diffTypeEnum == null) continue;

                switch (diffTypeEnum) {
                    case LONG_FUND:
                        longFund.setCount(longFund.getCount() + 1);
                        if (detail.getChannelAmount() != null) {
                            longFund.setTotalAmount(longFund.getTotalAmount().add(detail.getChannelAmount()));
                        }
                        break;
                    case SHORT_FUND:
                        shortFund.setCount(shortFund.getCount() + 1);
                        if (detail.getLocalAmount() != null) {
                            shortFund.setTotalAmount(shortFund.getTotalAmount().add(detail.getLocalAmount()));
                        }
                        break;
                    case AMOUNT_MISMATCH:
                        amountMismatch.setCount(amountMismatch.getCount() + 1);
                        if (detail.getDiffAmount() != null) {
                            amountMismatch.setTotalDiffAmount(amountMismatch.getTotalDiffAmount().add(detail.getDiffAmount().abs()));
                        }
                        break;
                    case STATUS_MISMATCH:
                        statusMismatch.setCount(statusMismatch.getCount() + 1);
                        break;
                }
            }
        }

        summary.setTotalCount(matchCount + mismatchCount);
        summary.setMatchCount(matchCount);
        summary.setMismatchCount(mismatchCount);
        summary.setLongFund(longFund);
        summary.setShortFund(shortFund);
        summary.setAmountMismatch(amountMismatch);
        summary.setStatusMismatch(statusMismatch);

        return summary;
    }

    private ReconcileDetailVO convertToVO(ReconcileDetail detail) {
        ReconcileDetailVO vo = BeanUtil.copyProperties(detail, ReconcileDetailVO.class);

        PayChannelEnum channelEnum = PayChannelEnum.getByCode(detail.getPayChannel());
        if (channelEnum != null) {
            vo.setPayChannelDesc(channelEnum.getDesc());
        }

        ReconcileDiffTypeEnum diffTypeEnum = ReconcileDiffTypeEnum.getByCode(detail.getDiffType());
        if (diffTypeEnum != null) {
            vo.setDiffTypeDesc(diffTypeEnum.getDesc());
        }

        if (detail.getLocalStatus() != null) {
            PayStatusEnum statusEnum = PayStatusEnum.getByCode(detail.getLocalStatus());
            if (statusEnum != null) {
                vo.setLocalStatusDesc(statusEnum.getDesc());
            }
        }

        ReconcileHandleStatusEnum handleStatusEnum = ReconcileHandleStatusEnum.getByCode(detail.getHandleStatus());
        if (handleStatusEnum != null) {
            vo.setHandleStatusDesc(handleStatusEnum.getDesc());
        }

        return vo;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
