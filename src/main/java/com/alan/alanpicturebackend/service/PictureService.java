package com.alan.alanpicturebackend.service;

import com.alan.alanpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.alan.alanpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.alan.alanpicturebackend.model.dto.picture.*;
import com.alan.alanpicturebackend.model.entity.Picture;
import com.alan.alanpicturebackend.model.entity.User;
import com.alan.alanpicturebackend.model.vo.PictureVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

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
     * @param inputSource          图片文件和url地址
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser            上传用户
     * @return 返回上传的图片信息
     */
    PictureVO uploadPicture(Object inputSource,
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
     * 获取图片分页 VO 列表（封装类）
     *
     * @param request             请求
     * @param pictureQueryRequest 分页请求
     * @return 返回详细分页 VO 列表
     */
    Page<PictureVO> getListPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 管理员刷新图片分页 VO列表redis缓存
     */
    void updateListPictureVOCache();

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

    /**
     * 补充审核参数-管理员自动过审
     *
     * @param picture   图片信息
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 图片抓取
     *
     * @param pictureUploadByBatchRequest 抓取请求参数
     * @param loginUser                   登录用户
     * @return 返回抓取条数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 删除图片
     *
     * @param pictureId 图片id
     * @param loginUser 登录用户
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片
     *
     * @param pictureEditRequest 编辑请求
     * @param loginUser          登录用户
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 图片批量编辑
     * @param pictureEditByBatchRequest 编辑请求
     * @param loginUser 登录用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 校验图片权限
     *
     * @param longinUser 登录用户
     * @param picture    图片
     */
    void checkPictureAuth(User longinUser, Picture picture);

    /**
     * ai扩图任务请求
     *
     * @param createPictureOutPaintingTaskRequest 扩图任务请求参数
     * @param loginUser                           请求用户
     * @return 返回任务状态
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

    /**
     * 查询ai 扩图任务情况
     *
     * @param taskId 任务 id
     * @return 返回任务状态
     */
    GetOutPaintingTaskResponse getCreatePictureOutPaintingTaskRequest(String taskId);
}
