package com.alan.alanpicturebackend.service;

import com.alan.alanpicturebackend.model.dto.picture.PictureUploadRequest;
import com.alan.alanpicturebackend.model.entity.Picture;
import com.alan.alanpicturebackend.model.entity.User;
import com.alan.alanpicturebackend.model.vo.PictureVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author alan
 * @Description: 针对表【picture(图片)】的数据库操作Service
 * @Date: 2025-01-09 17:01:31
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile 图片文件
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser 上传用户
     * @return 返回上传的图片信息
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);


}
