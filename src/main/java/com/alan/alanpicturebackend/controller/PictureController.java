package com.alan.alanpicturebackend.controller;

import com.alan.alanpicturebackend.annotation.AuthCheck;
import com.alan.alanpicturebackend.common.BaseResponse;
import com.alan.alanpicturebackend.common.ResultUtils;
import com.alan.alanpicturebackend.constant.UserConstant;
import com.alan.alanpicturebackend.model.dto.picture.PictureUploadRequest;
import com.alan.alanpicturebackend.model.entity.User;
import com.alan.alanpicturebackend.model.vo.PictureVO;
import com.alan.alanpicturebackend.service.PictureService;
import com.alan.alanpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author alan
 * @Description: 图片控制层
 * @Date: 2025/1/10 20:50
 */
@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * 上传图片（可重新上传）
     *
     * @param multipartFile        上传的文件
     * @param pictureUploadRequest 图片上传请求
     * @param httpServletRequest   请求
     * @return 返回上传的图片信息
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestParam("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }
}
