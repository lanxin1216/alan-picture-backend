package com.alan.alanpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.exception.ThrowUtils;
import com.alan.alanpicturebackend.manager.FileManager;
import com.alan.alanpicturebackend.model.dto.file.UploadPictureResult;
import com.alan.alanpicturebackend.model.dto.picture.PictureQueryRequest;
import com.alan.alanpicturebackend.model.dto.picture.PictureUploadRequest;
import com.alan.alanpicturebackend.model.entity.User;
import com.alan.alanpicturebackend.model.vo.PictureVO;
import com.alan.alanpicturebackend.model.vo.UserVO;
import com.alan.alanpicturebackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alan.alanpicturebackend.model.entity.Picture;
import com.alan.alanpicturebackend.service.PictureService;
import com.alan.alanpicturebackend.mapper.PictureMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Resource
    private UserService userService;

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

    /**
     * 将查询请求转换为 QueryWrapper 对象
     *
     * @param pictureQueryRequest 查询请求
     * @return 返回QueryWrapper对象
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 如果查询请求为空，则直接返回空的对象
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 取出查询请求对象的所有值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        // 多字段搜索，拼接
        if (StrUtil.isNotBlank(searchText)) {
            // 拼接查询语句，等同于：【~ and (name like "%xxx%" or introduction like "%xxx%")】
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }

        // 填充 QueryWrapper 对象
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        // JSON 数组查询 查询是否包含 tag 列表中的标签
        /* 等同于：【~ and (tag like "%\"java\"%" and like "%\"javaAi\"%") */
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序规则
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 将单个图片对象封装为 VO 对象
     *
     * @param picture 图片对象
     * @param request 请求
     * @return 图片VO类
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 将对象拷贝过去，使用VO类对象中的方法
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 获取用户信息，查询图片关联的用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId); // 数据库获取到用户信息
            // 将用户信息转VO对象
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 将图片分页对象列表转换为 VO 列表对象
     *
     * @param picturePage 分页图片列表
     * @param request     请求
     * @return 图片分页的 VO 对象列表
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 获取到分页的图片列表对象
        List<Picture> pictureList = picturePage.getRecords();
        // 创建一个新的图片 VO 分页对象，初始化当前页面、页面大小、总页数等属性
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        // 使用huTool工具判断图片列表是否为空
        if (CollUtil.isEmpty(pictureList)) {
            // 如果图片列表为空，直接返回空的 VO 分页对象
            return pictureVOPage;
        }
        // 将图片对象列表转换为图片 VO 对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 从图片列表中提取用户 ID 集合，用于后续关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 根据用户 ID 集合查询用户信息，并将结果按用户 ID 分组存储到 Map 中
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));

        // 遍历图片 VO 对象列表
        pictureVOList.forEach(pictureVO -> {
            // 获取当前图片 VO 对象对应的用户 ID
            Long userId = pictureVO.getUserId();
            // 初始化用户对象为 null
            User user = null;

            // 如果 Map 中包含当前用户 ID
            if (userIdUserListMap.containsKey(userId)) {
                // 从 Map 中获取对应的用户列表，并取第一个用户对象
                user = userIdUserListMap.get(userId).get(0);
            }
            // 调用 userService 的 getUserVO 方法，将用户对象转换为用户 VO 对象，并填充到当前图片 VO 对象中
            pictureVO.setUser(userService.getUserVO(user));
        });
        // 将转换后的图片 VO 对象列表设置到 VO 分页对象中
        pictureVOPage.setRecords(pictureVOList);
        // 返回图片分页的 VO 对象列表
        return pictureVOPage;
    }

    /**
     * 图片数据校验，用于更新修改图片数据时
     *
     * @param picture 图片数据
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);

        // 取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 更新修改数据时，图片 id 不能为空
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "id 不能为空");
        // 如果存在url
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        // 如果存在 introduction
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }
}




