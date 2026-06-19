package com.payhub.common.interceptor;

import com.payhub.common.context.SandboxContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class SandboxInterceptor implements HandlerInterceptor {

    public static final String SANDBOX_HEADER = "X-Sandbox-Mode";
    public static final String SANDBOX_SCENE_HEADER = "X-Sandbox-Scene";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String sandboxMode = request.getHeader(SANDBOX_HEADER);
        String sandboxScene = request.getHeader(SANDBOX_SCENE_HEADER);

        if ("true".equalsIgnoreCase(sandboxMode) || "1".equals(sandboxMode)) {
            SandboxContext.setSandboxMode(true);
            if (sandboxScene != null && !sandboxScene.isEmpty()) {
                SandboxContext.setScene(sandboxScene);
            }
            log.debug("沙箱模式已启用, scene={}", sandboxScene);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SandboxContext.clear();
    }
}
