package com.alan.alanpicturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.alan.alanpicturebackend.config.CosClientConfig;
import com.alan.alanpicturebackend.exception.BusinessException;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.manager.CosManager;
import com.alan.alanpicturebackend.manager.utils.PictureProcessUtils;
import com.alan.alanpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

/**
 * @author alan
 * @Description: 图片上传模板 抽象类
 * @Date: 2025/1/18 14:43
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;

    @Resource
    private PictureProcessUtils pictureProcessUtils;

    /**
     * 模板方法，定义上传流程
     *
     * @param inputSource      上传文件 或者 文件URL地址
     * @param uploadPathPrefix 上传路径前缀
     * @return 返回图片上传信息
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        /* 校验图片(文件或URL地址) */
        validPicture(inputSource);

        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        /* 获取文件的原始文件名 */
        String originFilename = getOriginFilename(inputSource);
        String originFileSuffix = FileUtil.getSuffix(originFilename);
        String uploadTime = DateUtil.formatDate(new Date());   // 上传时间

        // 优化百炼ai扩图功能，去掉？参数
        if (originFileSuffix.contains("?")) {
            originFileSuffix = originFileSuffix.split("\\?")[0];
        }

        // 原始图片上传图片名称
        String uploadFilename = String.format("%s_%s.%s", uploadTime, uuid, originFileSuffix);
        // 缩略图名称
        String thumbnailImageName = String.format("%s_%s_thumbnail.%s", uploadTime, uuid, originFileSuffix);
        // 压缩图
        String previewImageName = String.format("%s_%s_preview.webp", uploadTime, uuid);

        // 上传路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        String thumbnailImagePath = String.format("/%s/%s", uploadPathPrefix, thumbnailImageName);
        String previewImagePath = String.format("/%s/%s", uploadPathPrefix, previewImageName);

        File originalFile = null;
        File thumbnailImage = null; // 缩略图
        File previewImage = null;  // 压缩图（预览图）
        try {
            // 3. 创建临时文件
            originalFile = File.createTempFile("originalImage", "." + originFileSuffix);
            thumbnailImage = File.createTempFile("thumbnailImage", "." + originFileSuffix);
            previewImage = File.createTempFile("previewImage", ".webp");

            /* 处理文件来源（本地或 URL） */
            processFile(inputSource, originalFile);

            /* 处理图片 */
            // 1). 生成缩略图
            pictureProcessUtils.toThumbnailImage(originalFile, thumbnailImage);

            // 2). 生成压缩图（webp）
            pictureProcessUtils.toPreviewImage(originalFile, previewImage);

            // 4. 上传图片到对象存储
            // 1) 原图
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, originalFile);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 2）缩略图
            cosManager.putObject(thumbnailImagePath, thumbnailImage);
            // 3）压缩图（webp）
            cosManager.putObject(previewImagePath, previewImage);

            // 5. 封装返回结果
            return buildResult(originFilename, originalFile, uploadPath, imageInfo, thumbnailImagePath, previewImagePath);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 清理临时文件
            deleteTempFile(originalFile, thumbnailImage, previewImage);
        }
    }

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 封装返回结果
     */
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath,
                                            ImageInfo imageInfo, String thumbnailImagePath, String previewImagePath) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailImagePath);
        uploadPictureResult.setPreviewUrl(cosClientConfig.getHost() + "/" + previewImagePath);
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File originalFile, File thumbnailImage, File previewImage) {
        if (originalFile == null && thumbnailImage == null && previewImage == null) {
            return;
        }
        boolean originalFileDelete = originalFile.delete();
        boolean thumbnailImageDelete = thumbnailImage.delete();
        boolean previewImageDelete = previewImage.delete();
        if (!originalFileDelete || !thumbnailImageDelete || !previewImageDelete) {
            log.error("file delete error, originalFilePath = {}", originalFile.getAbsolutePath());
        }
    }
}