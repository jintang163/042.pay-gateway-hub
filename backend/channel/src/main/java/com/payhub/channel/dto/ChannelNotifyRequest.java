package com.payhub.channel.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChannelNotifyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String channelCode;

    private String notifyData;

    private String signature;
}
