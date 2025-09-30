package com.alan.alanpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户修改密码请求
 */
@Data
public class UserUpdatePasswordRequest implements Serializable {

    private static final long serialVersionUID = -7544939494934254062L;

    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 验证码
     */
    private String verificationCode;

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认新密码
     */
    private String checkNewPassword;
}
