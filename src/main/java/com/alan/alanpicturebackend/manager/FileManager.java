package com.alan.alanpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.alan.alanpicturebackend.config.CosClientConfig;
import com.alan.alanpicturebackend.exception.BusinessException;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.exception.ThrowUtils;
import com.alan.alanpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author alan
 * @Description: 通用文件上传服务 （腾讯云cos）
 * @Date: 2025/1/9 17:14
 * @deprecated 已废弃，改为使用 upload 包的模板方法优化
 */
@Slf4j
@Service
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 返回图片上传信息
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);
        // 处理文件上传地址
        String uuid = RandomUtil.randomString(16);       // 使用HuTool工具类生成一个 16 位的随机数
        String originFilename = multipartFile.getOriginalFilename(); // 获取原始文件名
        String fileSuffix = FileUtil.getSuffix(originFilename);  // 文件后缀
        String uploadTime = DateUtil.formatDate(new Date());   // 上传时间
        String uploadFilename = String.format("%S_%S.%S", uploadTime, uuid, fileSuffix);
        // 上传路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);  // 上传临时文件
            multipartFile.transferTo(file);  // 将文件转移到临时文件
            // 上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            // 获取文件宽度、高度
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            // 计算文件宽度比
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败。", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        } finally {
            // 清理临时文件
            this.deleteTempFile(file);
        }

    }

    /**
     * 使用 URL 上传图片
     *
     * @param fileUrl          图片url地址
     * @param uploadPathPrefix 上传路径前缀
     * @return 返回图片上传信息
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        // 校验图片
//        validPicture(multipartFile);
        /* 1.修改为校验url */
        validPictureInUrl(fileUrl);
        // 处理文件上传地址
        String uuid = RandomUtil.randomString(16);       // 使用HuTool工具类生成一个 16 位的随机数
//        String originFilename = multipartFile.getOriginalFilename(); // 获取原始文件名
        /* 2.从url中获取文件名 */
        String originFilename = FileUtil.mainName(fileUrl); // 获取原始文件名
        String fileSuffix = FileUtil.getSuffix(originFilename);  // 文件后缀
        String uploadTime = DateUtil.formatDate(new Date());   // 上传时间
        String uploadFilename = String.format("%S_%S.%S", uploadTime, uuid, fileSuffix);
        // 上传路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);  // 上传临时文件
//            multipartFile.transferTo(file);  // 将文件转移到临时文件
            /* 使用 hutool 工具类将文件下载到临时文件 */
            HttpUtil.downloadFile(fileUrl, file);
            // 上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            // 获取文件宽度、高度
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            // 计算文件宽度比
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败。", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        } finally {
            // 清理临时文件
            this.deleteTempFile(file);
        }

    }

    /**
     * 校验文件(文件上传）
     *
     * @param multipartFile 文件
     */
    public void validPicture(MultipartFile multipartFile) {
        // 判断是否为空
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 判断文件大小
        long fileSize = multipartFile.getSize(); // 获取文件大小
        final long ONE_M = 1024 * 1024L;  // 1M
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "上传文件大小不能超过 2 MB");

        // 判断文件后缀
        // 允许上传的文件类型
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        // 获取文件后缀 (使用 Hutool 工具包）
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "上传文件类型错误");
    }

    /**
     * 校验文件（URL 上传）
     *
     * @param fileUrl URL地址
     */
    public void validPictureInUrl(String fileUrl) {
        ThrowUtils.throwIf(fileUrl == null, ErrorCode.PARAMS_ERROR, "文件地址（URL）不能为空");
        // 校验URL格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }
        //校验 URL 协议
        if (!fileUrl.startsWith("http://") || !fileUrl.startsWith("https://")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");
        }

        // 发送 HEAD 请求验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 4. 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 5. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }

}
