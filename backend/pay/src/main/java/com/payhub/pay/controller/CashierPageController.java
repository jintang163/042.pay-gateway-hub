package com.payhub.pay.controller;

import com.payhub.common.context.CurrentUserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
@RequestMapping("/cashier")
public class CashierPageController {

    @GetMapping("")
    public String cashierPage(HttpServletRequest request) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        request.setAttribute("merchantNo", merchantNo);
        return "cashier";
    }
}
