package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReconcileSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reconcileNo;

    private String reconcileDate;

    private String payChannel;

    private Integer totalCount;

    private Integer matchCount;

    private Integer mismatchCount;

    private LongFundSummary longFund;

    private ShortFundSummary shortFund;

    private AmountMismatchSummary amountMismatch;

    private StatusMismatchSummary statusMismatch;

    @Data
    public static class LongFundSummary {
        private Integer count;
        private java.math.BigDecimal totalAmount;
    }

    @Data
    public static class ShortFundSummary {
        private Integer count;
        private java.math.BigDecimal totalAmount;
    }

    @Data
    public static class AmountMismatchSummary {
        private Integer count;
        private java.math.BigDecimal totalDiffAmount;
    }

    @Data
    public static class StatusMismatchSummary {
        private Integer count;
    }
}
