package com.alan.alanpicturebackend.model.dto.picture;

import lombok.Data;

/**
 * @author alan
 * @Description: 图片抓取请求
 * @Date: 2025/1/18 21:08
 */
@Data
public class PictureUploadByBatchRequest {

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix;
}
