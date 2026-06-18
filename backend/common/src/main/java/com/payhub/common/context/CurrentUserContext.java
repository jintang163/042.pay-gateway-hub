package com.payhub.common.context;

import cn.hutool.core.util.StrUtil;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class CurrentUserContext {

    private static final String CURRENT_MERCHANT_NO = "currentMerchantNo";
    private static final String CURRENT_USER = "currentUser";

    public static String getCurrentMerchantNo() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未获取到请求上下文");
        }
        String merchantNo = (String) request.getAttribute(CURRENT_MERCHANT_NO);
        if (StrUtil.isBlank(merchantNo)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未获取到当前商户号");
        }
        return merchantNo;
    }

    public static String getCurrentMerchantNoSilently() {
        try {
            return getCurrentMerchantNo();
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getCurrentUser() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getAttribute(CURRENT_USER);
    }

    public static boolean isAdmin() {
        Object user = getCurrentUser();
        if (user == null) {
            return false;
        }
        try {
            java.lang.reflect.Method method = user.getClass().getMethod("getRole");
            Object role = method.invoke(user);
            if (role != null) {
                String roleStr = role.toString();
                return "admin".equals(roleStr) || "operator".equals(roleStr);
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
