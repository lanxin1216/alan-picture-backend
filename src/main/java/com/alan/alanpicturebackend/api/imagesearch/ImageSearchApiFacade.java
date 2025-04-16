package com.alan.alanpicturebackend.api.imagesearch;

import com.alan.alanpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.alan.alanpicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.alan.alanpicturebackend.api.imagesearch.sub.GetImageListApi;
import com.alan.alanpicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author alan
 * @Description: 图片搜索服务（门面模式）
 * @Date: 2025/4/16 17:51
 */
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://avatars.githubusercontent.com/u/131834711?v=4";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }

}
