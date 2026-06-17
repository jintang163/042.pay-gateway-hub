package com.payhub.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.merchant.dto.MerchantLoginRequest;
import com.payhub.merchant.dto.MerchantRegisterRequest;
import com.payhub.merchant.dto.MerchantUserVO;
import com.payhub.merchant.entity.MerchantUser;

public interface MerchantUserService extends IService<MerchantUser> {

    void register(MerchantRegisterRequest request);

    MerchantUserVO login(MerchantLoginRequest request);

    MerchantUser getByUsername(String username);
}
