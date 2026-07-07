package com.cityglow.repository;

import com.cityglow.entity.ObservationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 观星日志 Repository。
 */
public interface ObservationLogRepository extends JpaRepository<ObservationLog, Long> {

    /**
     * 按经纬度范围查询日志(地图视口查询),按创建时间倒序。
     *
     * @param latMin 纬度下界
     * @param latMax 纬度上界
     * @param lngMin 经度下界
     * @param lngMax 经度上界
     * @return 范围内的日志列表,按 created_at 倒序
     */
    List<ObservationLog> findByLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
            BigDecimal latMin, BigDecimal latMax, BigDecimal lngMin, BigDecimal lngMax);
}
