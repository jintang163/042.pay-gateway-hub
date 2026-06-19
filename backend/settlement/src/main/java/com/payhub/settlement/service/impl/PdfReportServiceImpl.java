package com.payhub.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.mapper.PayOrderMapper;
import com.payhub.settlement.entity.SettlementRecord;
import com.payhub.settlement.mapper.SettlementRecordMapper;
import com.payhub.settlement.service.PdfReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PdfReportServiceImpl implements PdfReportService {

    @Autowired
    private SettlementRecordMapper settlementRecordMapper;

    @Autowired
    private PayOrderMapper payOrderMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<Map<String, Object>> generateSettlementReportData(String merchantNo, LocalDate startDate, LocalDate endDate) {
        log.info("生成结算报表数据: merchantNo={}, startDate={}, endDate={}", merchantNo, startDate, endDate);

        List<Map<String, Object>> result = new ArrayList<>();

        LambdaQueryWrapper<SettlementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SettlementRecord::getMerchantNo, merchantNo)
                .between(SettlementRecord::getSettleDate, startDate, endDate)
                .orderByDesc(SettlementRecord::getSettleDate);
        List<SettlementRecord> records = settlementRecordMapper.selectList(wrapper);

        log.info("查询到结算记录数量: {}", records.size());

        for (SettlementRecord record : records) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", record.getId());
            data.put("settlementNo", record.getSettlementNo());
            data.put("merchantNo", record.getMerchantNo());
            data.put("settleDate", record.getSettleDate() != null ? record.getSettleDate().format(DATE_FORMATTER) : "");
            data.put("totalAmount", record.getTotalAmount() != null ? record.getTotalAmount().toPlainString() : "0.00");
            data.put("feeAmount", record.getFeeAmount() != null ? record.getFeeAmount().toPlainString() : "0.00");
            data.put("actualSettleAmount", record.getActualSettleAmount() != null ? record.getActualSettleAmount().toPlainString() : "0.00");
            data.put("orderCount", record.getOrderCount() != null ? record.getOrderCount() : 0);
            data.put("payChannel", record.getPayChannel() != null ? record.getPayChannel() : "");
            data.put("settleStatus", record.getSettleStatus() != null ? record.getSettleStatus() : 0);
            data.put("settleStatusDesc", convertSettleStatus(record.getSettleStatus()));
            data.put("bankName", record.getBankName() != null ? record.getBankName() : "");
            data.put("bankAccount", record.getBankAccount() != null ? maskBankAccount(record.getBankAccount()) : "");
            data.put("accountName", record.getAccountName() != null ? record.getAccountName() : "");
            data.put("failReason", record.getFailReason() != null ? record.getFailReason() : "");
            data.put("settleTime", record.getSettleTime() != null ? record.getSettleTime().format(DATETIME_FORMATTER) : "");
            result.add(data);
        }

        LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.MAX);
        LambdaQueryWrapper<PayOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PayOrder::getMerchantNo, merchantNo)
                .eq(PayOrder::getPayStatus, 1)
                .between(PayOrder::getPayTime, startTime, endTime);
        Long orderCount = payOrderMapper.selectCount(orderWrapper);

        log.info("日期范围内支付订单总数: {}", orderCount);

        return result;
    }

    @Override
    public byte[] generateSettlementPdf(String merchantNo, String merchantName, LocalDate startDate, LocalDate endDate,
                                         List<SettlementRecord> settlementRecords) {
        log.info("生成结算报表PDF: merchantNo={}, merchantName={}, 日期范围={} ~ {}, 记录数={}",
                merchantNo, merchantName, startDate, endDate,
                settlementRecords != null ? settlementRecords.size() : 0);

        List<SettlementRecord> records = settlementRecords;
        if (records == null || records.isEmpty()) {
            LambdaQueryWrapper<SettlementRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SettlementRecord::getMerchantNo, merchantNo)
                    .between(SettlementRecord::getSettleDate, startDate, endDate)
                    .orderByDesc(SettlementRecord::getSettleDate);
            records = settlementRecordMapper.selectList(wrapper);
        }

        String html = buildSettlementReportHtml(merchantNo, merchantName, startDate, endDate, records);

        log.info("报表HTML生成完成, 长度={} 字符", html.length());

        return htmlToPdfBytes(html);
    }

    private byte[] htmlToPdfBytes(String html) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            ByteArrayInputStream bais = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
            InputSource is = new InputSource(bais);
            is.setEncoding("UTF-8");
            Document w3cDoc = docBuilder.parse(is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            PdfRendererBuilder pdfBuilder = new PdfRendererBuilder();
            pdfBuilder.useFastMode();
            pdfBuilder.withW3cDocument(w3cDoc, null);
            pdfBuilder.toStream(baos);
            pdfBuilder.run();

            byte[] pdfBytes = baos.toByteArray();
            log.info("PDF 生成成功, PDF 大小: {} bytes ({} KB)", pdfBytes.length, String.format("%.2f", pdfBytes.length / 1024.0));
            return pdfBytes;
        } catch (Exception e) {
            log.error("HTML转PDF失败: {}", e.getMessage(), e);
            throw new RuntimeException("PDF生成失败: " + e.getMessage(), e);
        }
    }

    private String buildSettlementReportHtml(String merchantNo, String merchantName,
                                              LocalDate startDate, LocalDate endDate,
                                              List<SettlementRecord> records) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <title>商户结算报表</title>\n");
        html.append("    <style>\n");
        html.append("        @page { size: A4; margin: 20mm; }\n");
        html.append("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("        body { font-family: 'SimSun', 'Microsoft YaHei', Arial, sans-serif; font-size: 12px; color: #333; }\n");
        html.append("        .report-container { width: 100%; }\n");
        html.append("        .report-header { text-align: center; padding-bottom: 15px; margin-bottom: 20px; border-bottom: 2px solid #2c5aa0; }\n");
        html.append("        .report-title { font-size: 22px; font-weight: bold; color: #2c5aa0; margin-bottom: 8px; }\n");
        html.append("        .report-subtitle { font-size: 12px; color: #666; line-height: 1.6; }\n");
        html.append("        .info-section { background: #f8fafd; border: 1px solid #dce8f5; padding: 12px; margin-bottom: 18px; }\n");
        html.append("        .info-row { display: flex; margin-bottom: 6px; font-size: 12px; }\n");
        html.append("        .info-row:last-child { margin-bottom: 0; }\n");
        html.append("        .info-label { width: 100px; font-weight: bold; color: #555; }\n");
        html.append("        .info-value { color: #333; flex: 1; }\n");
        html.append("        .summary-section { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 18px; }\n");
        html.append("        .summary-card { border: 1px solid #ddd; padding: 10px; }\n");
        html.append("        .summary-label { font-size: 11px; color: #666; margin-bottom: 6px; }\n");
        html.append("        .summary-value { font-size: 18px; font-weight: bold; color: #2c5aa0; }\n");
        html.append("        .summary-unit { font-size: 11px; font-weight: normal; color: #666; margin-left: 2px; }\n");
        html.append("        .table-section { margin-bottom: 18px; }\n");
        html.append("        .table-title { font-size: 14px; font-weight: bold; color: #2c5aa0; margin-bottom: 10px; padding-left: 8px; border-left: 3px solid #2c5aa0; }\n");
        html.append("        table { width: 100%; border-collapse: collapse; font-size: 11px; }\n");
        html.append("        thead th { background: #2c5aa0; color: #fff; padding: 8px 6px; text-align: left; font-weight: 600; border: 1px solid #2c5aa0; }\n");
        html.append("        tbody td { padding: 7px 6px; border: 1px solid #ddd; color: #555; }\n");
        html.append("        .text-right { text-align: right; }\n");
        html.append("        .text-center { text-align: center; }\n");
        html.append("        .status-pending { display: inline-block; padding: 2px 6px; background: #fff3cd; color: #856404; font-size: 10px; }\n");
        html.append("        .status-processing { display: inline-block; padding: 2px 6px; background: #cce5ff; color: #004085; font-size: 10px; }\n");
        html.append("        .status-success { display: inline-block; padding: 2px 6px; background: #d4edda; color: #155724; font-size: 10px; }\n");
        html.append("        .status-fail { display: inline-block; padding: 2px 6px; background: #f8d7da; color: #721c24; font-size: 10px; }\n");
        html.append("        .amount-positive { color: #155724; font-weight: 600; }\n");
        html.append("        .report-footer { text-align: center; padding-top: 15px; border-top: 1px solid #ddd; color: #999; font-size: 10px; line-height: 1.6; margin-top: 20px; }\n");
        html.append("        .empty-tip { text-align: center; padding: 30px 10px; color: #999; background: #fafafa; border: 1px solid #ddd; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"report-container\">\n");

        html.append("        <div class=\"report-header\">\n");
        html.append("            <div class=\"report-title\">商户结算报表</div>\n");
        html.append("            <div class=\"report-subtitle\">\n");
        html.append("                Merchant Settlement Report &nbsp;&nbsp;|&nbsp;&nbsp;\n");
        html.append("                报表生成时间: ").append(LocalDateTime.now().format(DATETIME_FORMATTER)).append("\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        html.append("        <div class=\"info-section\">\n");
        html.append("            <div class=\"info-row\">\n");
        html.append("                <span class=\"info-label\">商户编号:</span>\n");
        html.append("                <span class=\"info-value\">").append(merchantNo != null ? merchantNo : "-").append("</span>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"info-row\">\n");
        html.append("                <span class=\"info-label\">商户名称:</span>\n");
        html.append("                <span class=\"info-value\">").append(merchantName != null ? merchantName : "-").append("</span>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"info-row\">\n");
        html.append("                <span class=\"info-label\">结算周期:</span>\n");
        html.append("                <span class=\"info-value\">\n");
        html.append("                    ").append(startDate != null ? startDate.format(DATE_FORMATTER) : "-");
        html.append("                    至 ").append(endDate != null ? endDate.format(DATE_FORMATTER) : "-").append("\n");
        html.append("                </span>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        BigDecimal totalAmountSum = BigDecimal.ZERO;
        BigDecimal feeAmountSum = BigDecimal.ZERO;
        BigDecimal actualSettleSum = BigDecimal.ZERO;
        int totalOrderCount = 0;

        if (records != null) {
            for (SettlementRecord r : records) {
                totalAmountSum = totalAmountSum.add(r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO);
                feeAmountSum = feeAmountSum.add(r.getFeeAmount() != null ? r.getFeeAmount() : BigDecimal.ZERO);
                actualSettleSum = actualSettleSum.add(r.getActualSettleAmount() != null ? r.getActualSettleAmount() : BigDecimal.ZERO);
                totalOrderCount += r.getOrderCount() != null ? r.getOrderCount() : 0;
            }
        }
        int recordCount = records != null ? records.size() : 0;

        html.append("        <div class=\"summary-section\">\n");
        html.append("            <div class=\"summary-card\">\n");
        html.append("                <div class=\"summary-label\">结算笔数</div>\n");
        html.append("                <div class=\"summary-value\">").append(recordCount).append("<span class=\"summary-unit\">笔</span></div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"summary-card\">\n");
        html.append("                <div class=\"summary-label\">订单总数</div>\n");
        html.append("                <div class=\"summary-value\">").append(totalOrderCount).append("<span class=\"summary-unit\">单</span></div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"summary-card\">\n");
        html.append("                <div class=\"summary-label\">交易总额</div>\n");
        html.append("                <div class=\"summary-value\">¥").append(totalAmountSum.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"summary-card\">\n");
        html.append("                <div class=\"summary-label\">实际结算</div>\n");
        html.append("                <div class=\"summary-value\">¥").append(actualSettleSum.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()).append("</div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        html.append("        <div class=\"table-section\">\n");
        html.append("            <div class=\"table-title\">结算明细</div>\n");

        if (records == null || records.isEmpty()) {
            html.append("            <div class=\"empty-tip\">该时间段内暂无结算记录</div>\n");
        } else {
            html.append("            <table>\n");
            html.append("                <thead>\n");
            html.append("                    <tr>\n");
            html.append("                        <th>序号</th>\n");
            html.append("                        <th>结算单号</th>\n");
            html.append("                        <th>结算日期</th>\n");
            html.append("                        <th>支付渠道</th>\n");
            html.append("                        <th class=\"text-right\">订单数</th>\n");
            html.append("                        <th class=\"text-right\">交易金额(元)</th>\n");
            html.append("                        <th class=\"text-right\">手续费(元)</th>\n");
            html.append("                        <th class=\"text-right\">实际结算(元)</th>\n");
            html.append("                        <th class=\"text-center\">状态</th>\n");
            html.append("                        <th>收款账户</th>\n");
            html.append("                    </tr>\n");
            html.append("                </thead>\n");
            html.append("                <tbody>\n");

            int index = 1;
            for (SettlementRecord record : records) {
                html.append("                    <tr>\n");
                html.append("                        <td>").append(index++).append("</td>\n");
                html.append("                        <td>").append(record.getSettlementNo() != null ? record.getSettlementNo() : "-").append("</td>\n");
                html.append("                        <td>").append(record.getSettleDate() != null ? record.getSettleDate().format(DATE_FORMATTER) : "-").append("</td>\n");
                html.append("                        <td>").append(record.getPayChannel() != null ? record.getPayChannel() : "-").append("</td>\n");
                html.append("                        <td class=\"text-right\">").append(record.getOrderCount() != null ? record.getOrderCount() : 0).append("</td>\n");
                html.append("                        <td class=\"text-right amount-positive\">").append(record.getTotalAmount() != null ? record.getTotalAmount().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() : "0.00").append("</td>\n");
                html.append("                        <td class=\"text-right\">").append(record.getFeeAmount() != null ? record.getFeeAmount().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() : "0.00").append("</td>\n");
                html.append("                        <td class=\"text-right amount-positive\">").append(record.getActualSettleAmount() != null ? record.getActualSettleAmount().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() : "0.00").append("</td>\n");
                html.append("                        <td class=\"text-center\">").append(buildStatusBadge(record.getSettleStatus())).append("</td>\n");
                html.append("                        <td>").append(buildAccountInfo(record)).append("</td>\n");
                html.append("                    </tr>\n");
            }

            html.append("                </tbody>\n");
            html.append("            </table>\n");
        }
        html.append("        </div>\n");

        if (records != null && !records.isEmpty()) {
            html.append("        <div class=\"table-section\">\n");
            html.append("            <div class=\"table-title\">汇总信息</div>\n");
            html.append("            <table>\n");
            html.append("                <thead>\n");
            html.append("                    <tr>\n");
            html.append("                        <th>汇总项</th>\n");
            html.append("                        <th class=\"text-right\">金额(元)</th>\n");
            html.append("                        <th>说明</th>\n");
            html.append("                    </tr>\n");
            html.append("                </thead>\n");
            html.append("                <tbody>\n");
            html.append("                    <tr>\n");
            html.append("                        <td>交易总额</td>\n");
            html.append("                        <td class=\"text-right amount-positive\">").append(totalAmountSum.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()).append("</td>\n");
            html.append("                        <td>所有成功支付订单金额之和</td>\n");
            html.append("                    </tr>\n");
            html.append("                    <tr>\n");
            html.append("                        <td>手续费合计</td>\n");
            html.append("                        <td class=\"text-right\">").append(feeAmountSum.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()).append("</td>\n");
            html.append("                        <td>按各渠道费率计算的手续费</td>\n");
            html.append("                    </tr>\n");
            html.append("                    <tr>\n");
            html.append("                        <td><b>实际结算金额</b></td>\n");
            html.append("                        <td class=\"text-right\"><b class=\"amount-positive\">").append(actualSettleSum.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()).append("</b></td>\n");
            html.append("                        <td>交易总额 - 手续费合计</td>\n");
            html.append("                    </tr>\n");
            html.append("                </tbody>\n");
            html.append("            </table>\n");
            html.append("        </div>\n");
        }

        html.append("        <div class=\"report-footer\">\n");
        html.append("            <p>本报表由支付网关聚合平台自动生成 · 仅供对账参考</p>\n");
        html.append("            <p>如有疑问，请联系商户服务中心</p>\n");
        html.append("        </div>\n");

        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    private String buildStatusBadge(Integer status) {
        String statusClass;
        String statusDesc;
        switch (status != null ? status : -1) {
            case 0:
                statusClass = "status-pending";
                statusDesc = "待结算";
                break;
            case 1:
                statusClass = "status-processing";
                statusDesc = "结算中";
                break;
            case 2:
                statusClass = "status-success";
                statusDesc = "已结算";
                break;
            case 3:
                statusClass = "status-fail";
                statusDesc = "结算失败";
                break;
            default:
                statusClass = "status-pending";
                statusDesc = "未知";
        }
        return "<span class=\"" + statusClass + "\">" + statusDesc + "</span>";
    }

    private String buildAccountInfo(SettlementRecord record) {
        StringBuilder sb = new StringBuilder();
        if (record.getAccountName() != null) {
            sb.append(record.getAccountName());
        }
        if (record.getBankAccount() != null) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(maskBankAccount(record.getBankAccount()));
        }
        if (record.getBankName() != null) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(record.getBankName());
        }
        return sb.length() > 0 ? sb.toString() : "-";
    }

    private String convertSettleStatus(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待结算";
            case 1: return "结算中";
            case 2: return "已结算";
            case 3: return "结算失败";
            default: return "未知";
        }
    }

    private String maskBankAccount(String account) {
        if (account == null || account.length() <= 8) {
            return account;
        }
        int len = account.length();
        String prefix = account.substring(0, 4);
        String suffix = account.substring(len - 4);
        StringBuilder mask = new StringBuilder();
        for (int i = 0; i < len - 8; i++) {
            mask.append("*");
        }
        return prefix + mask + suffix;
    }
}
