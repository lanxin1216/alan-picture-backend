package com.alan.alanpicturebackend.service.impl;

import com.alan.alanpicturebackend.exception.BusinessException;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.manager.email.EmailServiceManage;
import com.alan.alanpicturebackend.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author alanK
 * @Description:
 * @Date: 2025/9/29 18:15
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Resource
    private EmailServiceManage emailServiceManage;

    @Override
    public void sendEmailVerificationCode(String email) {
        if (!validateEmail(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        // 发送邮箱验证码
        emailServiceManage.sendEmailVerificationCode(email);
    }

    @Override
    public boolean verifyEmailCode(String email, String code) {
        if (!validateEmail(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        return emailServiceManage.validateEmailVerificationCode(email, code);
    }

    /**
     * 验证邮箱格式
     */
    @Override
    public boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        email = email.trim();
        // 更严格的邮箱格式验证正则表达式
        String emailRegex = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                            + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}
