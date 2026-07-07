package com.cityglow.repository;

import com.cityglow.entity.AstroEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 天文事件 Repository。
 */
public interface AstroEventRepository extends JpaRepository<AstroEvent, Long> {

    /**
     * 查询指定时间之后的事件,按事件时间升序(时间轴展示用)。
     */
    List<AstroEvent> findByEventTimeAfterOrderByEventTimeAsc(LocalDateTime after);

    /**
     * 按事件类型筛选(如 METEOR / ECLIPSE / PLANET)。
     */
    List<AstroEvent> findByEventType(String eventType);
}
