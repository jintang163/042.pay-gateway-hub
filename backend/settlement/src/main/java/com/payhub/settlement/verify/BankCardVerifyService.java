package com.payhub.settlement.verify;

public interface BankCardVerifyService {

    BankCardVerifyResult verifyFourElements(String idCardName, String idCardNo, String bankCardNo, String bankPhone, String verifyRequestId);
}
