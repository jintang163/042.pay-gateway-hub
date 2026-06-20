package com.payhub.settlement.controller;

import cn.hutool.core.util.StrUtil;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
@RequestMapping("/merchant/withdraw")
public class MerchantWithdrawPageController {

    @GetMapping("")
    public String withdrawPage(HttpServletRequest request) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (StrUtil.isBlank(merchantNo)) {
            throw new BusinessException(ResultCode.NOT_LOGIN, "请先登录");
        }
        request.setAttribute("merchantNo", merchantNo);
        return "merchant-withdraw";
    }

    @GetMapping("/{merchantNo}")
    public String withdrawPageByMerchant(@PathVariable String merchantNo, HttpServletRequest request) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (StrUtil.isBlank(currentMerchantNo) || !currentMerchantNo.equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限访问");
            }
        }
        request.setAttribute("merchantNo", merchantNo);
        return "merchant-withdraw";
    }
}
