package com.alan.alanpicturebackend.service.impl;

import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.exception.ThrowUtils;
import com.alan.alanpicturebackend.manager.FileManager;
import com.alan.alanpicturebackend.model.dto.file.UploadPictureResult;
import com.alan.alanpicturebackend.model.dto.picture.PictureUploadRequest;
import com.alan.alanpicturebackend.model.entity.User;
import com.alan.alanpicturebackend.model.vo.PictureVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alan.alanpicturebackend.model.entity.Picture;
import com.alan.alanpicturebackend.service.PictureService;
import com.alan.alanpicturebackend.mapper.PictureMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author alan
 * @Description: 针对表【picture(图片)】的数据库操作Service实现
 * @Date: 2025-01-09 17:01:31
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private FileManager fileManager;

    /**
     * 图片上传
     *
     * @param multipartFile        图片文件
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser            上传用户
     * @return 上传的图片信息
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 上传用户检验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 判断是更新还是添加
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是存在id，则是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 上传图片，得到信息
        // 按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        // 构造要写入数据库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);

    }
}




