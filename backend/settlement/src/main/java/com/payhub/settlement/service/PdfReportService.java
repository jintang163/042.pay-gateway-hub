package com.payhub.settlement.service;

import com.payhub.settlement.entity.SettlementRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PdfReportService {

    List<Map<String, Object>> generateSettlementReportData(String merchantNo, LocalDate startDate, LocalDate endDate);

    byte[] generateSettlementPdf(String merchantNo, String merchantName, LocalDate startDate, LocalDate endDate,
                                  List<SettlementRecord> settlementRecords);
}
