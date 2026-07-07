package com.cityglow.controller;

import com.cityglow.domain.ApiResponse;
import com.cityglow.domain.PostcardResult;
import com.cityglow.domain.WatermarkInfo;
import com.cityglow.entity.ObservationLog;
import com.cityglow.repository.ObservationLogRepository;
import com.cityglow.service.PostcardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ObservationLogController Web 层测试(@WebMvcTest + MockMvc)。
 *
 * <p>用 {@link MockBean} mock {@link PostcardService} 与
 * {@link ObservationLogRepository},不加载完整 Spring 上下文与真实图片处理。
 * 上传目录通过 {@code app.upload.dir=target/test-uploads} 重定向到 target/(已 gitignore),
 * 避免污染工程目录。</p>
 *
 * <p>验证:</p>
 * <ul>
 *   <li>POST /upload 合法 JPEG → 200 + {logId, cardUrl}</li>
 *   <li>POST /upload 非法 MIME(image/gif)→ 业务码 400</li>
 *   <li>GET /{id} 存在 → 200 + 单条</li>
 *   <li>GET /{id} 不存在 → 业务码 404</li>
 *   <li>GET / 列表 → 200 + 倒序列表</li>
 * </ul>
 */
@WebMvcTest(ObservationLogController.class)
@TestPropertySource(properties = "app.upload.dir=target/test-uploads")
class ObservationLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostcardService postcardService;

    @MockBean
    private ObservationLogRepository logRepository;

    /**
     * 合法 JPEG 上传 → 200,返回 logId 与 cardUrl。
     * PostcardService 与 Repository 均 mock,验证编排逻辑与字段映射。
     */
    @Test
    void upload_validJpeg_returns200WithLogIdAndCardUrl() throws Exception {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", jpegBytes);

        WatermarkInfo info = new WatermarkInfo(
                "北京灵山", "39.9000, 116.4000", "2026-07-07 22:30", "Full Moon", 5);
        PostcardResult result = new PostcardResult(new byte[]{1, 2, 3}, info);
        when(postcardService.generate(any(byte[].class), anyDouble(), anyDouble(),
                any(), any())).thenReturn(result);

        ObservationLog saved = buildLog(1L, "北京灵山", 5);
        when(logRepository.save(any(ObservationLog.class))).thenReturn(saved);

        mockMvc.perform(multipart("/api/v1/logs/upload")
                        .file(image)
                        .param("lat", "39.9")
                        .param("lng", "116.4")
                        .param("locationName", "北京灵山")
                        .param("description", "银河清晰"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.logId").value(1))
                .andExpect(jsonPath("$.data.cardUrl").value("/uploads/1.jpg"));
    }

    /**
     * 非法 MIME(image/gif)→ HTTP 200,业务码 400,message="Only JPEG/PNG allowed"。
     * 与现有 Controller 约定一致(业务码在 payload,HTTP 始终 200)。
     */
    @Test
    void upload_invalidMime_returns400InPayload() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.gif", "image/gif", new byte[]{0});

        mockMvc.perform(multipart("/api/v1/logs/upload")
                        .file(image)
                        .param("lat", "39.9")
                        .param("lng", "116.4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Only JPEG/PNG allowed"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * id=1 存在 → 200 + 单条日志 JSON。
     */
    @Test
    void getById_exists_returns200WithLog() throws Exception {
        ObservationLog log = buildLog(1L, "北京灵山", 5);
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));

        mockMvc.perform(get("/api/v1/logs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.locationName").value("北京灵山"))
                .andExpect(jsonPath("$.data.bortleLevel").value(5));
    }

    /**
     * id=999 不存在 → HTTP 200,业务码 404,message="Log not found",data=null。
     */
    @Test
    void getById_notExists_returns404InPayload() throws Exception {
        when(logRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/logs/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Log not found"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * 列表 → 200 + 倒序列表(验证 findAll(Sort DESC createdAt) 被调用)。
     */
    @Test
    void list_returnsAllLogsDescending() throws Exception {
        ObservationLog first = buildLog(2L, "天津", 6);
        ObservationLog second = buildLog(1L, "北京灵山", 5);
        when(logRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")))
                .thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[1].id").value(1));
    }

    /**
     * 构造测试用 ObservationLog(id 非空,其余字段填充)。
     */
    private ObservationLog buildLog(Long id, String locationName, int bortle) {
        ObservationLog log = new ObservationLog();
        log.setId(id);
        log.setLocationName(locationName);
        log.setLatitude(new BigDecimal("39.9000000"));
        log.setLongitude(new BigDecimal("116.4000000"));
        log.setBortleLevel(bortle);
        log.setImageUrl("/uploads/" + id + ".jpg");
        log.setDescription("测试描述");
        return log;
    }
}
