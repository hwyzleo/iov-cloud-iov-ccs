package net.hwyz.iov.cloud.iov.ccs.service.application.cucc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * CUCC安全服务实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class CuccSecurityServiceImpl implements CuccSecurityService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long TIME_WINDOW_MS = TimeUnit.MINUTES.toMillis(5); // 5分钟时间窗

    @Value("${cucc.security.appid:}")
    private String appid;

    @Value("${cucc.security.app-secret:}")
    private String appSecret;

    @Value("${cucc.security.time-window-minutes:5}")
    private int timeWindowMinutes;

    // nonce缓存，用于防重放
    private final ConcurrentHashMap<String, Long> nonceCache = new ConcurrentHashMap<>();

    @Override
    public boolean verifySignature(String appid, long timestamp, String nonce, String signature, String body) {
        try {
            // 生成期望的签名
            String expectedSignature = generateSignature(appid, timestamp, nonce, body);

            // 使用常量时间比较，防止时序攻击
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("签名验证失败", e);
            return false;
        }
    }

    @Override
    public boolean isReplayAttack(long timestamp, String nonce) {
        // 检查时间窗
        long now = System.currentTimeMillis();
        long timeDiff = Math.abs(now - timestamp);
        if (timeDiff > TimeUnit.MINUTES.toMillis(timeWindowMinutes)) {
            log.warn("请求时间戳超出允许范围: timestamp={}, now={}, diff={}ms", timestamp, now, timeDiff);
            return true;
        }

        // 检查nonce是否重复
        String nonceKey = nonce + ":" + timestamp;
        Long existingTimestamp = nonceCache.putIfAbsent(nonceKey, now);

        if (existingTimestamp != null) {
            log.warn("检测到重放攻击: nonce={}, timestamp={}", nonce, timestamp);
            return true;
        }

        // 清理过期的nonce缓存
        cleanExpiredNonces();

        return false;
    }

    @Override
    public String generateSignature(String appid, long timestamp, String nonce, String body) {
        try {
            // 拼接待签名字符串
            String message = appid + timestamp + nonce + body;

            // 使用HMAC-SHA256签名
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);

            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }

    /**
     * 清理过期的nonce缓存
     */
    private void cleanExpiredNonces() {
        long now = System.currentTimeMillis();
        long expireTime = TimeUnit.MINUTES.toMillis(timeWindowMinutes);

        nonceCache.entrySet().removeIf(entry ->
                now - entry.getValue() > expireTime
        );
    }
}
