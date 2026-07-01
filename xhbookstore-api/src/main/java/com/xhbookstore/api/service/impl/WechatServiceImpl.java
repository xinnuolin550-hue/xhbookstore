package com.xhbookstore.api.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xhbookstore.api.config.WechatConfig;
import com.xhbookstore.api.service.IWechatService;
import com.xhbookstore.common.core.redis.RedisCache;

/**
 * 微信服务实现（分布式安全版）
 *
 * 多容器部署安全机制：
 * 1. 获取token先查Redis缓存，命中直接返回
 * 2. 缓存未命中时，用 SET NX 抢分布式锁（锁超时10秒防死锁）
 * 3. 抢到锁的实例调微信API，写入Redis后释放锁
 * 4. 未抢到锁的实例自旋等待（最多3秒），然后从Redis读取
 * 5. 定时刷新也用锁保护，避免多实例同时调微信API
 */
@Service
public class WechatServiceImpl implements IWechatService {

    private static final Logger log = LoggerFactory.getLogger(WechatServiceImpl.class);

    @Autowired private WechatConfig wechatConfig;
    @Autowired private RedisCache redisCache;
    @Autowired private RedisTemplate<Object, Object> redisTemplate;

    private static final String WX_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/stable_token";
    private static final String WX_PHONE_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=";
    private static final String LOCK_KEY_SUFFIX = ":lock";
    private static final long LOCK_TIMEOUT_SEC = 10;      // 锁超时10秒
    private static final long WAIT_MAX_MS = 3000;          // 未抢到锁最多等3秒
    private static final long WAIT_INTERVAL_MS = 200;      // 每次等200ms重试

    @Override
    public String getAccessToken() {
        String cacheKey = wechatConfig.getTokenCacheKey();

        // 查Redis缓存，存在直接返回（stable_token不互相失效，缓存即有效）
        String cached = redisCache.getCacheObject(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            log.debug("[微信Token] 命中Redis缓存");
            return cached;
        }

        // 缓存不存在，加锁避免并发重复请求（但stable_token不互斥，锁仅用于减少API调用）
        String lockKey = cacheKey + LOCK_KEY_SUFFIX;
        String lockValue = UUID.randomUUID().toString();
        boolean locked = tryLock(lockKey, lockValue, LOCK_TIMEOUT_SEC);

        if (locked) {
            try {
                String doubleCheck = redisCache.getCacheObject(cacheKey);
                if (doubleCheck != null && !doubleCheck.isEmpty()) {
                    return doubleCheck;
                }

                log.info("[微信Token] 调用stable_token接口...");
                String token = fetchAccessTokenFromWechat();
                if (token == null) {
                    log.error("[微信Token] 获取失败，请检查微信AppID/AppSecret配置");
                    return null;
                }

                long cacheSeconds = wechatConfig.getTokenExpireSeconds() - wechatConfig.getTokenRefreshAhead();
                redisCache.setCacheObject(cacheKey, token, (int) cacheSeconds, TimeUnit.SECONDS);
                log.info("[微信Token] 缓存成功，TTL={}秒", cacheSeconds);
                return token;
            } finally {
                unlock(lockKey, lockValue);
            }
        } else {
            // 未抢到锁，短暂等待后从缓存读取
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < WAIT_MAX_MS) {
                try { Thread.sleep(WAIT_INTERVAL_MS); } catch (InterruptedException e) { break; }
                String waited = redisCache.getCacheObject(cacheKey);
                if (waited != null && !waited.isEmpty()) {
                    return waited;
                }
            }
            log.warn("[微信Token] 等待超时，直接获取...");
            return fetchAccessTokenFromWechat();
        }
    }

    /**
     * 定时刷新，先看TTL再决定是否刷新
     * - TTL > 30分钟：说明最近刚被刷新过（可能是其他实例），跳过
     * - TTL <= 30分钟：快要过期了，抢锁刷新
     * - 缓存不存在：抢锁刷新
     */
    @Scheduled(fixedRate = 90 * 60 * 1000)
    public void refreshAccessToken() {
        String cacheKey = wechatConfig.getTokenCacheKey();
        String lockKey = cacheKey + LOCK_KEY_SUFFIX;
        String lockValue = UUID.randomUUID().toString();

        // 1. 先查TTL，判断是否需要刷新
        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        if (ttl != null && ttl > 1800) { // 剩余 > 30分钟，刚被刷新过，跳过
            log.debug("[微信Token] TTL剩余{}秒(>30min)，跳过刷新", ttl);
            return;
        }

        // 2. TTL不足或缓存不存在，尝试获取锁
        if (!tryLock(lockKey, lockValue, LOCK_TIMEOUT_SEC)) {
            log.debug("[微信Token] TTL={}，需要刷新但未获得锁，跳过", ttl);
            return;
        }

        try {
            // 3. Double-check：抢到锁后再查一次TTL
            Long ttl2 = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
            if (ttl2 != null && ttl2 > 1800) {
                log.info("[微信Token] Double-check TTL={}，已被刷新，跳过", ttl2);
                return;
            }

            log.info("[微信Token] TTL={}，开始刷新...", ttl2);
            String token = fetchAccessTokenFromWechat();
            if (token != null) {
                long cacheSeconds = wechatConfig.getTokenExpireSeconds() - wechatConfig.getTokenRefreshAhead();
                redisCache.setCacheObject(cacheKey, token, (int) cacheSeconds, TimeUnit.SECONDS);
                log.info("[微信Token] 刷新成功，新TTL={}秒", cacheSeconds);
            }
        } catch (Exception e) {
            log.error("[微信Token] 刷新失败", e);
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 使用 Redis SET NX 实现分布式锁
     */
    private boolean tryLock(String key, String value, long expireSeconds) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁（仅当value匹配时才删除，防止误删其他实例的锁）
     */
    private void unlock(String key, String value) {
        try {
            String current = (String) redisTemplate.opsForValue().get(key);
            if (value.equals(current)) {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("[微信Token] 释放锁异常", e);
        }
    }

    private String fetchAccessTokenFromWechat() {
        try {
            String body = "{\"grant_type\":\"client_credential\","
                    + "\"appid\":\"" + wechatConfig.getAppId() + "\","
                    + "\"secret\":\"" + wechatConfig.getAppSecret() + "\"}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WX_TOKEN_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = JSON.parseObject(response.body());

            if (json.containsKey("access_token")) {
                log.info("[微信Token] stable_token获取成功, expires_in={}", json.getInteger("expires_in"));
                return json.getString("access_token");
            } else {
                log.error("[微信Token] 微信返回错误: {}", response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("[微信Token] 网络异常", e);
            return null;
        }
    }

    @Override
    public String getPhoneNumber(String code) {
        String accessToken = getAccessToken();

        try {
            String url = WX_PHONE_URL + accessToken;
            String body = "{\"code\":\"" + code + "\"}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = JSON.parseObject(response.body());

            if (json.getIntValue("errcode") == 0) {
                JSONObject phoneInfo = json.getJSONObject("phone_info");
                String phone = phoneInfo.getString("purePhoneNumber");
                log.info("[微信手机号] 获取成功: {}", phone.substring(0, 3) + "****" + phone.substring(7));
                return phone;
            } else {
                log.error("[微信手机号] 失败: errcode={} errmsg={}",
                        json.getIntValue("errcode"), json.getString("errmsg"));
                return null;
            }
        } catch (Exception e) {
            log.error("[微信手机号] 网络异常", e);
            return null;
        }
    }
}
