package com.alan.alanpicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * @author alan
 * @Description: 图片分类标签列表视图
 * @Date: 2025/1/16 14:01
 */
@Data
public class PictureTagCategory {

    private List<String> tagList;

    private List<String> categoryList;
}
