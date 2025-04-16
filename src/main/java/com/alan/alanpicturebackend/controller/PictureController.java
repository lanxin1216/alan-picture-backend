package com.alan.alanpicturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.alan.alanpicturebackend.annotation.AuthCheck;
import com.alan.alanpicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.alan.alanpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.alan.alanpicturebackend.common.BaseResponse;
import com.alan.alanpicturebackend.common.DeleteRequest;
import com.alan.alanpicturebackend.common.ResultUtils;
import com.alan.alanpicturebackend.constant.UserConstant;
import com.alan.alanpicturebackend.exception.BusinessException;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.exception.ThrowUtils;
import com.alan.alanpicturebackend.model.dto.picture.*;
import com.alan.alanpicturebackend.model.dto.space.SpaceLevel;
import com.alan.alanpicturebackend.model.entity.Picture;
import com.alan.alanpicturebackend.model.entity.User;
import com.alan.alanpicturebackend.model.enums.PictureReviewStatusEnum;
import com.alan.alanpicturebackend.model.enums.SpaceLevelEnum;
import com.alan.alanpicturebackend.model.vo.PictureTagCategory;
import com.alan.alanpicturebackend.model.vo.PictureVO;
import com.alan.alanpicturebackend.service.PictureService;
import com.alan.alanpicturebackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestParam("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取用户
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        pictureService.deletePicture(id, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员）
     *
     * @param pictureUpdateRequest 更新图片请求
     * @return 返回结果
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将参数DTO转换为实体类
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 将list 转换为string（主要是标签tag）
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断图片是否存在
        Long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库修改数据
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "更新图片错误");
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员）
     *
     * @param id      图片 id
     * @param request 请求
     * @return 返回图片详细信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 返回
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     *
     * @param id      图片 id
     * @param request 请求
     * @return 返回图片详细信息
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(loginUser, picture);
        }
        // 判断是否过审了
        if (!picture.getReviewStatus().equals(PictureReviewStatusEnum.PASS.getValue())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 封装数据
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        // 返回
        return ResultUtils.success(pictureVO);
    }

    /**
     * 获取图片分页列表（仅管理员）
     *
     * @param pictureQueryRequest 分页请求
     * @return 返回详细分页列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询
        Page<Picture> objectPage = new Page<>(current, size);
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(objectPage, queryWrapper);
        return ResultUtils.success(picturePage);
    }

    /**
     * 获取图片分页列表（封装类）
     *
     * @param pictureQueryRequest 分页请求
     * @param request             请求
     * @return 返回详细分页 VO 列表
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        Page<PictureVO> listPictureVOByPage = pictureService.getListPictureVOByPage(pictureQueryRequest, request);
        return ResultUtils.success(listPictureVOByPage);
    }

    /**
     * 刷新图片分页 VO列表redis缓存（仅管理员）
     *
     * @return 返回清除结果
     */
    @PostMapping("/refresh/pageVoCache")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateListPictureVOCache() {
        pictureService.updateListPictureVOCache();
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取标签和分类
     * - 先使用数组写死
     *
     * @return 返回分类列表和标签列表
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 审核请求
     * @param request              请求
     * @return 返回审核结果
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量抓取网络图片，（bing搜索引擎的图片）
     *
     * @param pictureUploadByBatchRequest 抓取请求
     * @param request                     请求
     * @return 返回抓取保存的图片数量
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Integer count = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(count);
    }

    /**
     * 获取空间级别信息
     *
     * @return 返回空间级别列表
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(SpaceLevelEnum -> new SpaceLevel(
                        SpaceLevelEnum.getValue(),
                        SpaceLevelEnum.getText(),
                        SpaceLevelEnum.getMaxCount(),
                        SpaceLevelEnum.getMaxSize()
                ))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getUrl());
        return ResultUtils.success(resultList);
    }

}
