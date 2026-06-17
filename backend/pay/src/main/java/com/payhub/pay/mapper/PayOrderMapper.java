package com.payhub.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payhub.pay.entity.PayOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayOrderMapper extends BaseMapper<PayOrder> {
}
