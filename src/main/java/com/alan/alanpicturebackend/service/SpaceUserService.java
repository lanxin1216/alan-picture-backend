package com.alan.alanpicturebackend.service;

import com.alan.alanpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.alan.alanpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.alan.alanpicturebackend.model.entity.SpaceUser;
import com.alan.alanpicturebackend.model.vo.SpaceUserVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 86187
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-05-18 18:08:30
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加空间用户
     *
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员对象
     *
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取查询条件
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取空间用户VO
     *
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 查到封装类列表
     *
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
