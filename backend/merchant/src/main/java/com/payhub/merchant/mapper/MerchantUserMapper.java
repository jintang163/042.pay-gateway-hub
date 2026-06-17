package com.payhub.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payhub.merchant.entity.MerchantUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantUserMapper extends BaseMapper<MerchantUser> {
}
