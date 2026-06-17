package com.payhub.merchant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.JwtUtil;
import com.payhub.common.utils.SnowflakeIdUtil;
import com.payhub.merchant.dto.MerchantLoginRequest;
import com.payhub.merchant.dto.MerchantRegisterRequest;
import com.payhub.merchant.dto.MerchantUserVO;
import com.payhub.merchant.entity.MerchantUser;
import com.payhub.merchant.mapper.MerchantUserMapper;
import com.payhub.merchant.service.MerchantUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MerchantUserServiceImpl extends ServiceImpl<MerchantUserMapper, MerchantUser> implements MerchantUserService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(MerchantRegisterRequest request) {
        LambdaQueryWrapper<MerchantUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantUser::getUsername, request.getUsername());
        if (StrUtil.isNotBlank(request.getMerchantNo())) {
            wrapper.eq(MerchantUser::getMerchantNo, request.getMerchantNo());
        }
        Long count = this.count(wrapper);
        if (count > 0) {
            throw new BusinessException(ResultCode.MERCHANT_USERNAME_EXIST);
        }

        MerchantUser user = new MerchantUser();
        user.setMerchantNo(StrUtil.isNotBlank(request.getMerchantNo()) ? request.getMerchantNo() : "");
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole("admin");
        user.setStatus(1);
        this.save(user);

        log.info("商户用户注册成功: username={}, merchantNo={}", request.getUsername(), request.getMerchantNo());
    }

    @Override
    public MerchantUserVO login(MerchantLoginRequest request) {
        LambdaQueryWrapper<MerchantUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantUser::getUsername, request.getUsername())
                .last("LIMIT 1");
        MerchantUser user = this.getOne(wrapper);

        if (user == null || !BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.MERCHANT_LOGIN_ERROR);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.MERCHANT_DISABLED);
        }

        String token = JwtUtil.generateToken(user.getMerchantNo(), user.getUsername());

        MerchantUserVO vo = BeanUtil.copyProperties(user, MerchantUserVO.class);
        vo.setToken(token);
        vo.setRoleDesc(getRoleDesc(user.getRole()));
        vo.setStatusDesc(user.getStatus() == 1 ? "启用" : "禁用");

        log.info("商户用户登录成功: username={}, merchantNo={}", request.getUsername(), user.getMerchantNo());
        return vo;
    }

    @Override
    public MerchantUser getByUsername(String username) {
        LambdaQueryWrapper<MerchantUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantUser::getUsername, username)
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    private String getRoleDesc(String role) {
        if (role == null) {
            return "";
        }
        switch (role) {
            case "admin":
                return "管理员";
            case "operator":
                return "操作员";
            case "finance":
                return "财务";
            default:
                return role;
        }
    }
}
