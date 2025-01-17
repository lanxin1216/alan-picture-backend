package com.alan.alanpicturebackend.service;

import com.alan.alanpicturebackend.model.dto.picture.PictureQueryRequest;
import com.alan.alanpicturebackend.model.dto.picture.PictureReviewRequest;
import com.alan.alanpicturebackend.model.dto.picture.PictureUploadRequest;
import com.alan.alanpicturebackend.model.entity.Picture;
import com.alan.alanpicturebackend.model.entity.User;
import com.alan.alanpicturebackend.model.vo.PictureVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author alan
 * @Description: 针对表【picture(图片)】的数据库操作Service
 * @Date: 2025-01-09 17:01:31
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile        图片文件
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser            上传用户
     * @return 返回上传的图片信息
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 将查询请求对象转换为QueryWrapper对象
     *
     * @param pictureQueryRequest 查询请求
     * @return 返回QueryWrapper
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);


    /**
     * 将单个图片对象封装为 VO 对象
     *
     * @param picture 图片对象
     * @param request 请求
     * @return 图片VO类
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 将图片分页对象列表转换为 VO 列表对象
     *
     * @param picturePage 分页图片列表
     * @param request     请求
     * @return 图片分页的 VO 对象列表
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片数据校验，用于更新修改图片数据时
     *
     * @param picture 图片数据
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 审核请求
     * @param loginUser            登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

}
