package com.alan.alanpicturebackend.manager.utils;

import com.alan.alanpicturebackend.exception.BusinessException;
import com.alan.alanpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author alan
 * @Description: 图片处理工具类
 * @Date: 2025/1/20 20:14
 */
@Component
@Slf4j
public class PictureProcessUtils {

    /**
     * 转换缩略图
     * - 格式：256 * 256
     *
     * @param originalImage 原图
     * @return 缩略图
     */
    public File toThumbnailImage(File originalImage) {
        File thumbnailFile = null; // 接收缩略图

        try {
            // 生成临时文件
            thumbnailFile = File.createTempFile("/thumbnail", null);

            // 获取原图的宽高
            BufferedImage image = ImageIO.read(originalImage);
            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();

            // 设置目标尺寸
            int targetWidth = 256;
            int targetHeight = 256;

            // 如果目标尺寸大于原图的尺寸，不进行缩放
            if (targetWidth > originalWidth || targetHeight > originalHeight) {
                targetWidth = originalWidth;
                targetHeight = originalHeight;
            }
            // 生成缩略图
            Thumbnails.of(originalImage)
                    .size(targetWidth, targetHeight)
                    .toFile(thumbnailFile);

            return thumbnailFile;
        } catch (IOException e) {
            log.error("生成缩略图失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成缩略图是失败");
        } finally {
            // 6. 清理临时文件
            deleteTempFile(thumbnailFile);
        }
    }

    /**
     * 转换为压缩图（预览图）
     * - webp 格式
     *
     * @param originalImage 原图
     * @return 压缩图
     */
    public File toPreviewImage(File originalImage) {
        File previewFile = null; // 预览图

        try {
            // 生成临时文件
            previewFile = File.createTempFile("/preview", null);


            // 预览图（webp 格式）
//            Thumbnails.of(originalImage)
//                    .outputFormat("webp") // 转换为 WebP 格式
//                    .toFile(previewFile);

            // 使用java 的 ImageIO
            BufferedImage image = ImageIO.read(originalImage);

            ImageIO.write(image, "webp", previewFile);

            return previewFile;
        } catch (IOException e) {
            log.error("生成压缩图（预览图）失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成压缩图（预览图）失败");
        } finally {
            // 6. 清理临时文件
            deleteTempFile(previewFile);
        }
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
