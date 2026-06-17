package com.payhub.common.interceptor;

import com.payhub.common.context.CurrentUserProvider;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.JwtUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Autowired(required = false)
    private CurrentUserProvider currentUserProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);

        if (StringUtils.isBlank(authHeader) || !authHeader.startsWith(TOKEN_PREFIX)) {
            log.warn("缺少Authorization头或格式错误, uri: {}", request.getRequestURI());
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(TOKEN_PREFIX.length());

        Map<String, Object> payload = JwtUtil.parseToken(token);
        if (payload == null) {
            log.warn("Token无效或已过期, uri: {}", request.getRequestURI());
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String username = (String) payload.get("username");
        String merchantNo = (String) payload.get("merchantNo");

        if (StringUtils.isBlank(username) || StringUtils.isBlank(merchantNo)) {
            log.warn("Token中缺少用户信息, uri: {}", request.getRequestURI());
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        if (currentUserProvider != null) {
            Object user = currentUserProvider.getUserByUsername(username);
            if (user != null) {
                request.setAttribute("currentUser", user);
            }
        }

        request.setAttribute("currentMerchantNo", merchantNo);
        return true;
    }
}
