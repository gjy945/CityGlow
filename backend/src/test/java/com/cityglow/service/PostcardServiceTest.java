package com.cityglow.service;

import com.cityglow.domain.OpenWeatherOneCallResponse;
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
import static org.mockito.Mockito.when;

/**
 * PostcardService 单元测试。
 *
 * <p>用 Mockito mock {@link OpenWeatherClient}(月相查询),不真实调用外部 API。
 * 验证三个独立可观察行为 + 结构化并发全流程:</p>
 * <ul>
 *   <li>decodeAndCompress:大图缩放到长边 1920,小图保持原样</li>
 *   <li>drawWatermark:在右下角绘制可见水印文字</li>
 *   <li>generate:并行(解码压缩 + 月相查询)→ 画水印 → 编码,返回 PostcardResult</li>
 *   <li>generate 失败路径:非法图片字节 → ShutdownOnFailure 抛 RuntimeException(快速失败)</li>
 * </ul>
 *
 * <p>helper 方法为包级可见,允许聚焦单元测试;generate 为公开入口,验证
 * StructuredTaskScope 并行编排与 join/throwIfFailed 收尾。</p>
 */
@ExtendWith(MockitoExtension.class)
class PostcardServiceTest {

    @Mock
    private OpenWeatherClient openWeatherClient;

    @InjectMocks
    private PostcardService postcardService;

    /**
     * 3000x2000 大图 → 长边缩到 1920,纵横比保持 → 1920x1280。
     */
    @Test
    void decodeAndCompress_largeImage_scaledTo1920() throws IOException {
        BufferedImage large = newImage(3000, 2000, Color.RED);
        byte[] bytes = toJpegBytes(large);

        BufferedImage result = postcardService.decodeAndCompress(bytes);

        assertThat(result.getWidth()).isEqualTo(1920);
        assertThat(result.getHeight()).isEqualTo(1280);
    }

    /**
     * 800x600 小图(长边 ≤ 1920)→ 保持原尺寸,不放大。
     */
    @Test
    void decodeAndCompress_smallImage_unchanged() throws IOException {
        BufferedImage small = newImage(800, 600, Color.BLUE);
        byte[] bytes = toJpegBytes(small);

        BufferedImage result = postcardService.decodeAndCompress(bytes);

        assertThat(result.getWidth()).isEqualTo(800);
        assertThat(result.getHeight()).isEqualTo(600);
    }

    /**
     * 黑底图片上画白色水印 → 右下角区域应出现非黑像素(文字)。
     */
    @Test
    void drawWatermark_addsText() {
        BufferedImage image = newImage(400, 400, Color.BLACK);
        WatermarkInfo info = new WatermarkInfo(
                "测试地点", "39.9000, 116.4000", "2026-07-07 22:30", "Full Moon", 5);

        BufferedImage result = postcardService.drawWatermark(image, info);

        // 右下角 1/4 区域内至少有一个非黑像素(水印文字)
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
     * 全流程:mock 月相=0.5(满月),传入小图 JPEG 字节。
     * 验证 PostcardResult 含非空 JPEG 字节与正确水印元数据。
     */
    @Test
    void generate_returnsPostcardResultWithWatermark() throws IOException {
        double lat = 39.9;
        double lng = 116.4;
        BufferedImage src = newImage(100, 100, Color.DARK_GRAY);
        byte[] bytes = toJpegBytes(src);

        OpenWeatherOneCallResponse oneCall = new OpenWeatherOneCallResponse(
                new OpenWeatherOneCallResponse.Current(1700000000L, 1700050000L, 0.5));
        when(openWeatherClient.getOneCall(lat, lng)).thenReturn(oneCall);

        PostcardResult result = postcardService.generate(
                bytes, lat, lng, "北京灵山", "银河清晰");

        assertThat(result.jpegBytes()).isNotNull();
        assertThat(result.jpegBytes().length).isGreaterThan(0);
        WatermarkInfo info = result.watermarkInfo();
        assertThat(info.moonPhase()).isEqualTo("Full Moon");
        assertThat(info.bortleLevel()).isEqualTo(BortleEstimator.estimate(lat, lng));
        assertThat(info.location()).isEqualTo("北京灵山");
        assertThat(info.coordinates()).contains("39.9000", "116.4000");
        assertThat(info.dateTime()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}");
    }

    /**
     * 非法图片字节 → decodeAndCompress 子任务抛 IOException →
     * ShutdownOnFailure 经 throwIfFailed 包装为 RuntimeException(快速失败)。
     * 验证结构化并发的失败传播语义。
     */
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

    /**
     * 构造纯色 BufferedImage(RGB 类型)。
     */
    private BufferedImage newImage(int w, int h, Color color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2DHelper.fill(img, color);
        return img;
    }

    /**
     * 将 BufferedImage 编码为 JPEG 字节(测试输入构造用)。
     */
    private byte[] toJpegBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    /** Graphics2D 填充辅助(避免在测试主流程中显式管理 Graphics2D 资源)。 */
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
