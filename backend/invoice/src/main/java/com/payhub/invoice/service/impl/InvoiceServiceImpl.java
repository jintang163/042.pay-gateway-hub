package com.payhub.invoice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.HttpUtil;
import com.payhub.common.utils.SignUtil;
import com.payhub.invoice.config.InvoiceProperties;
import com.payhub.invoice.dto.*;
import com.payhub.invoice.entity.Invoice;
import com.payhub.invoice.entity.InvoiceChannelConfig;
import com.payhub.invoice.entity.InvoiceItem;
import com.payhub.invoice.enums.InvoiceStatusEnum;
import com.payhub.invoice.enums.InvoiceTitleTypeEnum;
import com.payhub.invoice.enums.InvoiceTypeEnum;
import com.payhub.invoice.mapper.InvoiceChannelConfigMapper;
import com.payhub.invoice.mapper.InvoiceItemMapper;
import com.payhub.invoice.mapper.InvoiceMapper;
import com.payhub.invoice.service.InvoiceService;
import com.payhub.invoice.strategy.InvoiceChannelStrategy;
import com.payhub.invoice.strategy.InvoiceChannelStrategyFactory;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.service.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements InvoiceService {

    private static final String DEFAULT_SIGN_TYPE = "MD5";
    private static final String DEFAULT_MERCHANT_SECRET = "payhub_default_secret_key_2024";

    @Autowired
    private InvoiceChannelStrategyFactory strategyFactory;

    @Autowired
    private InvoiceChannelConfigMapper channelConfigMapper;

    @Autowired
    private InvoiceItemMapper invoiceItemMapper;

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private InvoiceProperties invoiceProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceResult applyInvoice(InvoiceApplyRequest request) {
        LambdaQueryWrapper<PayOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PayOrder::getOrderNo, request.getOrderNo())
                .eq(PayOrder::getMerchantNo, request.getMerchantNo())
                .last("LIMIT 1");
        PayOrder order = payOrderService.getOne(orderWrapper);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!PayStatusEnum.SUCCESS.getCode().equals(order.getPayStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有支付成功的订单才能申请发票");
        }

        LambdaQueryWrapper<Invoice> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Invoice::getOrderNo, request.getOrderNo())
                .eq(Invoice::getMerchantNo, request.getMerchantNo())
                .eq(Invoice::getInvoiceType, InvoiceTypeEnum.BLUE.getCode())
                .in(Invoice::getInvoiceStatus,
                        InvoiceStatusEnum.PENDING.getCode(),
                        InvoiceStatusEnum.ISSUING.getCode(),
                        InvoiceStatusEnum.SUCCESS.getCode())
                .last("LIMIT 1");
        Invoice existInvoice = this.getOne(existWrapper);
        if (existInvoice != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该订单已申请过发票，发票号: " + existInvoice.getInvoiceNo());
        }

        String channelCode = StrUtil.isNotBlank(request.getChannelCode())
                ? request.getChannelCode()
                : invoiceProperties.getDefaultChannel();
        if (!strategyFactory.hasStrategy(channelCode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的发票渠道: " + channelCode);
        }

        InvoiceChannelConfig channelConfig = getChannelConfig(request.getMerchantNo(), channelCode);
        InvoiceChannelStrategy strategy = strategyFactory.getStrategy(channelCode);

        String invoiceNo = "INV" + System.currentTimeMillis();

        BigDecimal invoiceAmount = request.getInvoiceAmount() != null
                ? request.getInvoiceAmount()
                : order.getPayAmount();
        BigDecimal taxRate = new BigDecimal("0.06");
        BigDecimal taxAmount = invoiceAmount.multiply(taxRate).divide(new BigDecimal("1").add(taxRate), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal totalAmount = invoiceAmount;

        Invoice invoice = new Invoice();
        invoice.setInvoiceNo(invoiceNo);
        invoice.setMerchantNo(request.getMerchantNo());
        invoice.setOrderNo(request.getOrderNo());
        invoice.setChannelCode(channelCode);
        invoice.setInvoiceType(InvoiceTypeEnum.BLUE.getCode());
        invoice.setInvoiceStatus(InvoiceStatusEnum.PENDING.getCode());
        invoice.setTitleType(request.getTitleType());
        invoice.setBuyerTitle(request.getBuyerTitle());
        invoice.setBuyerTaxNo(request.getBuyerTaxNo());
        invoice.setBuyerAddress(request.getBuyerAddress());
        invoice.setBuyerBankName(request.getBuyerBankName());
        invoice.setBuyerBankAccount(request.getBuyerBankAccount());
        invoice.setBuyerPhone(request.getBuyerPhone());
        invoice.setBuyerEmail(request.getBuyerEmail());
        invoice.setInvoiceContent(StrUtil.isNotBlank(request.getInvoiceContent()) ? request.getInvoiceContent() : "商品明细");
        invoice.setInvoiceAmount(invoiceAmount);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
        invoice.setTaxRate("6%");
        invoice.setRemark(request.getRemark());
        invoice.setNotifyUrl(request.getNotifyUrl());
        this.save(invoice);

        saveInvoiceItems(invoice.getId(), invoiceNo, request.getItems(), invoiceAmount, taxRate);

        try {
            strategy.issueInvoice(request, channelConfig, invoice);
        } catch (Exception e) {
            log.error("调用发票渠道异常: invoiceNo={}, error={}", invoiceNo, e.getMessage(), e);
            invoice.setInvoiceStatus(InvoiceStatusEnum.FAILED.getCode());
            invoice.setFailReason("发票渠道调用异常: " + e.getMessage());
        }
        this.updateById(invoice);

        log.info("发票申请提交: invoiceNo={}, orderNo={}, channel={}, status={}",
                invoiceNo, request.getOrderNo(), channelCode, invoice.getInvoiceStatus());

        return convertToResult(invoice);
    }

    private void saveInvoiceItems(Long invoiceId, String invoiceNo, List<InvoiceItemDTO> items,
                                  BigDecimal defaultAmount, BigDecimal defaultTaxRate) {
        if (items == null || items.isEmpty()) {
            InvoiceItem item = new InvoiceItem();
            item.setInvoiceId(invoiceId);
            item.setInvoiceNo(invoiceNo);
            item.setItemName("商品明细");
            item.setItemCode("DEFAULT");
            item.setQuantity(BigDecimal.ONE);
            item.setUnitPrice(defaultAmount);
            item.setAmount(defaultAmount);
            item.setTaxAmount(defaultAmount.multiply(defaultTaxRate).divide(new BigDecimal("1").add(defaultTaxRate), 2, BigDecimal.ROUND_HALF_UP));
            item.setTaxRate(defaultTaxRate.multiply(new BigDecimal("100")).toPlainString() + "%");
            item.setTaxIncludedFlag(1);
            invoiceItemMapper.insert(item);
            return;
        }

        for (InvoiceItemDTO dto : items) {
            InvoiceItem item = new InvoiceItem();
            item.setInvoiceId(invoiceId);
            item.setInvoiceNo(invoiceNo);
            item.setItemName(dto.getItemName());
            item.setItemCode(dto.getItemCode());
            item.setSpecification(dto.getSpecification());
            item.setUnit(dto.getUnit());
            item.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : BigDecimal.ONE);
            item.setUnitPrice(dto.getUnitPrice());
            item.setAmount(dto.getAmount());
            item.setTaxAmount(dto.getTaxAmount());
            item.setTaxRate(StrUtil.isNotBlank(dto.getTaxRate()) ? dto.getTaxRate() : "6%");
            item.setTaxIncludedFlag(dto.getTaxIncludedFlag() != null ? dto.getTaxIncludedFlag() : 1);
            invoiceItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceResult redFlushInvoice(InvoiceRedFlushRequest request) {
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invoice::getInvoiceNo, request.getOriginalInvoiceNo())
                .eq(Invoice::getMerchantNo, request.getMerchantNo())
                .last("LIMIT 1");
        Invoice originalInvoice = this.getOne(wrapper);
        if (originalInvoice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "原发票不存在");
        }
        if (!InvoiceStatusEnum.SUCCESS.getCode().equals(originalInvoice.getInvoiceStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有开票成功的发票才能红冲");
        }
        if (InvoiceTypeEnum.RED.getCode().equals(originalInvoice.getInvoiceType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "红票不能再次红冲");
        }

        LambdaQueryWrapper<Invoice> redExistWrapper = new LambdaQueryWrapper<>();
        redExistWrapper.eq(Invoice::getOriginalInvoiceNo, request.getOriginalInvoiceNo())
                .eq(Invoice::getMerchantNo, request.getMerchantNo())
                .eq(Invoice::getInvoiceType, InvoiceTypeEnum.RED.getCode())
                .in(Invoice::getInvoiceStatus,
                        InvoiceStatusEnum.RED_PENDING.getCode(),
                        InvoiceStatusEnum.RED_ISSUING.getCode(),
                        InvoiceStatusEnum.RED_SUCCESS.getCode())
                .last("LIMIT 1");
        Invoice redExist = this.getOne(redExistWrapper);
        if (redExist != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该发票已申请红冲，红冲发票号: " + redExist.getInvoiceNo());
        }

        String channelCode = originalInvoice.getChannelCode();
        InvoiceChannelConfig channelConfig = getChannelConfig(request.getMerchantNo(), channelCode);
        InvoiceChannelStrategy strategy = strategyFactory.getStrategy(channelCode);

        String redInvoiceNo = "INVRED" + System.currentTimeMillis();

        Invoice redInvoice = new Invoice();
        redInvoice.setInvoiceNo(redInvoiceNo);
        redInvoice.setMerchantNo(request.getMerchantNo());
        redInvoice.setOrderNo(originalInvoice.getOrderNo());
        redInvoice.setChannelCode(channelCode);
        redInvoice.setInvoiceType(InvoiceTypeEnum.RED.getCode());
        redInvoice.setInvoiceStatus(InvoiceStatusEnum.RED_PENDING.getCode());
        redInvoice.setTitleType(originalInvoice.getTitleType());
        redInvoice.setBuyerTitle(originalInvoice.getBuyerTitle());
        redInvoice.setBuyerTaxNo(originalInvoice.getBuyerTaxNo());
        redInvoice.setBuyerAddress(originalInvoice.getBuyerAddress());
        redInvoice.setBuyerBankName(originalInvoice.getBuyerBankName());
        redInvoice.setBuyerBankAccount(originalInvoice.getBuyerBankAccount());
        redInvoice.setBuyerPhone(originalInvoice.getBuyerPhone());
        redInvoice.setBuyerEmail(originalInvoice.getBuyerEmail());
        redInvoice.setInvoiceContent("红字发票-" + originalInvoice.getInvoiceContent());
        redInvoice.setInvoiceAmount(originalInvoice.getInvoiceAmount().negate());
        redInvoice.setTaxAmount(originalInvoice.getTaxAmount().negate());
        redInvoice.setTotalAmount(originalInvoice.getTotalAmount().negate());
        redInvoice.setTaxRate(originalInvoice.getTaxRate());
        redInvoice.setOriginalInvoiceNo(request.getOriginalInvoiceNo());
        redInvoice.setRedReason(request.getRedReason());
        redInvoice.setRemark(request.getRedReason());
        redInvoice.setNotifyUrl(request.getNotifyUrl());
        this.save(redInvoice);

        try {
            strategy.redFlushInvoice(request, channelConfig, originalInvoice);
            redInvoice.setInvoiceStatus(InvoiceStatusEnum.RED_ISSUING.getCode());
            redInvoice.setChannelInvoiceNo(originalInvoice.getChannelInvoiceNo() + "_RED");
        } catch (Exception e) {
            log.error("调用发票红冲异常: originalInvoiceNo={}, error={}", request.getOriginalInvoiceNo(), e.getMessage(), e);
            redInvoice.setInvoiceStatus(InvoiceStatusEnum.RED_FAILED.getCode());
            redInvoice.setFailReason("发票红冲渠道调用异常: " + e.getMessage());
        }
        this.updateById(redInvoice);

        originalInvoice.setInvoiceStatus(InvoiceStatusEnum.RED_ISSUING.getCode());
        this.updateById(originalInvoice);

        log.info("发票红冲申请提交: redInvoiceNo={}, originalInvoiceNo={}, status={}",
                redInvoiceNo, request.getOriginalInvoiceNo(), redInvoice.getInvoiceStatus());

        return convertToResult(redInvoice);
    }

    @Override
    public List<InvoiceResult> batchApplyInvoice(List<InvoiceApplyRequest> requests) {
        List<InvoiceResult> results = new ArrayList<>();
        for (InvoiceApplyRequest request : requests) {
            try {
                results.add(applyInvoice(request));
            } catch (Exception e) {
                log.error("批量开票失败: orderNo={}, error={}", request.getOrderNo(), e.getMessage());
                InvoiceResult failResult = new InvoiceResult();
                failResult.setOrderNo(request.getOrderNo());
                failResult.setInvoiceStatus(InvoiceStatusEnum.FAILED.getCode());
                failResult.setInvoiceStatusDesc("开票失败: " + e.getMessage());
                results.add(failResult);
            }
        }
        return results;
    }

    @Override
    public InvoiceResult getInvoiceDetail(String invoiceNo, String merchantNo) {
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invoice::getInvoiceNo, invoiceNo)
                .eq(Invoice::getMerchantNo, merchantNo)
                .last("LIMIT 1");
        Invoice invoice = this.getOne(wrapper);
        if (invoice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "发票不存在");
        }
        InvoiceResult result = convertToResult(invoice);

        LambdaQueryWrapper<InvoiceItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InvoiceItem::getInvoiceId, invoice.getId());
        List<InvoiceItem> items = invoiceItemMapper.selectList(itemWrapper);
        if (items != null && !items.isEmpty()) {
            List<InvoiceItemDTO> itemDTOs = new ArrayList<>();
            for (InvoiceItem item : items) {
                InvoiceItemDTO dto = InvoiceItemDTO.builder()
                        .itemName(item.getItemName())
                        .itemCode(item.getItemCode())
                        .specification(item.getSpecification())
                        .unit(item.getUnit())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .amount(item.getAmount())
                        .taxAmount(item.getTaxAmount())
                        .taxRate(item.getTaxRate())
                        .taxIncludedFlag(item.getTaxIncludedFlag())
                        .build();
                itemDTOs.add(dto);
            }
            result.setItems(itemDTOs);
        }

        return result;
    }

    @Override
    public IPage<InvoiceResult> listPage(InvoiceQueryRequest request) {
        Page<Invoice> page = new Page<>(request.getCurrent(), request.getSize());
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invoice::getMerchantNo, request.getMerchantNo());

        if (StrUtil.isNotBlank(request.getInvoiceNo())) {
            wrapper.eq(Invoice::getInvoiceNo, request.getInvoiceNo());
        }
        if (StrUtil.isNotBlank(request.getOrderNo())) {
            wrapper.eq(Invoice::getOrderNo, request.getOrderNo());
        }
        if (StrUtil.isNotBlank(request.getChannelInvoiceNo())) {
            wrapper.eq(Invoice::getChannelInvoiceNo, request.getChannelInvoiceNo());
        }
        if (StrUtil.isNotBlank(request.getChannelCode())) {
            wrapper.eq(Invoice::getChannelCode, request.getChannelCode());
        }
        if (request.getInvoiceType() != null) {
            wrapper.eq(Invoice::getInvoiceType, request.getInvoiceType());
        }
        if (request.getInvoiceStatus() != null) {
            wrapper.eq(Invoice::getInvoiceStatus, request.getInvoiceStatus());
        }
        if (StrUtil.isNotBlank(request.getBuyerTitle())) {
            wrapper.like(Invoice::getBuyerTitle, request.getBuyerTitle());
        }
        if (StrUtil.isNotBlank(request.getStartTime())) {
            wrapper.ge(Invoice::getCreatedAt, request.getStartTime());
        }
        if (StrUtil.isNotBlank(request.getEndTime())) {
            wrapper.le(Invoice::getCreatedAt, request.getEndTime());
        }

        wrapper.orderByDesc(Invoice::getCreatedAt);
        IPage<Invoice> invoicePage = this.page(page, wrapper);

        Page<InvoiceResult> resultPage = new Page<>(invoicePage.getCurrent(), invoicePage.getSize(), invoicePage.getTotal());
        List<InvoiceResult> records = new ArrayList<>();
        for (Invoice invoice : invoicePage.getRecords()) {
            records.add(convertToResult(invoice));
        }
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(String channel, Map<String, String> params, String body) {
        log.info("接收发票异步回调, channel={}, params={}, body={}", channel, params, body);

        InvoiceChannelStrategy strategy;
        try {
            strategy = strategyFactory.getStrategy(channel);
        } catch (Exception e) {
            log.warn("不支持的发票渠道: {}", channel);
            return "fail";
        }

        InvoiceChannelConfig config = null;
        if (!strategy.verifyCallback(params, body, config)) {
            log.warn("发票回调签名验证失败, channel={}, params={}", channel, params);
            return "fail";
        }

        InvoiceCallbackResult callbackResult = strategy.parseCallback(params, body);
        if (callbackResult == null || StrUtil.isBlank(callbackResult.getInvoiceNo())) {
            log.warn("解析发票回调失败, channel={}, body={}", channel, body);
            return "fail";
        }

        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invoice::getInvoiceNo, callbackResult.getInvoiceNo())
                .last("LIMIT 1");
        Invoice invoice = this.getOne(wrapper);
        if (invoice == null) {
            log.warn("发票不存在, invoiceNo={}", callbackResult.getInvoiceNo());
            return "success";
        }

        invoice.setInvoiceStatus(callbackResult.getInvoiceStatus());
        if (StrUtil.isNotBlank(callbackResult.getChannelInvoiceNo())) {
            invoice.setChannelInvoiceNo(callbackResult.getChannelInvoiceNo());
        }
        if (StrUtil.isNotBlank(callbackResult.getPdfUrl())) {
            invoice.setPdfUrl(callbackResult.getPdfUrl());
        }
        if (callbackResult.getIssueTime() != null) {
            invoice.setIssueTime(callbackResult.getIssueTime());
        }
        if (callbackResult.getInvoiceAmount() != null) {
            invoice.setInvoiceAmount(callbackResult.getInvoiceAmount());
        }
        if (callbackResult.getTaxAmount() != null) {
            invoice.setTaxAmount(callbackResult.getTaxAmount());
        }
        if (callbackResult.getTotalAmount() != null) {
            invoice.setTotalAmount(callbackResult.getTotalAmount());
        }
        if (InvoiceStatusEnum.FAILED.getCode().equals(callbackResult.getInvoiceStatus())
                || InvoiceStatusEnum.RED_FAILED.getCode().equals(callbackResult.getInvoiceStatus())) {
            invoice.setFailReason(callbackResult.getFailReason());
        }
        this.updateById(invoice);

        if (InvoiceStatusEnum.SUCCESS.getCode().equals(callbackResult.getInvoiceStatus())
                || InvoiceStatusEnum.RED_SUCCESS.getCode().equals(callbackResult.getInvoiceStatus())
                || InvoiceStatusEnum.FAILED.getCode().equals(callbackResult.getInvoiceStatus())
                || InvoiceStatusEnum.RED_FAILED.getCode().equals(callbackResult.getInvoiceStatus())) {
            notifyMerchantAsync(invoice);
        }

        if (InvoiceStatusEnum.RED_SUCCESS.getCode().equals(callbackResult.getInvoiceStatus())
                && StrUtil.isNotBlank(invoice.getOriginalInvoiceNo())) {
            LambdaQueryWrapper<Invoice> origWrapper = new LambdaQueryWrapper<>();
            origWrapper.eq(Invoice::getInvoiceNo, invoice.getOriginalInvoiceNo())
                    .last("LIMIT 1");
            Invoice origInvoice = this.getOne(origWrapper);
            if (origInvoice != null) {
                origInvoice.setInvoiceStatus(InvoiceStatusEnum.RED_SUCCESS.getCode());
                this.updateById(origInvoice);
            }
        }

        log.info("发票回调处理完成: invoiceNo={}, status={}", invoice.getInvoiceNo(), invoice.getInvoiceStatus());
        return "success";
    }

    @Async
    public void notifyMerchantAsync(Invoice invoice) {
        notifyMerchant(invoice);
    }

    private void notifyMerchant(Invoice invoice) {
        if (StrUtil.isBlank(invoice.getNotifyUrl())) {
            log.info("商户发票通知地址为空, 跳过通知, invoiceNo={}", invoice.getInvoiceNo());
            return;
        }
        try {
            Map<String, Object> notifyParams = buildNotifyParams(invoice);
            String sign = SignUtil.sign(notifyParams, DEFAULT_SIGN_TYPE, DEFAULT_MERCHANT_SECRET, null, null);
            notifyParams.put("sign", sign);
            notifyParams.put("signType", DEFAULT_SIGN_TYPE);

            log.info("通知商户发票结果, invoiceNo={}, notifyUrl={}, params={}",
                    invoice.getInvoiceNo(), invoice.getNotifyUrl(), notifyParams);

            String response = HttpUtil.postJson(invoice.getNotifyUrl(), notifyParams);
            log.info("通知商户发票结果, invoiceNo={}, response={}", invoice.getInvoiceNo(), response);

            if (response == null || !"success".equalsIgnoreCase(response.trim())) {
                log.warn("商户发票通知响应异常, invoiceNo={}, response={}", invoice.getInvoiceNo(), response);
            }
        } catch (Exception e) {
            log.error("通知商户发票结果失败, invoiceNo={}, notifyUrl={}",
                    invoice.getInvoiceNo(), invoice.getNotifyUrl(), e);
        }
    }

    private Map<String, Object> buildNotifyParams(Invoice invoice) {
        Map<String, Object> params = new TreeMap<>();
        params.put("merchantNo", invoice.getMerchantNo());
        params.put("invoiceNo", invoice.getInvoiceNo());
        params.put("orderNo", invoice.getOrderNo());
        params.put("channelInvoiceNo", invoice.getChannelInvoiceNo());
        params.put("channelCode", invoice.getChannelCode());
        params.put("invoiceType", invoice.getInvoiceType());
        params.put("invoiceStatus", invoice.getInvoiceStatus());
        params.put("buyerTitle", invoice.getBuyerTitle());
        params.put("invoiceAmount", invoice.getInvoiceAmount());
        params.put("taxAmount", invoice.getTaxAmount());
        params.put("totalAmount", invoice.getTotalAmount());
        params.put("pdfUrl", invoice.getPdfUrl());
        params.put("failReason", invoice.getFailReason());

        if (invoice.getIssueTime() != null) {
            params.put("issueTime", invoice.getIssueTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (invoice.getOriginalInvoiceNo() != null) {
            params.put("originalInvoiceNo", invoice.getOriginalInvoiceNo());
        }
        return params;
    }

    @Override
    public String downloadPdf(String invoiceNo, String merchantNo) {
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invoice::getInvoiceNo, invoiceNo)
                .eq(Invoice::getMerchantNo, merchantNo)
                .last("LIMIT 1");
        Invoice invoice = this.getOne(wrapper);
        if (invoice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "发票不存在");
        }
        if (!InvoiceStatusEnum.SUCCESS.getCode().equals(invoice.getInvoiceStatus())
                && !InvoiceStatusEnum.RED_SUCCESS.getCode().equals(invoice.getInvoiceStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "发票未开具成功，无法下载");
        }

        InvoiceChannelConfig channelConfig = getChannelConfig(merchantNo, invoice.getChannelCode());
        InvoiceChannelStrategy strategy = strategyFactory.getStrategy(invoice.getChannelCode());
        String pdfUrl = strategy.downloadPdfUrl(invoice, channelConfig);

        if (StrUtil.isBlank(pdfUrl) && StrUtil.isNotBlank(invoice.getPdfUrl())) {
            pdfUrl = invoice.getPdfUrl();
        }

        if (StrUtil.isBlank(pdfUrl)) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "发票PDF地址获取失败");
        }
        return pdfUrl;
    }

    @Override
    public InvoiceResult queryInvoiceStatus(String invoiceNo, String merchantNo) {
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invoice::getInvoiceNo, invoiceNo)
                .eq(Invoice::getMerchantNo, merchantNo)
                .last("LIMIT 1");
        Invoice invoice = this.getOne(wrapper);
        if (invoice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "发票不存在");
        }

        if (InvoiceStatusEnum.ISSUING.getCode().equals(invoice.getInvoiceStatus())
                || InvoiceStatusEnum.RED_ISSUING.getCode().equals(invoice.getInvoiceStatus())) {
            try {
                InvoiceChannelConfig channelConfig = getChannelConfig(merchantNo, invoice.getChannelCode());
                InvoiceChannelStrategy strategy = strategyFactory.getStrategy(invoice.getChannelCode());
                strategy.queryInvoiceStatus(invoice, channelConfig);
                this.updateById(invoice);
            } catch (Exception e) {
                log.warn("查询发票状态异常: invoiceNo={}, error={}", invoiceNo, e.getMessage());
            }
        }

        return convertToResult(invoice);
    }

    private InvoiceChannelConfig getChannelConfig(String merchantNo, String channelCode) {
        LambdaQueryWrapper<InvoiceChannelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvoiceChannelConfig::getMerchantNo, merchantNo)
                .eq(InvoiceChannelConfig::getChannelCode, channelCode)
                .eq(InvoiceChannelConfig::getEnabled, 1)
                .last("LIMIT 1");
        InvoiceChannelConfig config = channelConfigMapper.selectOne(wrapper);
        if (config == null) {
            log.warn("未找到商户发票渠道配置: merchantNo={}, channelCode={}, 使用系统默认配置", merchantNo, channelCode);
            config = new InvoiceChannelConfig();
            config.setMerchantNo(merchantNo);
            config.setChannelCode(channelCode);
        }
        return config;
    }

    private InvoiceResult convertToResult(Invoice invoice) {
        InvoiceStatusEnum statusEnum = InvoiceStatusEnum.getByCode(invoice.getInvoiceStatus());
        InvoiceTitleTypeEnum titleTypeEnum = InvoiceTitleTypeEnum.getByCode(invoice.getTitleType());

        return InvoiceResult.builder()
                .id(invoice.getId())
                .invoiceNo(invoice.getInvoiceNo())
                .merchantNo(invoice.getMerchantNo())
                .orderNo(invoice.getOrderNo())
                .channelInvoiceNo(invoice.getChannelInvoiceNo())
                .channelCode(invoice.getChannelCode())
                .invoiceType(invoice.getInvoiceType())
                .invoiceStatus(invoice.getInvoiceStatus())
                .invoiceStatusDesc(statusEnum != null ? statusEnum.getDesc() : String.valueOf(invoice.getInvoiceStatus()))
                .titleType(invoice.getTitleType())
                .titleTypeDesc(titleTypeEnum != null ? titleTypeEnum.getDesc() : String.valueOf(invoice.getTitleType()))
                .buyerTitle(invoice.getBuyerTitle())
                .buyerTaxNo(invoice.getBuyerTaxNo())
                .buyerAddress(invoice.getBuyerAddress())
                .buyerBankName(invoice.getBuyerBankName())
                .buyerBankAccount(invoice.getBuyerBankAccount())
                .buyerPhone(invoice.getBuyerPhone())
                .buyerEmail(invoice.getBuyerEmail())
                .invoiceContent(invoice.getInvoiceContent())
                .invoiceAmount(invoice.getInvoiceAmount())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .taxRate(invoice.getTaxRate())
                .pdfUrl(invoice.getPdfUrl())
                .originalInvoiceNo(invoice.getOriginalInvoiceNo())
                .redReason(invoice.getRedReason())
                .remark(invoice.getRemark())
                .failReason(invoice.getFailReason())
                .issueTime(invoice.getIssueTime())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
