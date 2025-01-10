package com.alan.alanpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author alan
 * @Description: 图片上传请求参数
 * @Date: 2025/1/9 17:08
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
