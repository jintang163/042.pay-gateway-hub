package com.payhub.channel.transfer;

public interface TransferService {

    TransferResult transfer(TransferRequest request);

    String getChannelCode();
}
