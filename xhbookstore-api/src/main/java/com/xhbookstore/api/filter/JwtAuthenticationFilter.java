package com.xhbookstore.api.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import com.alibaba.fastjson2.JSON;
import com.xhbookstore.api.constant.ApiErrorCode;
import com.xhbookstore.api.model.ApiResponse;
import com.xhbookstore.common.utils.StringUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

/**
 * JWT身份验证过滤器
 * 对所有 /api/mp/v1/** 路径进行Token校验
 * 白名单路径无需Token
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final String tokenSecret;

    public JwtAuthenticationFilter(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    // 白名单 - 无需Token的路径
    private static final List<String> WHITE_LIST = Arrays.asList(
        "/api/mp/v1/auth/wechat-phone-login",
        "/api/mp/v1/auth/refresh-token",
        "/api/mp/v1/auth/session",
        "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 仅处理 /api/mp/v1/** 路径
        if (!uri.startsWith("/api/mp/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取Token（白名单也尝试解析，但不强制要求）
        String token = extractToken(request);
        boolean isWhiteListed = isWhiteList(uri);

        if (StringUtils.isEmpty(token)) {
            if (isWhiteListed) {
                filterChain.doFilter(request, response);
                return;
            }
            writeUnauthorized(response, ApiErrorCode.UNAUTHORIZED, "缺少访问令牌");
            return;
        }

        // 验证Token
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(tokenSecret)
                    .parseClaimsJws(token)
                    .getBody();
            // 将用户信息存入request，供Controller使用
            request.setAttribute("userId", claims.getSubject());
            request.setAttribute("isMember", claims.get("isMember"));
            request.setAttribute("isStaff", claims.get("isStaff"));
            request.setAttribute("memberId", claims.get("memberId"));
            request.setAttribute("staffUserId", claims.get("staffUserId"));
            request.setAttribute("phone", claims.get("phone"));
        } catch (ExpiredJwtException e) {
            if (isWhiteListed) {
                log.debug("[Token过期-白名单] uri={}", uri);
                filterChain.doFilter(request, response);
                return;
            }
            log.warn("[Token过期] uri={}", uri);
            writeUnauthorized(response, ApiErrorCode.AUTH_TOKEN_EXPIRED, "登录已过期，请重新登录");
            return;
        } catch (Exception e) {
            if (isWhiteListed) {
                log.debug("[Token无效-白名单] uri={}", uri);
                filterChain.doFilter(request, response);
                return;
            }
            log.warn("[Token无效] uri={}, error={}", uri, e.getMessage());
            writeUnauthorized(response, ApiErrorCode.AUTH_TOKEN_INVALID, "无效的访问令牌");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhiteList(String uri) {
        return WHITE_LIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return request.getParameter("token");
    }

    private void writeUnauthorized(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<?> resp = ApiResponse.error(code, message);
        response.getWriter().write(JSON.toJSONString(resp));
    }
}
