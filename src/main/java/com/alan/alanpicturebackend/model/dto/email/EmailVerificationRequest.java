package com.alan.alanpicturebackend.model.dto.email;

import lombok.Data;

import java.io.Serializable;

/**
 * @author alanK
 * @Description: 电子邮箱验证
 */
@Data
public class EmailVerificationRequest implements Serializable {
    private String email;

    private static final long serialVersionUID = 1L;
}

