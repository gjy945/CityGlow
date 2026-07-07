package com.cityglow.service;

import com.cityglow.domain.OpenWeatherOneCallResponse;
import com.cityglow.domain.PostcardResult;
import com.cityglow.domain.WatermarkInfo;
import com.cityglow.util.BortleEstimator;
import com.cityglow.util.MoonPhaseDescription;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;

/**
 * 星空明信片生成服务(设计文档第 4 节模块 4、第 6 节结构化并发)。
 *
 * <p>核心职责:接收用户上传的原始图片字节,用 <b>JDK 21 结构化并发
 * (StructuredTaskScope)</b> 并行执行两个独立子任务,合并后串行收尾
 * (画水印 + 编码 JPEG),返回 {@link PostcardResult}。</p>
 *
 * <p><b>并行点设计</b>:真正的并行机会在于"图片解码压缩"(CPU/IO 密集)
 * 与"水印元数据准备"(需调 OpenWeather 取月相,涉及外部网络往返)
 * 互不依赖,可同时进行。画水印依赖前两者结果,故放在 join 后串行执行。</p>
 *
 * <pre>
 * try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
 *     Subtask&lt;BufferedImage&gt; imageTask = scope.fork(() -&gt; decodeAndCompress(bytes));
 *     Subtask&lt;WatermarkInfo&gt; infoTask = scope.fork(() -&gt; prepareWatermarkInfo(lat, lng, name));
 *     scope.join();
 *     scope.throwIfFailed(ex -&gt; new RuntimeException("Postcard generation failed", ex));
 *     // 串行收尾:画水印 + 编码
 * }
 * </pre>
 *
 * <p><b>ShutdownOnFailure 语义</b>:任一子任务失败则取消其余子任务并快速失败,
 * 避免无谓计算。{@code throwIfFailed} 将子任务异常包装为 RuntimeException 抛出。</p>
 *
 * <p><b>StructuredTaskScope 是 JDK 21 PREVIEW 特性</b>,编译与运行均需
 * {@code --enable-preview}(见 pom.xml 三处配置)。</p>
 *
 * <p><b>EXIF 元数据</b>:务实跳过(需 commons-imaging 依赖,YAGNI)。
 * 水印已包含经纬度/时间/月相/Bortle 全部元数据的可视化形式,等价传达。</p>
 */
@Service
public class PostcardService {

    /** 压缩后长边像素上限。 */
    static final int MAX_LONG_EDGE = 1920;
    /** JPEG 编码质量(0-1)。 */
    static final float JPEG_QUALITY = 0.85f;

    private final OpenWeatherClient openWeatherClient;

    public PostcardService(OpenWeatherClient openWeatherClient) {
        this.openWeatherClient = openWeatherClient;
    }

    /**
     * 生成星空明信片:并行(解码压缩 + 水印元数据)→ 串行(画水印 + 编码)。
     *
     * @param imageBytes   原始图片字节(JPEG/PNG)
     * @param lat          纬度
     * @param lng          经度
     * @param locationName 地点名称(可空)
     * @param description  描述(当前未写入水印,保留参数)
     * @return 明信片结果(含 JPEG 字节与水印元数据)
     * @throws RuntimeException 若子任务失败或被中断
     */
    public PostcardResult generate(byte[] imageBytes, double lat, double lng,
                                   String locationName, String description) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // 子任务 1:解码并压缩原图(CPU/IO 密集)
            StructuredTaskScope.Subtask<BufferedImage> imageTask =
                    scope.fork(() -> decodeAndCompress(imageBytes));
            // 子任务 2:准备水印元数据(调 OpenWeather 取月相 + Bortle 查表 + 文本格式化)
            StructuredTaskScope.Subtask<WatermarkInfo> infoTask =
                    scope.fork(() -> prepareWatermarkInfo(lat, lng, locationName));

            scope.join();
            // 任一子任务失败则抛出,取消另一在途子任务(ShutdownOnFailure 已处理取消)
            scope.throwIfFailed(ex -> new RuntimeException("Postcard generation failed", ex));

            BufferedImage image = imageTask.get();
            WatermarkInfo info = infoTask.get();

