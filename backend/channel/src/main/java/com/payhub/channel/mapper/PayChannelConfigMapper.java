package com.payhub.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payhub.channel.entity.PayChannelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PayChannelConfigMapper extends BaseMapper<PayChannelConfig> {

    @Select("SELECT * FROM pay_channel_config WHERE status = 1 ORDER BY id ASC")
    List<PayChannelConfig> selectAllEnabled();

    @Select("SELECT * FROM pay_channel_config WHERE channel_code = #{channelCode} AND status = 1 LIMIT 1")
    PayChannelConfig selectByChannelCode(@Param("channelCode") String channelCode);
}
