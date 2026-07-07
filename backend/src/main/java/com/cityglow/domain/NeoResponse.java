package com.cityglow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * NASA NeoWs (Near Earth Object Web Service) Feed 端点响应 DTO。
 *
 * <p>封装 {@code GET /neo/rest/v1/feed} 响应,按日期分组返回近 7 天接近地球的小行星列表。</p>
 *
 * <p>NASA NeoWs 返回字段:</p>
 * <ul>
 *   <li>顶层 {@code near_earth_objects} - Map&lt;YYYY-MM-DD, List&lt;Asteroid&gt;&gt;</li>
 *   <li>每个 Asteroid 含 {@code id}, {@code name}, {@code absolute_magnitude_h},
 *       嵌套 {@code estimated_diameter.kilometers.estimated_diameter_min/max}(单位 km,转米),
 *       {@code is_potentially_hazardous_asteroid}, {@code close_approach_data} 列表</li>
 *   <li>每个 CloseApproachData 含 {@code close_approach_date_full}(如 "2024-Jan-01 00:00"),
 *       嵌套 {@code relative_velocity.kilometers_per_hour},
 *       嵌套 {@code miss_distance.kilometers}</li>
 * </ul>
 *
 * <p><b>嵌套 record + @JsonCreator</b>:用 @JsonCreator 静态工厂方法把嵌套字段
 * 拍平为 record 的扁平字段(estimated_diameter_min_meters 等),
 * 调用方无需感知 NASA 的多层嵌套结构。</p>
 *
 * <p>{@link JsonIgnoreProperties}(ignoreUnknown = true)忽略未识别字段,
 * 保证 NASA 加新字段不破坏反序列化。</p>
 *
 * @param nearEarthObjects 按日期分组的近地小行星列表
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NeoResponse(
        @JsonProperty("near_earth_objects") Map<LocalDate, List<Asteroid>> nearEarthObjects
) {

    /**
     * 规范化构造器:null Map 转为空 Map。
     */
    public NeoResponse {
        nearEarthObjects = (nearEarthObjects == null) ? Map.of() : nearEarthObjects;
    }

    /**
     * 近地小行星(Asteroid)。
     *
     * @param id                            NASA 内部 ID(字符串)
     * @param name                          小行星名称(如 "433 Eros (A898 PA)")
     * @param absoluteMagnitudeH            绝对星等 H(亮度指标,越小越亮)
     * @param estimatedDiameterMinMeters    估算直径下限(米,从 km 转换)
     * @param estimatedDiameterMaxMeters    估算直径上限(米,从 km 转换)
     * @param isPotentiallyHazardous        是否为潜在危险小行星(PHA)
     * @param closeApproachData             接近地球数据列表(可能多次接近)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Asteroid(
            String id,
            String name,
            double absoluteMagnitudeH,
            double estimatedDiameterMinMeters,
            double estimatedDiameterMaxMeters,
            boolean isPotentiallyHazardous,
            List<CloseApproachData> closeApproachData
    ) {

        /**
         * JSON 反序列化工厂:拍平 estimated_diameter.kilometers 嵌套结构,
         * 把 km 转 m(×1000),null close_approach_data 转 List.of()。
         *
         * @param id                            NASA ID
         * @param name                          名称
         * @param absoluteMagnitudeH            绝对星等
         * @param estimatedDiameter             嵌套直径对象
         * @param isPotentiallyHazardousAsteroid 是否危险
         * @param closeApproachData             接近数据列表
         * @return 拍平后的 Asteroid 实例
         */
        @JsonCreator
        public static Asteroid fromJson(
                @JsonProperty("id") String id,
                @JsonProperty("name") String name,
                @JsonProperty("absolute_magnitude_h") double absoluteMagnitudeH,
                @JsonProperty("estimated_diameter") EstimatedDiameter estimatedDiameter,
                @JsonProperty("is_potentially_hazardous_asteroid") boolean isPotentiallyHazardousAsteroid,
                @JsonProperty("close_approach_data") List<CloseApproachData> closeApproachData
        ) {
            double minMeters = 0.0;
            double maxMeters = 0.0;
            if (estimatedDiameter != null && estimatedDiameter.kilometers() != null) {
                minMeters = estimatedDiameter.kilometers().estimatedDiameterMin() * 1000.0;
                maxMeters = estimatedDiameter.kilometers().estimatedDiameterMax() * 1000.0;
            }
            List<CloseApproachData> safeList = (closeApproachData == null) ? List.of() : closeApproachData;
            return new Asteroid(
                    id, name, absoluteMagnitudeH,
                    minMeters, maxMeters,
                    isPotentiallyHazardousAsteroid,
                    safeList
            );
        }

        /**
         * 嵌套 estimated_diameter 容器。
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record EstimatedDiameter(
                @JsonProperty("kilometers") Kilometers kilometers
        ) {
        }

        /**
         * 嵌套 kilometers 直径范围(NASA 单位 km)。
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Kilometers(
                @JsonProperty("estimated_diameter_min") double estimatedDiameterMin,
                @JsonProperty("estimated_diameter_max") double estimatedDiameterMax
        ) {
        }
    }

    /**
     * 小行星接近地球数据。
     *
     * @param closeApproachDate    接近日期(从 NASA "close_approach_date_full" 解析,如 "2024-Jan-01 00:00")
     * @param relativeVelocityKph  相对速度(km/h,从 relative_velocity.kilometers_per_hour 取)
     * @param missDistanceKm       错过距离(km,从 miss_distance.kilometers 取)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CloseApproachData(
            LocalDate closeApproachDate,
            double relativeVelocityKph,
            double missDistanceKm
    ) {

        /** NASA 日期格式 "2024-Jan-01 00:00",月份用英文缩写,需 Locale.ENGLISH。 */
        private static final DateTimeFormatter NASA_DATE_FORMAT =
                DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm", Locale.ENGLISH);

        /**
         * JSON 反序列化工厂:拍平 relative_velocity 与 miss_distance 嵌套结构,
         * 解析 NASA 自定义日期格式 "2024-Jan-01 00:00" 为 LocalDate。
         *
         * @param closeApproachDateFull NASA 原始日期字符串
         * @param relativeVelocity      嵌套速度对象
         * @param missDistance          嵌套距离对象
         * @return 拍平后的 CloseApproachData 实例
         */
        @JsonCreator
        public static CloseApproachData fromJson(
                @JsonProperty("close_approach_date_full") String closeApproachDateFull,
                @JsonProperty("relative_velocity") RelativeVelocity relativeVelocity,
                @JsonProperty("miss_distance") MissDistance missDistance
        ) {
            return new CloseApproachData(
                    parseNasaDate(closeApproachDateFull),
                    (relativeVelocity != null) ? relativeVelocity.kilometersPerHour() : 0.0,
                    (missDistance != null) ? missDistance.kilometers() : 0.0
            );
        }

        /**
         * 解析 NASA 日期字符串 "2024-Jan-01 00:00" 为 LocalDate。
         *
         * @param s NASA 日期字符串
         * @return LocalDate,解析失败返回 null
         */
        private static LocalDate parseNasaDate(String s) {
            if (s == null || s.isBlank()) {
                return null;
            }
            try {
                return LocalDate.parse(s, NASA_DATE_FORMAT);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * 嵌套 relative_velocity 容器。
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record RelativeVelocity(
                @JsonProperty("kilometers_per_hour") double kilometersPerHour
        ) {
        }

        /**
         * 嵌套 miss_distance 容器。
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record MissDistance(
                @JsonProperty("kilometers") double kilometers
        ) {
        }
    }
}
