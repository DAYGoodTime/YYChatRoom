package com.yychat.common.util;

import com.yychat.common.model.Constant;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * 图片缩略图生成工具类
 * 支持多种图片格式，生成高质量缩略图
 * 放置在common包下，可被客户端和服务器端共享使用
 */
public class ThumbnailGenerator {

    // 缩略图默认尺寸
    public static final int THUMBNAIL_WIDTH = 150;
    public static final int THUMBNAIL_HEIGHT = 150;
    // 缩略图质量（0.1-1.0）
    private static final float THUMBNAIL_QUALITY = 0.7f;

    /**
     * 从字节数组生成缩略图（用于接收方）
     */
    public static ThumbnailResult generateThumbnailFromBytes(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return ThumbnailResult.error("图片数据为空");
        }

        try {
            // 从字节数组创建BufferedImage
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageData);
            BufferedImage originalImage = ImageIO.read(bais);
            if (originalImage == null) {
                return ThumbnailResult.error("无法解析图片数据");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 生成缩略图
            BufferedImage thumbnail = createThumbnail(originalImage);

            // 获取缩略图的实际尺寸
            int actualThumbnailWidth = thumbnail.getWidth();
            int actualThumbnailHeight = thumbnail.getHeight();

            // 转换为字节数组
            byte[] thumbnailData = imageToBytes(thumbnail, "webp");
            // 检查缩略图大小是否符合UDP传输要求（60KB）
            if (thumbnailData.length > 60 * 1024) {
                // 如果缩略图仍然太大，进一步压缩
                thumbnailData = compressThumbnail(thumbnail);
            }

            return new ThumbnailResult(true, thumbnailData, originalWidth, originalHeight,
                    actualThumbnailWidth, actualThumbnailHeight, imageData.length, "jpg");

        } catch (IOException e) {
            return ThumbnailResult.error("生成缩略图失败: " + e.getMessage());
        }
    }

    /**
     * 创建缩略图
     */
    private static BufferedImage createThumbnail(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 计算缩略图尺寸，保持宽高比
        double scale = Math.min((double) THUMBNAIL_WIDTH / originalWidth,
                (double) THUMBNAIL_HEIGHT / originalHeight);

        int thumbnailWidth = (int) (originalWidth * scale);
        int thumbnailHeight = (int) (originalHeight * scale);

        // 创建缩略图
        BufferedImage thumbnail = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);

        // 绘制缩略图（居中绘制，空白区域填充白色）
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

        // 计算居中位置
        int x = (THUMBNAIL_WIDTH - thumbnailWidth) / 2;
        int y = (THUMBNAIL_HEIGHT - thumbnailHeight) / 2;

        // 启用高质量缩放
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, x, y, thumbnailWidth, thumbnailHeight, null);
        g2d.dispose();

        return thumbnail;
    }

    /**
     * 进一步压缩缩略图
     */
    private static byte[] compressThumbnail(BufferedImage thumbnail) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 逐步降低质量直到满足大小要求
        float quality = THUMBNAIL_QUALITY;
        while (quality > 0.1f) {
            baos.reset();
            ImageWriter writer = ImageIO.getImageWritersByFormatName("webp").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            writer.setOutput(javax.imageio.ImageIO.createImageOutputStream(baos));
            writer.write(null, new javax.imageio.IIOImage(thumbnail, null, null), param);
            writer.dispose();

            if (baos.size() <= 60 * 1024) {
                break;
            }
            quality -= 0.1f;
        }

        return baos.toByteArray();
    }

    /**
     * 将BufferedImage转换为字节数组
     */
    private static byte[] imageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    /**
     * 将字节数组转换为ImageIcon
     */
    public static ImageIcon bytesToImageIcon(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return null;
        }

        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bais);
            if (image == null) {
                return null;
            }
            return new ImageIcon(image);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    /**
     * 检查是否支持的图片格式
     */
    private static boolean isSupportedFormat(String fileName) {
        return fileName.matches(Constant.IMAGE_REX);
    }

    /**
     * 缩略图生成结果类
     */
    public static class ThumbnailResult {
        private final boolean success;
        private final byte[] thumbnailData;
        private final int originalWidth;
        private final int originalHeight;
        private final int thumbnailWidth;
        private final int thumbnailHeight;
        private final long originalSize;
        private final String format;
        private final String errorMessage;

        private ThumbnailResult(boolean success, byte[] thumbnailData, int originalWidth,
                                int originalHeight, int thumbnailWidth, int thumbnailHeight,
                                long originalSize, String format) {
            this.success = success;
            this.thumbnailData = thumbnailData;
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
            this.thumbnailWidth = thumbnailWidth;
            this.thumbnailHeight = thumbnailHeight;
            this.originalSize = originalSize;
            this.format = format;
            this.errorMessage = null;
        }

        private ThumbnailResult(boolean success, String errorMessage) {
            this.success = success;
            this.thumbnailData = null;
            this.originalWidth = 0;
            this.originalHeight = 0;
            this.thumbnailWidth = 0;
            this.thumbnailHeight = 0;
            this.originalSize = 0;
            this.format = null;
            this.errorMessage = errorMessage;
        }

        public static ThumbnailResult error(String message) {
            return new ThumbnailResult(false, message);
        }

        // Getter方法
        public boolean isSuccess() {
            return success;
        }

        public byte[] getThumbnailData() {
            return thumbnailData;
        }

        public int getOriginalWidth() {
            return originalWidth;
        }

        public int getOriginalHeight() {
            return originalHeight;
        }

        public long getOriginalSize() {
            return originalSize;
        }

        public String getFormat() {
            return format;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public int getThumbnailSize() {
            return thumbnailData != null ? thumbnailData.length : 0;
        }

        public int getThumbnailWidth() {
            return thumbnailWidth;
        }

        public int getThumbnailHeight() {
            return thumbnailHeight;
        }
    }
}