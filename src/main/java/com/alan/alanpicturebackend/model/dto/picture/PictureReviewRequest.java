package com.alan.alanpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author alan
 * @Description: 图片审核请求类
 * @Date: 2025/1/17 20:42
 */
@Data
public class PictureReviewRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 状态：0-待审核, 1-通过, 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;


    private static final long serialVersionUID = 1L;
}
