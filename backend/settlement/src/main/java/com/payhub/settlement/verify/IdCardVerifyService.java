package com.payhub.settlement.verify;

public interface IdCardVerifyService {

    IdCardVerifyResult verifySecondGen(String idCardName, String idCardNo, String verifyRequestId);

    IdCardVerifyResult verifyThirdGen(String idCardName, String idCardNo, String verifyRequestId);

    IdCardVerifyResult verifyWithLiveness(String idCardName, String idCardNo, String faceImageBase64, String verifyRequestId);
}
