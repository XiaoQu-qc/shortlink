package com.nageoffer.shortlink.admin.common.biz.user;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.nageoffer.shortlink.admin.common.enums.UserErrorCodeEnum.USER_TOKEN_FAIL;

/**
 * 用户信息传输过滤器
 *
 */
@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;

    private static final List<String> IGNORE_URI = List.of(
            "/api/shortlink/admin/v1/user/login",
            "/api/shortlink/admin/v1/user/has-username"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        String requestURI = httpServletRequest.getRequestURI();
        if(!IGNORE_URI.contains(requestURI)) {
            String method = httpServletRequest.getMethod();
            if (!(Objects.equals(requestURI, "/api/shortlink/admin/v1/user") && Objects.equals(method, "POST"))) {
                String username = httpServletRequest.getHeader("username");
                String token = httpServletRequest.getHeader("token");
                if(!StrUtil.isAllNotBlank(username, token)){
                    throw new ClientException(USER_TOKEN_FAIL);
                }
                Object userInfoJsonStr;
                try {
                    userInfoJsonStr = stringRedisTemplate.opsForHash().get("login_" + username, token);
                    if(userInfoJsonStr == null) {
                        throw new ClientException(USER_TOKEN_FAIL);
                    }
                } catch (ClientException e) {
                    throw new ClientException(USER_TOKEN_FAIL);
                }
                UserInfoDTO userInfoDTO = JSON.parseObject(userInfoJsonStr.toString(), UserInfoDTO.class);
                UserContext.setUser(userInfoDTO);
            }
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }
}
