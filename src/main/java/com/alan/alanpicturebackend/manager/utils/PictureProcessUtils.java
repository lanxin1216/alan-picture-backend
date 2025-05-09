package com.alan.alanpicturebackend.manager.utils;

import com.alan.alanpicturebackend.exception.BusinessException;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.luciad.imageio.webp.WebPWriteParam;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
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
     * - 如果原图宽高小于设定值，不修改
     * - （Thumbnails的缩略好像默认是不会修改的，为了清晰一点，自己又添加一个判断逻辑）
     *
     * @param originalImage 原图
     */
    public void toThumbnailImage(File originalImage, File thumbnailFile) {

        try {
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

        } catch (IOException e) {
            log.error("生成缩略图失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成缩略图是失败");
        }
    }

    /**
     * 转换为压缩图（预览图）
     * - webp 格式
     *
     * @param originalImage 原图
     * @param previewImage  生成的预览图
     */
    public void toPreviewImage(File originalImage, File previewImage) {
        ImageWriter writer = null;
        FileImageOutputStream outputStream = null;
        try {
            // 获取原始文件的编码
            BufferedImage image = ImageIO.read(originalImage);
            // 创建WebP ImageWriter实例
            writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
            // 配置编码参数
            WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
            writeParam.setCompressionMode(WebPWriteParam.MODE_DEFAULT);
            // 配置ImageWriter输出
            outputStream = new FileImageOutputStream(previewImage);
            writer.setOutput(outputStream);
            // 进行编码，重新生成新图片
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } catch (IOException e) {
            log.error("生成压缩图（预览图）失败，原图路径：{}，预览图路径：{}", originalImage.getAbsolutePath(), previewImage.getAbsolutePath(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成压缩图（预览图）失败");
        } finally {
            // 确保资源被正确关闭
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.error("关闭 FileImageOutputStream 时发生错误", e);
                }
            }
            if (writer != null) {
                writer.dispose();
            }
        }
    }
}
