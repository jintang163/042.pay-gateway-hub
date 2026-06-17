package com.payhub.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payhub.merchant.entity.MerchantInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface MerchantInfoMapper extends BaseMapper<MerchantInfo> {

    IPage<MerchantInfo> selectPageList(Page<MerchantInfo> page, @Param("params") Map<String, Object> params);
}
