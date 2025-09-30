package com.alan.alanpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author alan
 * @Description: 用户创建请求
 * @Date: 2025/1/7 17:19
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}

