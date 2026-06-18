package com.payhub.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payhub.merchant.entity.PaymentPageConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface PaymentPageConfigMapper extends BaseMapper<PaymentPageConfig> {

    IPage<PaymentPageConfig> selectPageList(Page<PaymentPageConfig> page, @Param("params") Map<String, Object> params);
}
