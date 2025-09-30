package com.alan.alanpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author alan
 * @Description: 用户登录请求
 * @Date: 2024/12/20 21:26
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -1804165094616407378L;


    /**
     * 用户登录邮箱
     */
    private String email;

    /**
     * 用户密码
     */
    private String userPassword;
}
