package com.ocr.recognition.qps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaiduQpsLimiter {

    private static final String QPS_KEY = "ocr:baidu:qps:";

    private final StringRedisTemplate redisTemplate;

    @Value("${ocr.baidu.qps-limit:10}")
    private int qpsLimit;

    /**
     * 尝试获取令牌（简单滑动窗口限流）
     */
    public boolean tryAcquire(String apiType) {
        String key = QPS_KEY + apiType + ":" + System.currentTimeMillis() / 1000;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(2));
        }
        return count != null && count <= qpsLimit;
    }

    /**
     * 等待直到获取令牌
     */
    public void acquire(String apiType) throws InterruptedException {
        int maxRetry = 30;
        int retry = 0;
        while (!tryAcquire(apiType) && retry < maxRetry) {
            Thread.sleep(100);
            retry++;
        }
        if (retry >= maxRetry) {
            log.warn("百度OCR QPS限流等待超时: {}", apiType);
        }
    }
}
