package com.cityglow.domain;

/**
 * 明信片水印数据(JDK 21 Record,不可变)。
 *
 * <p>由 {@code PostcardService.prepareWatermarkInfo} 在结构化并发的子任务中
 * 并行准备,随后交给 {@code drawWatermark} 绘制到图片右下角。</p>
 *
 * @param location     地点名称(如 "北京灵山")
 * @param coordinates  格式化经纬度(如 "39.9890, 115.4320")
 * @param dateTime     格式化拍摄时间(如 "2026-07-07 22:30")
 * @param moonPhase    月相描述(如 "Full Moon")
 * @param bortleLevel  Bortle 暗空等级 1-9
 */
public record WatermarkInfo(
        String location,
        String coordinates,
        String dateTime,
        String moonPhase,
        int bortleLevel
) {
}
