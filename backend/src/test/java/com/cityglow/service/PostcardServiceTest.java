package com.cityglow.service;

import com.cityglow.domain.PostcardResult;
import com.cityglow.domain.WatermarkInfo;
import com.cityglow.util.BortleEstimator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PostcardService 单元测试。
 *
 * <p>月相由 {@link com.cityglow.util.MoonPhaseCalculator} 根据当前日期计算,
 * 不再 mock {@link OpenWeatherClient}(无需调 OpenWeather One Call API)。
 * 验证:</p>
 * <ul>
 *   <li>decodeAndCompress:大图缩放到长边 1920,小图保持原样</li>
 *   <li>drawWatermark:在右下角绘制可见水印文字</li>
 *   <li>generate:并行(解码压缩 + 水印元数据)→ 画水印 → 编码,返回 PostcardResult</li>
 *   <li>generate 失败路径:非法图片字节 → ShutdownOnFailure 抛 RuntimeException</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PostcardServiceTest {

    @Mock
    private OpenWeatherClient openWeatherClient;  // 保留 mock 占位(PostcardService 构造注入,但不再调用)

    @InjectMocks
    private PostcardService postcardService;

    @Test
    void decodeAndCompress_largeImage_scaledTo1920() throws IOException {
        BufferedImage large = newImage(3000, 2000, Color.RED);
        byte[] bytes = toJpegBytes(large);

        BufferedImage result = postcardService.decodeAndCompress(bytes);

        assertThat(result.getWidth()).isEqualTo(1920);
        assertThat(result.getHeight()).isEqualTo(1280);
    }

    @Test
    void decodeAndCompress_smallImage_unchanged() throws IOException {
        BufferedImage small = newImage(800, 600, Color.BLUE);
        byte[] bytes = toJpegBytes(small);

        BufferedImage result = postcardService.decodeAndCompress(bytes);

        assertThat(result.getWidth()).isEqualTo(800);
        assertThat(result.getHeight()).isEqualTo(600);
    }

    @Test
    void drawWatermark_addsText() {
        BufferedImage image = newImage(400, 400, Color.BLACK);
        WatermarkInfo info = new WatermarkInfo(
                "测试地点", "39.9000, 116.4000", "2026-07-07 22:30", "Full Moon", 5);

        BufferedImage result = postcardService.drawWatermark(image, info);

        boolean hasTextPixel = false;
        for (int x = 200; x < 400 && !hasTextPixel; x++) {
            for (int y = 200; y < 400; y++) {
                int rgb = result.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (r + g + b > 60) {
                    hasTextPixel = true;
                    break;
                }
            }
        }
        assertThat(hasTextPixel)
                .as("右下角应绘制了可见水印文字")
                .isTrue();
    }

    /**
     * 全流程:传入小图 JPEG 字节,验证 PostcardResult 含非空 JPEG 字节与正确水印元数据。
     * 月相由天文算法计算,无法断言具体值,但应是非空字符串。
     */
    @Test
    void generate_returnsPostcardResultWithWatermark() throws IOException {
        double lat = 39.9;
        double lng = 116.4;
        BufferedImage src = newImage(100, 100, Color.DARK_GRAY);
        byte[] bytes = toJpegBytes(src);

        PostcardResult result = postcardService.generate(
                bytes, lat, lng, "北京灵山", "银河清晰");

        assertThat(result.jpegBytes()).isNotNull();
        assertThat(result.jpegBytes().length).isGreaterThan(0);
        WatermarkInfo info = result.watermarkInfo();
        assertThat(info.moonPhase()).isNotBlank();
        assertThat(info.bortleLevel()).isEqualTo(BortleEstimator.estimate(lat, lng));
        assertThat(info.location()).isEqualTo("北京灵山");
        assertThat(info.coordinates()).contains("39.9000", "116.4000");
        assertThat(info.dateTime()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}");
    }

    @Test
    void generate_invalidImageBytes_throwsRuntimeException() {
        double lat = 39.9;
        double lng = 116.4;
        byte[] invalidBytes = "not an image".getBytes();

        assertThatThrownBy(() ->
                postcardService.generate(invalidBytes, lat, lng, "测试", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Postcard generation failed");
    }

    private BufferedImage newImage(int w, int h, Color color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2DHelper.fill(img, color);
        return img;
    }

    private byte[] toJpegBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    static final class Graphics2DHelper {
        static void fill(BufferedImage img, Color color) {
            java.awt.Graphics2D g = img.createGraphics();
            try {
                g.setColor(color);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
            } finally {
                g.dispose();
            }
        }
    }
}
