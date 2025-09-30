package com.alan.alanpicturebackend.controller;

import com.alan.alanpicturebackend.common.BaseResponse;
import com.alan.alanpicturebackend.common.ResultUtils;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.exception.ThrowUtils;
import com.alan.alanpicturebackend.model.dto.email.EmailVerificationRequest;
import com.alan.alanpicturebackend.service.EmailService;
import com.alan.alanpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author alanK
 * @Description: 邮箱服务
 * @Date: 2025/9/29 18:22
 */
@RestController
@RequestMapping("/email")
public class EmailController {
    @Resource
    private EmailService emailService;

    /**
     * 发送邮箱验证码
     * @param emailVerificationRequest
     * @return
     */
    @PostMapping("/verification")
    public BaseResponse<Boolean> userEmailVerification(@RequestBody EmailVerificationRequest emailVerificationRequest) {
        ThrowUtils.throwIf(emailVerificationRequest == null, ErrorCode.PARAMS_ERROR);
        String email = emailVerificationRequest.getEmail();
        emailService.sendEmailVerificationCode(email);
        return ResultUtils.success(true);
    }
}
