package com.payhub.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("pay_channel_config")
public class PayChannelConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String channelCode;

    private String channelName;

    private String channelMerchantId;

    private String channelAppId;

    private String channelSecretKey;

    private String channelPublicKey;

    private String channelPrivateKey;

    private String notifyUrl;

    private Integer sandboxMode;

    private String gatewayUrl;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
