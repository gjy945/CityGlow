package com.cityglow.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * NASA APOD (Astronomy Picture of the Day) API 响应 DTO(设计文档第 4 节模块 3)。
 *
 * <p>NASA API 返回 JSON 字段采用 snake_case(如 {@code media_type}),
 * 而 Java record 组件按惯例使用 camelCase。用 {@link JsonProperty} 显式映射,
 * Jackson 反序列化时正确识别。</p>
 *
 * <p>不可变 record,线程安全,适合在 ApodController 缓存中复用。</p>
 *
 * @param title      图片标题
 * @param explanation 解释说明
 * @param url        标清图 URL
 * @param hdurl      高清图 URL,可为 null(NASA 视频类型时无此字段)
 * @param mediaType  媒体类型(典型值 "image" / "video"),映射自 JSON {@code media_type}
 * @param date       APOD 日期(YYYY-MM-DD)
 * @param copyright  版权声明,可选(NASA 公共领域图片可能缺此字段)
 */
public record NasaApodResponse(
        String title,
        String explanation,
        String url,
        String hdurl,
        @JsonProperty("media_type") String mediaType,
        String date,
        String copyright
) {
}
