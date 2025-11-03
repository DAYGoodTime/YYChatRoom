import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
