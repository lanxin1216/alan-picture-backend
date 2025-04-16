package com.alan.alanpicturebackend.api.imagesearch.model;

import lombok.Data;

/**
 * @author alan
 * @Description: 以图搜图图片搜索结果
 * @Date: 2025/4/16 17:14
 */
@Data
public class ImageSearchResult {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}
