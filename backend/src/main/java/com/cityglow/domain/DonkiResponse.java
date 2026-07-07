package com.cityglow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NASA DONKI (Database Of Notifications, Knowledge, and Information) 响应 DTO。
 *
 * <p>封装两个端点的合并结果:</p>
 * <ul>
 *   <li>{@code /GST} - Geomagnetic Storm(地磁暴)事件,可能引发极光</li>
 *   <li>{@code /FLR} - Solar Flare(太阳耀斑)事件,X 级耀斑可引发极光</li>
 * </ul>
 *
 * <p>NASA DONKI 返回字段:</p>
 * <ul>
 *   <li>GST:{@code activityID}(唯一 ID),{@code startTime}(开始时间),
 *       {@code observedTime}(观测时间),{@code link}(详情链接)</li>
 *   <li>FLR:{@code flareID}(耀斑 ID),{@code classType}(耀斑级别,如 "X1.5"、"M3.2"),
 *       {@code peakTime}(峰值时间),{@code link}(详情链接)</li>
 * </ul>
 *
 * <p>用 {@link JsonProperty} 显式映射 NASA 字段名到 record 组件。
 * {@link JsonIgnoreProperties}(ignoreUnknown = true)忽略未识别字段,
 * 保证 NASA 加新字段不破坏反序列化。</p>
 *
 * <p>顶层字段用 {@code Optional} 风格的 null 兜底:NASA 偶发返回 null 列表时
 * 通过构造器规范化为空列表,避免 NPE。</p>
 *
 * @param gstEvents 地磁暴事件列表(可能为空)
 * @param flrEvents 太阳耀斑事件列表(可能为空)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DonkiResponse(
        @JsonProperty("gstEvents") List<GeomagneticStorm> gstEvents,
        @JsonProperty("flrEvents") List<SolarFlare> flrEvents
) {

    /**
     * 规范化构造器:null 列表转为空列表,避免下游 NPE。
     */
    public DonkiResponse {
        gstEvents = (gstEvents == null) ? List.of() : gstEvents;
        flrEvents = (flrEvents == null) ? List.of() : flrEvents;
    }

    /**
     * 地磁暴事件(Geomagnetic Storm)。
     *
     * <p>地磁暴由日冕物质抛射(CME)冲击地球磁层引起,Kp 指数 ≥ 5 即视为地磁暴。
     * 强地磁暴(Kp ≥ 7)可在中低纬度引发极光,CityGlow 据此推荐极光观测。</p>
     *
     * @param activityID   事件唯一 ID(NASA 内部标识)
     * @param startTime    事件开始时间(UTC)
     * @param observedTime 观测时间(UTC)
     * @param link         NASA DONKI 详情页链接
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeomagneticStorm(
            @JsonProperty("activityID") String activityID,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss['Z']", timezone = "UTC")
            @JsonProperty("startTime") LocalDateTime startTime,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss['Z']", timezone = "UTC")
            @JsonProperty("observedTime") LocalDateTime observedTime,
            @JsonProperty("link") String link
    ) {
    }

    /**
     * 太阳耀斑事件(Solar Flare)。
     *
     * <p>耀斑按 X 射线峰值流量分级:A &lt; B &lt; C &lt; M &lt; X。
     * X 级最强,M 级次之。X 级耀斑常引发强地磁暴,可观测到极光。</p>
     *
     * @param flareID   耀斑唯一 ID
     * @param classType 耀斑级别(如 "X1.5"、"M3.2"、"C5.0")
     * @param peakTime  峰值时间(UTC)
     * @param link      NASA DONKI 详情页链接
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SolarFlare(
            @JsonProperty("flareID") String flareID,
            @JsonProperty("classType") String classType,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss['Z']", timezone = "UTC")
            @JsonProperty("peakTime") LocalDateTime peakTime,
            @JsonProperty("link") String link
    ) {
    }
}
