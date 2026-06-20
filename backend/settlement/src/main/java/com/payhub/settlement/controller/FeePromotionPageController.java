package com.payhub.settlement.controller;

import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/admin/fee-promotion")
public class FeePromotionPageController {

    @GetMapping("")
    public String promotionListPage() {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可访问");
        }
        return "fee-promotion";
    }
}
