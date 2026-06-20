package com.payhub.settlement.service;

import com.payhub.channel.transfer.TransferResult;

import java.math.BigDecimal;

public interface UnifiedTransferService {

    TransferContext buildContextForSplitDetail(Long splitDetailId);

    TransferContext buildContextForAgentWithdraw(Long withdrawId);

    TransferContext buildContextForSettlementRecord(Long settlementId);

    TransferContext buildContextForMerchantWithdraw(Long withdrawId);

    TransferResult executeTransfer(TransferContext context);

    TransferResult queryTransferStatus(String transferNo, String channelTransferNo, String channel);

    public static class TransferContext {
        private Long sourceId;
        private String sourceType;
        private String transferNo;
        private String channel;
        private String receiverAccount;
        private String receiverName;
        private BigDecimal amountFen;
        private String bankName;
        private String bankCode;
        private String bankBranchName;
        private Integer receiverType;
        private String idCardNo;
        private String idCardName;
        private String bankPhone;
        private String merchantNo;
        private String remark;
        private String sourceNo;
        private Boolean skipIdCardVerify;

        public Long getSourceId() { return sourceId; }
        public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public String getTransferNo() { return transferNo; }
        public void setTransferNo(String transferNo) { this.transferNo = transferNo; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public String getReceiverAccount() { return receiverAccount; }
        public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }
        public String getReceiverName() { return receiverName; }
        public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
        public BigDecimal getAmountFen() { return amountFen; }
        public void setAmountFen(BigDecimal amountFen) { this.amountFen = amountFen; }
        public String getBankName() { return bankName; }
        public void setBankName(String bankName) { this.bankName = bankName; }
        public String getBankCode() { return bankCode; }
        public void setBankCode(String bankCode) { this.bankCode = bankCode; }
        public String getBankBranchName() { return bankBranchName; }
        public void setBankBranchName(String bankBranchName) { this.bankBranchName = bankBranchName; }
        public Integer getReceiverType() { return receiverType; }
        public void setReceiverType(Integer receiverType) { this.receiverType = receiverType; }
        public String getIdCardNo() { return idCardNo; }
        public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
        public String getIdCardName() { return idCardName; }
        public void setIdCardName(String idCardName) { this.idCardName = idCardName; }
        public String getBankPhone() { return bankPhone; }
        public void setBankPhone(String bankPhone) { this.bankPhone = bankPhone; }
        public String getMerchantNo() { return merchantNo; }
        public void setMerchantNo(String merchantNo) { this.merchantNo = merchantNo; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public String getSourceNo() { return sourceNo; }
        public void setSourceNo(String sourceNo) { this.sourceNo = sourceNo; }
        public Boolean getSkipIdCardVerify() { return skipIdCardVerify; }
        public void setSkipIdCardVerify(Boolean skipIdCardVerify) { this.skipIdCardVerify = skipIdCardVerify; }
    }
}
