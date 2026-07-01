package com.xhbookstore.api.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.xhbookstore.api.config.SecurityProperties;
import com.xhbookstore.api.constant.ApiErrorCode;
import com.xhbookstore.api.exception.ApiException;
import com.xhbookstore.api.model.ApiResponse;
import com.xhbookstore.api.service.IWechatService;
import com.xhbookstore.common.utils.StringUtils;
import com.xhbookstore.common.core.domain.entity.SysUser;
import com.xhbookstore.system.domain.member.Member;
import com.xhbookstore.system.domain.member.MemberExt;
import com.xhbookstore.system.mapper.SysUserMapper;
import com.xhbookstore.system.mapper.member.MemberMapper;
import com.xhbookstore.system.service.member.IMemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * 认证接口
 * 安全机制：
 * - accessToken 2小时 + refreshToken 30天，双Token机制
 * - 登录接口按IP限流（默认5次/分钟）
 * - JWT密钥从配置文件读取
 */
@RestController
@RequestMapping("/api/mp/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired private IWechatService wechatService;
    @Autowired private MemberMapper memberMapper;
    @Autowired private SysUserMapper sysUserMapper;
    @Autowired private IMemberService memberService;
    @Autowired private SecurityProperties securityProperties;

    /** 微信手机号登录 — 查询member和sys_user两张表，返回双Token */
    @PostMapping("/wechat-phone-login")
    public ApiResponse<Map<String, Object>> wechatPhoneLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (StringUtils.isEmpty(code)) {
            throw new ApiException(ApiErrorCode.PARAM_INVALID, "缺少微信授权码");
        }

        // 获取手机号（微信API未配置时使用Mock）
        String phone = wechatService.getPhoneNumber(code);
        if (phone == null || phone.isEmpty()) {
            throw new ApiException(ApiErrorCode.AUTH_CODE_INVALID);
        }

        // 分别查询会员表和员工表
        Member member = memberMapper.selectMemberByPhone(phone);  // SQL 已过滤 status=0
        SysUser staff = sysUserMapper.selectUserByPhonenumber(phone); // SQL 已过滤 status='0' del_flag='0'

        // 二次校验：会员状态必须为正常(0)，排除注销(1)和挂失(2)
        if (member != null && member.getStatus() != null && member.getStatus() != 0) {
            log.warn("[登录拒绝] phone={}, memberId={}, status={} (非正常状态)", maskPhone(phone), member.getId(), member.getStatus());
            member = null;
        }

        // 新用户自动注册：手机号不在member表中，自动创建会员记录
        if (member == null && staff == null) {
            try {
                member = new Member();
                member.setPhone(phone);
                member.setName("");                           // 姓名后续补充
                member.setStatus(0);                          // 正常
                member.setSource("wechat");                   // 来源：微信小程序
                member.setCurrentPoints(0);
                member.setBorrowCountValid(0);
                member.setSyncErp(0);
                member.setLastOperator("system");
                // 生成卡号：微信用户用 "9" + 时间戳后10位 生成11位唯一卡号
                String cardNo = "9" + String.format("%010d", System.currentTimeMillis() % 10_000_000_000L);
                member.setCardNo(cardNo);
                // 直接写入 member 表（不用 insertMember service，避免调用 SecurityUtils 报错）
                memberMapper.insertMember(member);
                // 重新查出完整数据
                member = memberMapper.selectMemberByPhone(phone);
                log.info("[自动注册] phone={}, cardNo={}, memberId={}", maskPhone(phone), cardNo, member != null ? member.getId() : null);
            } catch (Exception e) {
                log.error("[自动注册失败] phone={}, error={}", maskPhone(phone), e.getMessage());
                member = null;  // 注册失败不阻塞登录
            }
        }

        boolean isMember = member != null;
        boolean isStaff = staff != null;

        // 生成唯一 userId：优先用 memberId，其次 staffId，否则 UUID
        String userId;
        if (member != null) {
            userId = "M" + member.getId();
        } else if (staff != null) {
            userId = "S" + staff.getUserId();
        } else {
            userId = UUID.randomUUID().toString();
        }

        String secret = securityProperties.getJwt().getSecret();
        long accessExpire = securityProperties.getJwt().getAccessTokenExpire();
        long refreshExpire = securityProperties.getJwt().getRefreshTokenExpire();

        // 生成 accessToken（短期，2小时）
        String accessToken = Jwts.builder()
                .setSubject(userId)
                .claim("isMember", isMember)
                .claim("isStaff", isStaff)
                .claim("phone", phone)
                .claim("memberId", member != null ? member.getId() : null)
                .claim("staffUserId", staff != null ? staff.getUserId() : null)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpire * 1000))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();

        // 生成 refreshToken（长期，30天）
        String refreshToken = Jwts.builder()
                .setSubject(userId)
                .claim("isMember", isMember)
                .claim("isStaff", isStaff)
                .claim("phone", phone)
                .claim("memberId", member != null ? member.getId() : null)
                .claim("staffUserId", staff != null ? staff.getUserId() : null)
                .claim("type", "refresh")
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpire * 1000))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();

        log.info("[登录] phone={}, isMember={}, isStaff={}", maskPhone(phone), isMember, isStaff);

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("expiresIn", accessExpire);
        data.put("isStaff", isStaff);
        data.put("staffUserId", staff != null ? staff.getUserId() : null);
        data.put("isMember", isMember);
        data.put("userId", userId);
        data.put("memberId", member != null ? member.getId() : null);
        return ApiResponse.success(data);
    }

    /** 刷新Token — 用refreshToken换新accessToken（携带完整的身份信息） */
    @PostMapping("/refresh-token")
    public ApiResponse<Map<String, Object>> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (StringUtils.isEmpty(refreshToken)) {
            throw new ApiException(ApiErrorCode.PARAM_INVALID, "缺少refreshToken");
        }

        String secret = securityProperties.getJwt().getSecret();
        long accessExpire = securityProperties.getJwt().getAccessTokenExpire();

        try {
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(refreshToken).getBody();
            if (!"refresh".equals(claims.get("type"))) {
                throw new ApiException(ApiErrorCode.AUTH_TOKEN_INVALID, "无效的refreshToken");
            }

            String userId = claims.getSubject();
            String newAccessToken = Jwts.builder()
                    .setSubject(userId)
                    .claim("isMember", claims.get("isMember"))
                    .claim("isStaff", claims.get("isStaff"))
                    .claim("phone", claims.get("phone"))
                    .claim("memberId", claims.get("memberId"))
                    .claim("staffUserId", claims.get("staffUserId"))
                    .claim("type", "access")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + accessExpire * 1000))
                    .signWith(SignatureAlgorithm.HS256, secret)
                    .compact();

            log.info("[刷新Token] userId={}", userId);

            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", newAccessToken);
            data.put("expiresIn", accessExpire);
            return ApiResponse.success(data);
        } catch (ExpiredJwtException e) {
            throw new ApiException(ApiErrorCode.AUTH_TOKEN_EXPIRED, "refreshToken已过期，请重新登录");
        } catch (Exception e) {
            throw new ApiException(ApiErrorCode.AUTH_TOKEN_INVALID, "无效的refreshToken");
        }
    }

    /** 校验登录态 */
    @GetMapping("/session")
    public ApiResponse<Map<String, Object>> checkSession(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        Map<String, Object> data = new HashMap<>();
        data.put("valid", userId != null);
        if (userId != null) {
            data.put("isStaff", request.getAttribute("isStaff"));
            data.put("isMember", request.getAttribute("isMember"));
            data.put("staffUserId", request.getAttribute("staffUserId"));
            data.put("userId", userId);
        }
        return ApiResponse.success(data);
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout() {
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        return ApiResponse.success(data);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
