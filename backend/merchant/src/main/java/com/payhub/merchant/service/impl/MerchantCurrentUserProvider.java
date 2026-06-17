package com.payhub.merchant.service.impl;

import com.payhub.common.context.CurrentUserProvider;
import com.payhub.merchant.entity.MerchantUser;
import com.payhub.merchant.service.MerchantUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantCurrentUserProvider implements CurrentUserProvider {

    private final MerchantUserService merchantUserService;

    @Override
    public Object getUserByUsername(String username) {
        MerchantUser user = merchantUserService.getByUsername(username);
        if (user == null) {
            log.warn("用户不存在, username: {}", username);
            return null;
        }
        return user;
    }
}
