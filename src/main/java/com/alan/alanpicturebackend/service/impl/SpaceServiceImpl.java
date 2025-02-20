package com.alan.alanpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.alan.alanpicturebackend.exception.BusinessException;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.exception.ThrowUtils;
import com.alan.alanpicturebackend.mapper.SpaceMapper;
import com.alan.alanpicturebackend.model.dto.space.SpaceAddRequest;
import com.alan.alanpicturebackend.model.dto.space.SpaceQueryRequest;
import com.alan.alanpicturebackend.model.entity.Space;
import com.alan.alanpicturebackend.model.entity.User;
import com.alan.alanpicturebackend.model.enums.SpaceLevelEnum;
import com.alan.alanpicturebackend.model.vo.SpaceVO;
import com.alan.alanpicturebackend.model.vo.UserVO;
import com.alan.alanpicturebackend.service.SpaceService;
import com.alan.alanpicturebackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 86187
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-02-20 16:54:22
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;

    // 添加编程式事务
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 填充参数默认值
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        // 默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 填充数据
        this.fillSpaceBySpaceLevel(space);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 校验参数
        this.validSpace(space, true);
        // 权限校验
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        // 控制同一用户只能创建一个私有空间
        // 使用本地锁和事务
        // 针对用户加锁
        String lock = String.valueOf((userId)).intern(); // 将字符串对象放入常量池中
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 查询用户是否已经存在私有空间（exists 查询是否存在）
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户仅能有一个私有空间");
                // 写入数据库
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建空间失败");
                return space.getId();
            });
            // 返回结果是包装类，可以做一些处理
            return Optional.ofNullable(newSpaceId).orElse(-1L); // 如果为空则返回-1,理论上不会出现这种清空
        }
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        // 级别枚举
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 判断是否为创建
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevelEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }
        // 修改数据时，判断是否要修改级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 25) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能超过25个字符");
        }
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        // 如果查询请求为空，则直接返回空的对象
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 取出查询请求对象的所有值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 填充 QueryWrapper 对象
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        // 排序规则
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 将对象拷贝过去，使用VO类对象中的方法
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 获取用户信息，查询空间关联的用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId); // 数据库获取到用户信息
            // 将用户信息转VO对象
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        // 获取到分页的空间列表对象
        List<Space> spaceList = spacePage.getRecords();
        // 创建一个新的空间 VO 分页对象，初始化当前页面、页面大小、总页数等属性
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        // 使用huTool工具判断空间列表是否为空
        if (CollUtil.isEmpty(spaceList)) {
            // 如果空间列表为空，直接返回空的 VO 分页对象
            return spaceVOPage;
        }
        // 将空间对象列表转换为空间 VO 对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 从空间列表中提取用户 ID 集合，用于后续关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 根据用户 ID 集合查询用户信息，并将结果按用户 ID 分组存储到 Map 中
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 遍历空间 VO 对象列表
        spaceVOList.forEach(spaceVO -> {
            // 获取当前空间 VO 对象对应的用户 ID
            Long userId = spaceVO.getUserId();
            // 初始化用户对象为 null
            User user = null;

            // 如果 Map 中包含当前用户 ID
            if (userIdUserListMap.containsKey(userId)) {
                // 从 Map 中获取对应的用户列表，并取第一个用户对象
                user = userIdUserListMap.get(userId).get(0);
            }
            // 调用 userService 的 getUserVO 方法，将用户对象转换为用户 VO 对象，并填充到当前空间 VO 对象中
            spaceVO.setUser(userService.getUserVO(user));
        });
        // 将转换后的空间 VO 对象列表设置到 VO 分页对象中
        spaceVOPage.setRecords(spaceVOList);
        // 返回空间分页的 VO 对象列表
        return spaceVOPage;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 获取空间等级枚举对象
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            // 如果管理员没有设置空间大小
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public Page<SpaceVO> getListSpaceVOByPage(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 限制查询条数最大值，防止爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询 数据库
        Page<Space> objectPage = new Page<>(current, size);
        QueryWrapper<Space> queryWrapper = this.getQueryWrapper(spaceQueryRequest);
        Page<Space> spacePage = this.page(objectPage, queryWrapper);
        // 封装
        return this.getSpaceVOPage(spacePage, request);
    }
}




