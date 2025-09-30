package com.alan.alanpicturebackend.manager.email.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author alanK
 * @Description: Redis 数据库存储验证码
 * @Date: 2025/9/29 15:57
 */
@Component
public class InRedisVerificationCodeStore {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis key 前缀
    private static final String VERIFICATION_CODE_PREFIX = "verification_code:";

    // 验证码过期时间（分钟）
    private static final long CODE_EXPIRE_MINUTES = 5;

    // 发送间隔时间（秒）
    private static final long SEND_INTERVAL_SECONDS = 60;

    // 发送间隔key前缀
    private static final String SEND_INTERVAL_PREFIX = "send_interval:";

    @Autowired
    public InRedisVerificationCodeStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存验证码
     * @param email 邮箱
     * @param code 验证码
     */
    public void save(String email, String code) {
        String key = getKey(email);
        String intervalKey = getIntervalKey(email);

        // 存储验证码，设置5分钟过期时间
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 设置发送间隔标记，60秒后自动过期
        redisTemplate.opsForValue().set(intervalKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 验证验证码
     * @param email 邮箱
     * @param code 验证码
     * @return 验证结果
     */
    public boolean validate(String email, String code) {
        String key = getKey(email);
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            return false;
        }

        boolean isValid = code.equals(storedCode);
        if (isValid) {
            // 验证成功后删除验证码和间隔标记
            redisTemplate.delete(key);
            redisTemplate.delete(getIntervalKey(email));
            return true;
        }
        return false;
    }

    /**
     * 判断邮箱是否频繁（60秒）
     * @param email 邮箱
     * @return 是否可以发送
     */
    public boolean isFrequent(String email) {
        String intervalKey = getIntervalKey(email);
        // 检查是否在发送间隔内
        Boolean hasInterval = redisTemplate.hasKey(intervalKey);
        return hasInterval != null && hasInterval;
    }

    /**
     * 删除验证码（手动清理）
     * @param email 邮箱
     */
    public void delete(String email) {
        String key = getKey(email);
        String intervalKey = getIntervalKey(email);
        redisTemplate.delete(key);
        redisTemplate.delete(intervalKey);
    }

    /**
     * 检查验证码是否存在且未过期
     * @param email 邮箱
     * @return 是否存在
     */
    public boolean exists(String email) {
        String key = getKey(email);
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    /**
     * 获取剩余过期时间
     * @param email 邮箱
     * @return 剩余时间（秒）
     */
    public Long getExpire(String email) {
        String key = getKey(email);
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 获取发送间隔剩余时间
     * @param email 邮箱
     * @return 剩余时间（秒）
     */
    public Long getSendIntervalExpire(String email) {
        String intervalKey = getIntervalKey(email);
        return redisTemplate.getExpire(intervalKey, TimeUnit.SECONDS);
    }

    /**
     * 生成验证码Redis key
     * @param email 邮箱
     * @return 完整的key
     */
    private String getKey(String email) {
        return VERIFICATION_CODE_PREFIX + email;
    }

    /**
     * 生成发送间隔Redis key
     * @param email 邮箱
     * @return 完整的key
     */
    private String getIntervalKey(String email) {
        return SEND_INTERVAL_PREFIX + email;
    }
}