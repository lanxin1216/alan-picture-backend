package com.alan.alanpicturebackend.controller;

import com.alan.alanpicturebackend.annotation.AuthCheck;
import com.alan.alanpicturebackend.common.BaseResponse;
import com.alan.alanpicturebackend.common.ResultUtils;
import com.alan.alanpicturebackend.constant.UserConstant;
import com.alan.alanpicturebackend.exception.BusinessException;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author alan
 * @Description: 测试文件功能
 * @Date: 2025/1/9 16:07
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class TestFileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     *
     * @param multipartFile 文件
     * @return 返回文件地址
     */
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestParam("file") MultipartFile multipartFile) {
        // 文件目录
        String fileName = multipartFile.getOriginalFilename();  // 获取文件名
        String filepath = String.format("/test/%s", fileName);  // 拼接文件路径

        // 上传文件
        // 将文件转存到本地临时文件
        File file = null;
        try {
            // 创建本地临时文件
            file = File.createTempFile(filepath, null);
            // 将文件传入到临时文件
            multipartFile.transferTo(file);
            // 调用上传
            cosManager.putObject(filepath, file);
            return ResultUtils.success(filepath);  // 返回文件路径
        } catch (Exception e) {
            log.error("file upload error, filepath = {}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件资源
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @PostMapping("/test/Download/")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInputStream = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);  // 获取到文件对象
            cosObjectInputStream = cosObject.getObjectContent();  // 获取到文件的流

            // 处理下载的流，将流转换为字节数组
            byte[] byteArray = IOUtils.toByteArray(cosObjectInputStream);

            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);

            // 写入响应
            response.getOutputStream().write(byteArray); // 写入一个字节数组
            response.getOutputStream().flush();  // 刷新缓冲区
        } catch (IOException e) {
            log.error("file Download error, filepath = {}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            // 释放流资源
            if (cosObjectInputStream != null) {
                cosObjectInputStream.close();
            }
        }
    }

}
