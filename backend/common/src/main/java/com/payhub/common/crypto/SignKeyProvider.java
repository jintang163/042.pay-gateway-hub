package com.payhub.common.crypto;

public interface SignKeyProvider {

    String getSignKey(String merchantNo, String signType);

    String getPublicKey(String merchantNo, String signType);
}
