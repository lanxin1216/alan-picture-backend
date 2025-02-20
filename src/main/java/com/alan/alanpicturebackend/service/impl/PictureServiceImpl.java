package com.alan.alanpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alan.alanpicturebackend.exception.BusinessException;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.exception.ThrowUtils;
import com.alan.alanpicturebackend.manager.upload.FilePictureUpload;
import com.alan.alanpicturebackend.manager.upload.PictureUploadTemplate;
import com.alan.alanpicturebackend.manager.upload.UrlPictureUpload;
import com.alan.alanpicturebackend.mapper.PictureMapper;
import com.alan.alanpicturebackend.model.dto.file.UploadPictureResult;
import com.alan.alanpicturebackend.model.dto.picture.*;
import com.alan.alanpicturebackend.model.entity.Picture;
import com.alan.alanpicturebackend.model.entity.Space;
import com.alan.alanpicturebackend.model.entity.User;
import com.alan.alanpicturebackend.model.enums.PictureReviewStatusEnum;
import com.alan.alanpicturebackend.model.vo.PictureVO;
import com.alan.alanpicturebackend.model.vo.UserVO;
import com.alan.alanpicturebackend.service.PictureService;
import com.alan.alanpicturebackend.service.SpaceService;
import com.alan.alanpicturebackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author alan
 * @Description: 针对表【picture(图片)】的数据库操作Service实现
 * @Date: 2025-01-09 17:01:31
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 图片上传
     *
     * @param inputSource          图片文件或Url地址
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser            上传用户
     * @return 上传的图片信息
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 上传用户检验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验用户是否有权限上传图片到该空间(仅空间管理员可上传)
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
            // 校验空间额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "空间图片数量额度不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "空间存储大小额度不足");
            }
        }

        // 判断是更新还是添加
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是存在id，则是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
//            boolean exists = this.lambdaQuery()
//                    .eq(Picture::getId, pictureId)
//                    .exists();
//            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }

            // 校验空间是否一致
            // 如果没传，复用原来的 SpaceId （兼容公共图库）
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 SpaceId，必须和原图的一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "图片空间 id 不一致");
                }
            }
        }
        // 上传图片，得到信息
        // 按照用户 id 划分目录 => 安装空间划分
//        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        String uploadPathPrefix;
        if (spaceId == null) {
            // 公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            // 空间图库
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 需要根据 inputSource 类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        // 判断 inputSource 的类型
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要写入数据库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId); // 指定空间 id
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setPreviewUrl(uploadPictureResult.getPreviewUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());

        // 封装姓名
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 补充审核参数
        fillReviewParams(picture, loginUser);
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            // 更新空间操作的额度
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, finalSpaceId)
                    .setSql("totalSize = totalSize + " + picture.getPicSize())
                    .setSql("totalCount = totalCount + 1")
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
            return picture;
        });
        // todo 如果是更新图片，需要清空之前的老旧图片资源
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
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
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
        // 审核信息
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();

        // 填充 QueryWrapper 对象
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        Long reviewerId = pictureQueryRequest.getReviewerId();
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);

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
     * 获取图片分页 VO 列表（封装类）
     *
     * @param request             请求
     * @param pictureQueryRequest 分页请求
     * @return 返回详细分页 VO 列表
     */
    @Override
    public Page<PictureVO> getListPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制查询条数最大值，防止爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            // 如果 spaceId 为空，则默认查询公共空间的数据
            // 普通用户默认只能查看已过审的数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            // 私有空间
            // 普通用户只能查看自己创建的私有空间的数据
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
        }

        // 构建缓存 Key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest); // 查询条件
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String redisKey = "alanPicture:listPictureVoByPage:" + hashKey; // 缓存键

        // 查询 Redis 缓存
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        String cachedValue = valueOps.get(redisKey);
        if (cachedValue != null) {
            // Redis 缓存中查询到
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return cachedPage;
        }

        // 查询 数据库
        Page<Picture> objectPage = new Page<>(current, size);
        QueryWrapper<Picture> queryWrapper = this.getQueryWrapper(pictureQueryRequest);
        Page<Picture> picturePage = this.page(objectPage, queryWrapper);
        // 封装
        Page<PictureVO> pictureVOPage = this.getPictureVOPage(picturePage, request);

        // 添加 Redis 缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 设置过期时间：5~10分钟随机 防止雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300); // 300秒 + 0到300秒随机
        valueOps.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        return pictureVOPage;
    }

    @Override
    public void updateListPictureVOCache() {
        // 清除图片列表缓存
        // 使用通配符匹配所有图片列表缓存
        Set<String> redisKeys = stringRedisTemplate.keys("alanPicture:listPictureVoByPage:" + "*");
        if (redisKeys != null) {
            stringRedisTemplate.delete(redisKeys);
        }
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

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 审核请求
     * @param loginUser            登录用户
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR);

        // 校验审核状态是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

        // 操作数据库
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片审核异常");
    }

    /**
     * 补充审核参数-管理员自动过审
     *
     * @param picture   图片信息
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要修改为审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 图片抓取
     *
     * @param pictureUploadByBatchRequest 抓取请求参数
     * @param loginUser                   登录用户
     * @return 返回抓取条数
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验查询关键词
        String searchText = pictureUploadByBatchRequest.getSearchText();
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "查询关键词不能为空");

        // 校验查询数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "单次抓取数量最大限度为30条");

        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        // 如果名称不存在默认使用搜索关键词
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }

        // 添加抓取地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        // 请求抓取地址
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败：", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        /* 得到页面，获取页面的图片元素 */
        Element div = document.getElementsByClass("dgControl").first(); // 获取到页面的外层元素
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面元素失败");
        }
//        Elements imgElementList = div.select("img.mimg"); // 获取到内层元素的图片
        Elements imgElementList = div.select(".iusc"); // 获取到内层元素的图片

        /* 校验并且保存图片 */
        int uploadCount = 0; // 记录上传的图片
        for (Element imgElement : imgElementList) {
            // 获取到标签内的图片地址
//            String fileUrl = imgElement.attr("src");

            /* 修改获取原始高清图片*/
            String dataM = imgElement.attr("m");
            String fileUrl;
            try {
                // 解析JSON字符串
                JSONObject jsonObject = JSONUtil.parseObj(dataM);
                // 获取 murl 字段（原始图片URL）
                fileUrl = jsonObject.getStr("murl");
            } catch (Exception e) {
                log.error("解析图片数据失败", e);
                continue;
            }

            // 判断是否存在，不存在则跳过
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前连接为空, 已经跳过：{}", fileUrl);
                continue;
            }
            // 处理上传的图片地址，去除多余参数，防止上传时转义
            int questionMarkIndex = fileUrl.indexOf("?"); // 获取 ？ 所在的索引
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex); // 获取到第一个到 ？ 索引处
            }
            /* 上传图片 */
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            // 设置图片名称
            if (StrUtil.isNotBlank(namePrefix)) {
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                // 上传图片
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功：id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            /* 如果上传的图片达到了需要的数量，停止循环 */
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);
        // 判断图片是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        this.checkPictureAuth(loginUser, oldPicture);

        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "图片删除异常");
            // 更新空间操作的额度
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, oldPicture.getSpaceId())
                    .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                    .setSql("totalCount = totalCount - 1")
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
            return true;
        });
        // todo 异步清理文件待实现
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        this.checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void checkPictureAuth(User longinUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long longinUserId = longinUser.getId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(longinUserId) && !userService.isAdmin(longinUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有图库，仅本人可操作
            if (!picture.getUserId().equals(longinUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }
}