            // 串行收尾:画水印 + 编码 JPEG(依赖前两者结果)
            BufferedImage watermarked = drawWatermark(image, info);
            byte[] jpegBytes;
            try {
                jpegBytes = encodeJpeg(watermarked);
            } catch (IOException e) {
                throw new RuntimeException("JPEG encode failed", e);
            }
            return new PostcardResult(jpegBytes, info);
        } catch (InterruptedException e) {
            // 重新设置中断标志,遵守 Java 中断协议
            Thread.currentThread().interrupt();
            throw new RuntimeException("Postcard generation interrupted", e);
        }
    }

    /**
     * 解码原图并按长边缩放到 ≤ 1920px,保持纵横比。
     *
     * <p>包级可见以支持聚焦单元测试(压缩是独立可验证的行为)。</p>
     *
     * @param bytes 原始图片字节
     * @return 解码(并可能缩放)后的 BufferedImage(RGB 类型)
     * @throws IOException 若格式不支持或解码失败
     */
    BufferedImage decodeAndCompress(byte[] bytes) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
        if (original == null) {
            throw new IOException("Unsupported image format");
        }
        int origW = original.getWidth();
        int origH = original.getHeight();
        int longEdge = Math.max(origW, origH);
        if (longEdge <= MAX_LONG_EDGE) {
            return toRgb(original);
        }
        double scale = (double) MAX_LONG_EDGE / longEdge;
        int newW = (int) Math.round(origW * scale);
        int newH = (int) Math.round(origH * scale);
        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, newW, newH, null);
        } finally {
            g.dispose();
        }
        return scaled;
    }

    /**
     * 准备水印元数据:查 Bortle 等级(本地表)、调 OpenWeather 取月相、格式化文本。
     *
     * <p>包级可见以支持单元测试。OpenWeather 返回 null 时月相兜底为 0(新月)。</p>
     *
     * @param lat          纬度
     * @param lng          经度
     * @param locationName 地点名称(可空,空则用 "Unknown location")
     * @return 水印元数据
     */
    WatermarkInfo prepareWatermarkInfo(double lat, double lng, String locationName) {
        int bortle = BortleEstimator.estimate(lat, lng);
        OpenWeatherOneCallResponse oneCall = openWeatherClient.getOneCall(lat, lng);
        double moonPhase = (oneCall != null && oneCall.current() != null)
                ? oneCall.current().moonPhase() : 0.0;
        String moonPhaseDesc = MoonPhaseDescription.fromPhase(moonPhase);
        String coords = String.format("%.4f, %.4f", lat, lng);
        String dateTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String location = (locationName != null && !locationName.isBlank())
                ? locationName : "Unknown location";
        return new WatermarkInfo(location, coords, dateTime, moonPhaseDesc, bortle);
    }

    /**
     * 在图片右下角绘制半透明白色水印文字(地点/经纬度/时间/月相+Bortle)。
     *
     * <p>包级可见以支持单元测试。直接在传入图片上绘制(原地修改)。</p>
     *
     * @param image 目标图片
     * @param info  水印元数据
     * @return 同一张图片(已绘水印)
     */
    BufferedImage drawWatermark(BufferedImage image, WatermarkInfo info) {
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int fontSize = Math.max(12, image.getWidth() / 60);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
            g.setColor(new Color(255, 255, 255, 210));
            FontMetrics fm = g.getFontMetrics();
            List<String> lines = buildWatermarkLines(info);
            int lineHeight = fm.getHeight();
            int margin = fontSize;
            int startY = image.getHeight() - margin - (lines.size() - 1) * lineHeight;
            for (String line : lines) {
                int textWidth = fm.stringWidth(line);
                // 右对齐:从右边距向左偏移文本宽度
                g.drawString(line, image.getWidth() - margin - textWidth, startY);
                startY += lineHeight;
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * 将 BufferedImage 编码为 JPEG 字节流(质量 0.85)。
     *
     * <p>包级可见以支持单元测试。用 ImageWriter 显式设置压缩质量,
     * 而非 ImageIO.write 的默认质量。</p>
     *
     * @param image 目标图片
     * @return JPEG 字节流
     * @throws IOException 若编码失败
     */
    byte[] encodeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    /**
     * 构造水印文本行(右下角从上到下)。
     */
    private List<String> buildWatermarkLines(WatermarkInfo info) {
        List<String> lines = new ArrayList<>();
        lines.add(info.location());
        lines.add(info.coordinates());
        lines.add(info.dateTime());
        lines.add("Moon: " + info.moonPhase() + " | Bortle: " + info.bortleLevel());
        return lines;
    }

    /**
     * 将任意类型 BufferedImage 转为 TYPE_INT_RGB(避免 PNG alpha 通道在 JPEG 编码时变红)。
     */
    private BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }
}
