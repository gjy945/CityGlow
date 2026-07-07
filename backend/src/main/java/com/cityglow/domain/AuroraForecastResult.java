package com.cityglow.domain;

import com.cityglow.domain.DonkiResponse.GeomagneticStorm;
import com.cityglow.domain.DonkiResponse.SolarFlare;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 极光预报前端契约 DTO。
 *
 * <p>后端 {@link DonkiResponse} 把 NASA DONKI 的地磁暴(GST)与太阳耀斑(FLR)
 * 分成两个列表返回,但前端 {@code AuroraCard.vue} 期待扁平化的统一事件列表
 * (按时间倒序)。本类负责这层转换,对齐前后端契约。</p>
 *
 * <p>字段语义:</p>
 * <ul>
 *   <li>{@code totalCount}:近 N 天事件总数(GST + FLR)</li>
 *   <li>{@code events}:扁平化事件列表,按 startTime 倒序(最近在前)</li>
 * </ul>
 *
 * <p>每个 {@link AuroraEvent} 的字段映射:</p>
 * <ul>
 *   <li>{@code id} ← GST.activityID / FLR.flareID</li>
 *   <li>{@code type} ← "GST" / "FLR"</li>
 *   <li>{@code startTime} ← GST.startTime / FLR.peakTime</li>
 *   <li>{@code endTime} ← GST.observedTime(FLR 无对应字段,留空)</li>
 *   <li>{@code link} ← GST.link / FLR.link</li>
 *   <li>{@code note} ← FLR.classType(耀斑级别,如 "X1.5";GST 无)</li>
 * </ul>
 *
 * @param totalCount 事件总数
 * @param events     扁平化事件列表(按时间倒序)
 */
public record AuroraForecastResult(
        int totalCount,
        List<AuroraEvent> events
) {

    /**
     * 规范化构造器:null 列表转空列表。
     */
    public AuroraForecastResult {
        events = (events == null) ? List.of() : events;
    }

    /**
     * 从 DonkiResponse 构造前端契约对象:扁平化两个列表并按时间倒序排序。
     *
     * @param response DONKI 原始响应
     * @return 前端契约对象
     */
    public static AuroraForecastResult from(DonkiResponse response) {
        List<AuroraEvent> events = new ArrayList<>();
        for (GeomagneticStorm gst : response.gstEvents()) {
            events.add(new AuroraEvent(
                    gst.activityID(),
                    "GST",
                    gst.startTime(),
                    gst.observedTime(),
                    gst.link(),
                    null
            ));
        }
        for (SolarFlare flr : response.flrEvents()) {
            events.add(new AuroraEvent(
                    flr.flareID(),
                    "FLR",
                    flr.peakTime(),
                    null,
                    flr.link(),
                    flr.classType()
            ));
        }
        // 按 startTime 倒序(最近的在前;null 时间排到最后)
        events.sort((a, b) -> {
            if (a.startTime() == null && b.startTime() == null) return 0;
            if (a.startTime() == null) return 1;
            if (b.startTime() == null) return -1;
            return b.startTime().compareTo(a.startTime());
        });
        return new AuroraForecastResult(events.size(), List.copyOf(events));
    }

    /**
     * 单个极光相关事件(扁平化后)。
     *
     * @param id        事件唯一 ID
     * @param type      事件类型:"GST" / "FLR"
     * @param startTime 开始时间(GST.startTime / FLR.peakTime)
     * @param endTime   结束时间(GST.observedTime;FLR 无,为 null)
     * @param link      NASA DONKI 详情页链接
     * @param note      备注(FLR.classType,如 "X1.5";GST 无,为 null)
     */
    public record AuroraEvent(
            String id,
            String type,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String link,
            String note
    ) {
    }
}
