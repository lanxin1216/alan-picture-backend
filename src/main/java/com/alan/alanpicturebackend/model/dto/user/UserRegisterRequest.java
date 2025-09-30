package com.alan.alanpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = -7544939494934254062L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 验证码
     */
    private String verificationCode;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
