import cn.hutool.core.io.FileUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        //Image Io webp Test
        System.out.println("测试Webp支持");
        File file = new File("test\\1548.webp");
        if(!file.exists()){
            System.out.println("文件不存在");
            return;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if(image == null){
                System.out.println("无法加载webp");
            }else {
                System.out.println("webp加载成功");
            }
            conventToWebp();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void conventToWebp() throws IOException {
        File file = new File("test\\LED.jpeg");
        if(!file.exists()){
            System.out.println("文件不存在");
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(!ImageIO.write(ImageIO.read(file), "webp", baos)){
            System.out.println("转换失败");
            return;
        }
        byte[] bytes = baos.toByteArray();
        if(bytes.length == 0){
            System.out.println("转换失败");
            return;
        }
        File touch = FileUtil.touch(new File("test\\LED.webp"));
        FileUtil.writeBytes(bytes, touch);

        System.out.println("转换成功");
    }
}
