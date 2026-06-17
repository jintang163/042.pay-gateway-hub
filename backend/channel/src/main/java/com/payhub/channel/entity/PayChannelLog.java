package com.payhub.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("pay_channel_log")
public class PayChannelLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantNo;

    private String orderNo;

    private String channelCode;

    private String requestType;

    private String requestUrl;

    private String requestData;

    private String responseData;

    private String channelTradeNo;

    private Integer costTime;

    private String errorMsg;

    private LocalDateTime createTime;
}
