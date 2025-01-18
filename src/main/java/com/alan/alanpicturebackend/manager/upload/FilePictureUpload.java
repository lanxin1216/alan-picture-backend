package com.alan.alanpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author alan
 * @Description: 本地图片上传子类
 * @Date: 2025/1/18 15:13
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        // 判断是否为空
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 判断文件大小
        long fileSize = multipartFile.getSize(); // 获取文件大小
        final long TWO_MB = 2 * 1024 * 1024L;  // 2M
        ThrowUtils.throwIf(fileSize > TWO_MB, ErrorCode.PARAMS_ERROR, "上传文件大小不能超过 2 MB");

        // 判断文件后缀
        // 允许上传的文件类型
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        // 获取文件后缀 (使用 Hutool 工具包）
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "上传文件类型错误");
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
