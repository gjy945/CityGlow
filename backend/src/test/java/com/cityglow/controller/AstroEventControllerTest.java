package com.cityglow.controller;

import com.cityglow.entity.AstroEvent;
import com.cityglow.repository.AstroEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AstroEventController Web 层测试(@WebMvcTest + MockMvc)。
 *
 * <p>用 {@link MockBean} mock {@link AstroEventRepository},不加载完整 Spring 上下文,
 * 只装配 AstroEventController 的 MVC 基础设施(Jackson 序列化、参数绑定)。</p>
 *
 * <p>验证:</p>
 * <ul>
 *   <li>GET /api/v1/events 无筛选 → 200 + 列表</li>
 *   <li>GET /api/v1/events?type=METEOR → 200 + 按 type 筛选</li>
 *   <li>GET /api/v1/events?after=2026-06-01T00:00:00 → 200 + 按 after 筛选</li>
 *   <li>GET /api/v1/events/1 → 200 + 单个</li>
 *   <li>GET /api/v1/events/999 → 200 + ApiResponse.error(404, ...) (业务码,非 HTTP 404)</li>
 * </ul>
 */
@WebMvcTest(AstroEventController.class)
class AstroEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AstroEventRepository astroEventRepository;

    /**
     * 无筛选 → 200,返回全部事件列表,JSON 含 code/message/data 数组。
     */
    @Test
    void list_noFilter_returnsAllEvents() throws Exception {
        AstroEvent e1 = buildEvent(1L, "象限仪座流星雨",
                LocalDateTime.of(2026, 1, 4, 3, 0), "METEOR");
        AstroEvent e2 = buildEvent(2L, "金星西大距",
                LocalDateTime.of(2026, 2, 14, 19, 0), "PLANET");
        when(astroEventRepository.findAll(org.springframework.data.domain.Sort.by("eventTime").ascending()))
                .thenReturn(List.of(e1, e2));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("象限仪座流星雨"))
                .andExpect(jsonPath("$.data[0].eventType").value("METEOR"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].eventType").value("PLANET"));
    }

    /**
     * type=METEOR → 200,只返回流星雨类型事件。
     */
    @Test
    void list_filterByType_returnsMeteorsOnly() throws Exception {
        AstroEvent meteor = buildEvent(1L, "象限仪座流星雨",
                LocalDateTime.of(2026, 1, 4, 3, 0), "METEOR");
        when(astroEventRepository.findByEventType("METEOR"))
                .thenReturn(List.of(meteor));

        mockMvc.perform(get("/api/v1/events").param("type", "METEOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].eventType").value("METEOR"));
    }

    /**
     * after=2026-06-01T00:00:00 → 200,只返回 6 月后的事件。
     * 验证 ISO-8601 时间参数绑定正确。
     */
    @Test
    void list_filterByAfter_returnsFutureEvents() throws Exception {
        AstroEvent future = buildEvent(1L, "英仙座流星雨极大",
                LocalDateTime.of(2026, 8, 13, 3, 0), "METEOR");
        LocalDateTime after = LocalDateTime.of(2026, 6, 1, 0, 0);
        when(astroEventRepository.findByEventTimeAfterOrderByEventTimeAsc(after))
                .thenReturn(List.of(future));

        mockMvc.perform(get("/api/v1/events")
                        .param("after", "2026-06-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("英仙座流星雨极大"));
    }

    /**
     * id=1 存在 → 200 + 单个事件 JSON。
     */
    @Test
    void getById_exists_returns200WithEvent() throws Exception {
        AstroEvent e = buildEvent(1L, "双子座流星雨极大",
                LocalDateTime.of(2026, 12, 14, 9, 30), "METEOR");
        when(astroEventRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/api/v1/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("双子座流星雨极大"))
                .andExpect(jsonPath("$.data.eventType").value("METEOR"));
    }

    /**
     * id=999 不存在 → HTTP 200(因 ApiResponse.error 包装而非抛异常),
     * 但业务码 code=404,message="Event not found",data=null。
     */
    @Test
    void getById_notExists_returns404InPayload() throws Exception {
        when(astroEventRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/events/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Event not found"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * 构造测试用 AstroEvent 实例。
     */
    private AstroEvent buildEvent(Long id, String title, LocalDateTime time, String type) {
        AstroEvent e = new AstroEvent();
        e.setId(id);
        e.setTitle(title);
        e.setEventTime(time);
        e.setDescription("测试描述");
        e.setEventType(type);
        return e;
    }
}
