package com.alan.alanpicturebackend.manager.email.store;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存验证码存储
 */
@Component
public class InMemoryVerificationCodeStore {

    // 存储邮箱验证码的map
    private final Map<String,codeInfo> emailCodeMap = new ConcurrentHashMap<>();

    public void save(String email, String code){
        // 设置五分钟过期
        emailCodeMap.put(email,new codeInfo(code, LocalDateTime.now().plusMinutes(5)));
    }

    /**
     * 验证验证码
     * @param email
     * @param code
     * @return
     */
    public boolean validate(String email, String code){
        codeInfo codeInfo = emailCodeMap.get(email);
        if(codeInfo == null){
            return false;
        }

        boolean isValid = false;
        if(code.equals(codeInfo.getCode()) && LocalDateTime.now().isBefore(codeInfo.getExpireTime())){
            isValid = true;
        }
        if(isValid){
            emailCodeMap.remove(email);
            return true;
        }
        return false;
    }

    /**
     * 判断邮箱是否可发送验证码
     * @param email
     * @return
     */
    public boolean canSend(String email){
        codeInfo codeInfo = emailCodeMap.get(email);
        if(codeInfo != null && LocalDateTime.now().isBefore(codeInfo.getExpireTime())){
            return false;
        }
        return true;
    }

    @Data
    public static class codeInfo {
        // 验证码
        private String code;

        // 过期时间
        private LocalDateTime expireTime;

        // 上一次发送时间
        private LocalDateTime lastSendTime;

        public codeInfo(String code, LocalDateTime expireTime){
            this.code = code;
            this.expireTime = expireTime;
            this.lastSendTime = LocalDateTime.now();
        }
    }
}