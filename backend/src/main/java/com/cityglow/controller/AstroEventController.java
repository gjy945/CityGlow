package com.cityglow.controller;

import com.cityglow.domain.ApiResponse;
import com.cityglow.entity.AstroEvent;
import com.cityglow.repository.AstroEventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 天文事件 REST 接口(设计文档第 4 节模块 3、第 5.1 节接口契约)。
 *
 * <p>暴露:</p>
 * <ul>
 *   <li>{@code GET /api/v1/events}:列出所有事件,可选 {@code type}(类型)或
 *       {@code after}(ISO-8601 起始时间)筛选。无筛选时按 eventTime 升序返回全部。</li>
 *   <li>{@code GET /api/v1/events/{id}}:按主键取单个事件,不存在返回
 *       {@code ApiResponse.error(404, "Event not found")}。</li>
 * </ul>
 *
 * <p>不分页(YAGNI):天文事件年度数据量百级以内,全量返回即可。</p>
 */
@RestController
@RequestMapping("/api/v1/events")
public class AstroEventController {

    private final AstroEventRepository repo;

    public AstroEventController(AstroEventRepository repo) {
        this.repo = repo;
    }

    /**
     * 列出天文事件。
     *
     * <p>筛选优先级:type > after > 全部。两者不叠加,保持接口语义简单。</p>
     *
     * @param type  可选,事件类型(如 METEOR / ECLIPSE / PLANET)
     * @param after 可选,ISO-8601 起始时间(如 2026-06-01T00:00:00)
     * @return 统一 ApiResponse 包装的事件列表
     */
    @GetMapping
    public ApiResponse<List<AstroEvent>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after) {
        List<AstroEvent> events;
        if (type != null) {
            events = repo.findByEventType(type);
        } else if (after != null) {
            events = repo.findByEventTimeAfterOrderByEventTimeAsc(after);
        } else {
            events = repo.findAll(Sort.by("eventTime").ascending());
        }
        return ApiResponse.success(events);
    }

    /**
     * 按主键取单个事件。
     *
     * @param id 主键
     * @return 200 + 事件,或 404 错误响应(data=null)
     */
    @GetMapping("/{id}")
    public ApiResponse<AstroEvent> getById(@PathVariable Long id) {
        return repo.findById(id)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.error(404, "Event not found"));
    }
}
