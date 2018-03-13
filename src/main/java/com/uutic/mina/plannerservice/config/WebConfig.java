package com.uutic.mina.plannerservice.config;

import com.uutic.mina.plannerservice.exceptions.InvalidTokenException;
import com.uutic.mina.plannerservice.exceptions.TokenNotFoundException;
import com.uutic.mina.plannerservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {
    private Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("**").allowedOrigins("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptorAdapter() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String corsMethod = request.getHeader("access-control-request-method");
                String corsHeaders = request.getHeader("access-control-request-headers");
                if (corsMethod != null && corsHeaders != null) {
                    log.info("Preflight request:");
                    log.info("access-control-request-method: " + corsMethod);
                    log.info("access-control-request-headers: " + corsHeaders);
                    return super.preHandle(request, response, handler);
                }

                String authorization = request.getHeader("Authorization");
                if (authorization == null || authorization.isEmpty()) {
                    throw new TokenNotFoundException("未授权");
                }
                if (!authorization.startsWith("Bearer ")) {
                    throw new InvalidTokenException("无效的授权信息");
                }
                String token = authorization.substring(7);
                Map<String, String> claims = JwtUtil.decode(token);
                if (!claims.containsKey("plan_id") || !claims.containsKey("user_id"))
                    throw new InvalidTokenException("找不到必需的参数");
                request.setAttribute("plan_id", claims.get("plan_id"));
                request.setAttribute("user_id", claims.get("user_id"));

                return super.preHandle(request, response, handler);
            }
        }).addPathPatterns("/**").excludePathPatterns("/api/upload/newplan");
    }
}
