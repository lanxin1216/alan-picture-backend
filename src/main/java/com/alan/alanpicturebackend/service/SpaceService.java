package com.alan.alanpicturebackend.service;

import com.alan.alanpicturebackend.model.dto.space.SpaceAddRequest;
import com.alan.alanpicturebackend.model.dto.space.SpaceQueryRequest;
import com.alan.alanpicturebackend.model.entity.Space;
import com.alan.alanpicturebackend.model.entity.User;
import com.alan.alanpicturebackend.model.vo.SpaceVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 86187
 * @Description: 针对表【space(空间)】的数据库操作Service
 * @Date: 2025-02-20 16:54:22
 */
public interface SpaceService extends IService<Space> {

    /**
     * 新增空间
     * @param spaceAddRequest 新增请求
     * @param loginUser 登录用户
     * @return 新增空间id
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间
     *
     * @param space 空间
     * @param add   是否为新增校验
     */
    void validSpace(Space space, boolean add);

    /**
     * 将查询请求对象转换为QueryWrapper对象
     *
     * @param spaceQueryRequest 查询请求
     * @return 返回QueryWrapper
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 将单个空间对象封装为 VO 对象
     *
     * @param space   空间对象
     * @param request 请求
     * @return 空间VO类
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 将空间分页对象列表转换为 VO 列表对象
     *
     * @param spacePage 分页空间列表
     * @param request   请求
     * @return 空间分页的 VO 对象列表
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 填充空间等级
     *
     * @param space 空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 根据分页请求获取空间列表
     */
    Page<SpaceVO> getListSpaceVOByPage(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request);

    /**
     *校验空间权限
     */
    void checkSpaceAuth(User loginUser, Space space);

}
