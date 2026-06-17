package com.payhub.merchant.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.JwtUtil;
import com.payhub.merchant.dto.MerchantLoginRequest;
import com.payhub.merchant.dto.MerchantRegisterRequest;
import com.payhub.merchant.dto.MerchantUserVO;
import com.payhub.merchant.entity.MerchantUser;
import com.payhub.merchant.service.MerchantUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/merchant/user")
public class MerchantUserController {

    @Autowired
    private MerchantUserService merchantUserService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody MerchantRegisterRequest request) {
        merchantUserService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<MerchantUserVO> login(@Valid @RequestBody MerchantLoginRequest request) {
        MerchantUserVO userVO = merchantUserService.login(request);
        return Result.success(userVO);
    }

    @GetMapping("/info")
    public Result<MerchantUserVO> info(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = JwtUtil.getUsername(token);
        if (StrUtil.isBlank(username)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        MerchantUser user = merchantUserService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        MerchantUserVO userVO = BeanUtil.copyProperties(user, MerchantUserVO.class);
        userVO.setRoleDesc(getRoleDesc(user.getRole()));
        userVO.setStatusDesc(user.getStatus() == 1 ? "启用" : "禁用");
        return Result.success(userVO);
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
