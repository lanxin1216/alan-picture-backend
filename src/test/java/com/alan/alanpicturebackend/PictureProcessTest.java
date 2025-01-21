package com.alan.alanpicturebackend;

import com.luciad.imageio.webp.WebPWriteParam;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author alan
 * @Description: 图片文件处理测试类
 * @Date: 2025/1/21 17:14
 */
@SpringBootTest
@Slf4j
public class PictureProcessTest {

    /**
     * 生成缩略图
     */
    @Test
    public void toThumbnailImage() throws IOException {
        /*
         * size(width,height) 若图片横比256小，高比256小，不变
         */
        // jpg 格式
        Thumbnails.of("D:/Projects/zAlan-alan-picture/alan-picture-backend/static/test.jpg")
                .size(256, 256)
                .toFile("D:/Projects/zAlan-alan-picture/alan-picture-backend/static/toThumbnailImage.jpg");

        // png 格式
        Thumbnails.of("D:/Projects/zAlan-alan-picture/alan-picture-backend/static/test.png")
                .size(256, 256)
                .toFile("D:/Projects/zAlan-alan-picture/alan-picture-backend/static/toThumbnailImage_png.png");

        Thumbnails.of("D:/Projects/zAlan-alan-picture/alan-picture-backend/static/test.webp")
                .size(256, 256)
                .toFile("D:/Projects/zAlan-alan-picture/alan-picture-backend/static/toThumbnailImage_webp.webp");

        log.info("图片压缩缩略图成功");
    }

    /**
     * 生成压缩图（webp）预览图
     */
    @Test
    public void toPreviewImage() {

        // 旧文件地址
//        String oldFile = "D:/Projects/zAlan-alan-picture/alan-picture-backend/static/test.jpg";
//        String newFile = "D:/Projects/zAlan-alan-picture/alan-picture-backend/static/toPreviewImage_jpg.webp";

//        String oldFile = "D:/Projects/zAlan-alan-picture/alan-picture-backend/static/test.png";
//        String newFile = "D:/Projects/zAlan-alan-picture/alan-picture-backend/static/toPreviewImage_png.webp";

        String oldFile = "D:/Projects/zAlan-alan-picture/alan-picture-backend/static/test.webp";
        String newFile = "D:/Projects/zAlan-alan-picture/alan-picture-backend/static/toPreviewImage_webp.webp";

        try {
            // 获取原始文件的编码
            BufferedImage image = ImageIO.read(new File(oldFile));
            // 创建WebP ImageWriter实例
            ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
            // 配置编码参数
            WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
            // 设置压缩模式
            writeParam.setCompressionMode(WebPWriteParam.MODE_DEFAULT);
            // 配置ImageWriter输出
            writer.setOutput(new FileImageOutputStream(new File(newFile)));
            // 进行编码，重新生成新图片
            writer.write(null, new IIOImage(image, null, null), writeParam);
            log.info("图片转Webp成功");
        } catch (Exception e) {
            log.error("异常");
        }
    }
}
