package com.payhub.channel.transfer;

public interface TransferService {

    TransferResult transfer(TransferRequest request);

    TransferResult query(String transferNo, String channelTransferNo);

    String getChannelCode();
}
