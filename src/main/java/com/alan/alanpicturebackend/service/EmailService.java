package com.alan.alanpicturebackend.service;

/**
 * @author alanK
 * @Description: 邮件服务
 * @Date: 2025/9/29 18:14
 */
public interface EmailService {

    /**
     * 发送邮箱验证码
     */
    void sendEmailVerificationCode(String email);

    /**
     * 校验邮箱验证码
     */
    boolean verifyEmailCode(String email, String code);

    boolean validateEmail(String email);
}
