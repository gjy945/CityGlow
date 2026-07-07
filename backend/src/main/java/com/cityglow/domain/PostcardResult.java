package com.cityglow.domain;

/**
 * 明信片生成结果(JDK 21 Record,不可变)。
 *
 * <p>{@code PostcardService.generate} 的返回值。{@code jpegBytes} 为已压缩、
 * 已加水印的 JPEG 字节流;{@code watermarkInfo} 为水印所用元数据,
 * 供 Controller 写入 observation_logs 表的 bortle_level 等字段。</p>
 *
 * @param jpegBytes     处理后的 JPEG 字节流(长边 ≤ 1920px)
 * @param watermarkInfo 水印元数据
 */
public record PostcardResult(
        byte[] jpegBytes,
        WatermarkInfo watermarkInfo
) {
}
