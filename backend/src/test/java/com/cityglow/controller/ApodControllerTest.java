package com.cityglow.controller;

import com.cityglow.domain.NasaApodResponse;
import com.cityglow.service.NasaApodClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ApodController Web 层测试(@WebMvcTest + MockMvc)。
 *
 * <p>用 {@link MockBean} mock {@link NasaApodClient},验证:</p>
 * <ul>
 *   <li>首次调用调 client.getApod() 一次,返回 fresh 数据。</li>
 *   <li>第二次调用(缓存未过期)不调 client,直接返回缓存数据。
 *       验证 mock client 调用次数仍为 1。</li>
 *   <li>手动清缓存({@link ApodController#clearCache()})后再次调用,
 *       client 被调用第二次,返回新数据。</li>
 * </ul>
 *
 * <p>因 {@link ApodController#clearCache()} 包级可见,且本测试与控制器同包,
 * 可直接通过 {@code @Autowired} 注入控制器实例调用清缓存方法。
 * MockMvc 处理请求的控制器实例与注入的为同一 Spring 管理 bean。</p>
 */
@WebMvcTest(ApodController.class)
class ApodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApodController apodController;

    @MockBean
    private NasaApodClient nasaApodClient;

    /**
     * 每个测试前清缓存。
     *
     * <p>ApodController 是 Spring 单例,@WebMvcTest 跨测试方法复用同一实例,
     * 其 volatile 缓存字段不会自动重置。必须显式清空,否则前一个测试的缓存
     * 会污染后续测试(MockMvc 不重建 controller)。</p>
     */
    @BeforeEach
    void clearCache() {
        apodController.clearCache();
    }

    /**
     * 首次调用 → 200,fresh APOD 数据,client 被调一次。
     */
    @Test
    void getApod_firstCall_invokesClientAndReturnsFresh() throws Exception {
        NasaApodResponse apod = new NasaApodResponse(
                "Andromeda", "M31 说明",
                "https://apod.nasa.gov/image.jpg",
                "https://apod.nasa.gov/image_hd.jpg",
                "image", "2026-07-07", "NASA");
        when(nasaApodClient.getApod()).thenReturn(apod);

        mockMvc.perform(get("/api/v1/apod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.title").value("Andromeda"))
                // record 上 @JsonProperty("media_type") 同时作用于序列化与反序列化,
                // 故响应 JSON 字段为 media_type(与 NASA 上游一致)
                .andExpect(jsonPath("$.data.media_type").value("image"))
                .andExpect(jsonPath("$.data.url").value("https://apod.nasa.gov/image.jpg"))
                .andExpect(jsonPath("$.data.copyright").value("NASA"));

        verify(nasaApodClient, times(1)).getApod();
    }

    /**
     * 第二次调用(缓存内)→ 不调 client,返回首次缓存的数据。
     *
     * <p>即便 mock 现在被改成返回不同的值,缓存命中时实际 client 不会被调,
     * 因此响应仍是首次的 "Andromeda",且 verify 仍为 1 次。</p>
     */
    @Test
    void getApod_secondCallWithinCache_returnsCachedWithoutInvokingClient() throws Exception {
        NasaApodResponse first = new NasaApodResponse(
                "First APOD", "首次",
                "https://apod.nasa.gov/first.jpg", null,
                "image", "2026-07-07", null);
        NasaApodResponse second = new NasaApodResponse(
                "Second APOD", "第二次(不应被返回)",
                "https://apod.nasa.gov/second.jpg", null,
                "image", "2026-07-07", null);
        when(nasaApodClient.getApod()).thenReturn(first, second);

        // 首次调用 → first
        mockMvc.perform(get("/api/v1/apod"))
                .andExpect(jsonPath("$.data.title").value("First APOD"));

        // 第二次调用 → 缓存命中,仍返回 first,client 不再被调
        mockMvc.perform(get("/api/v1/apod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("First APOD"))
                .andExpect(jsonPath("$.data.url").value("https://apod.nasa.gov/first.jpg"));

        // 关键:client 仍只被调一次(缓存命中)
        verify(nasaApodClient, times(1)).getApod();
    }

    /**
     * 清缓存后调用 → client 被再次调用,返回新数据。
     */
    @Test
    void getApod_afterClearCache_invokesClientAgain() throws Exception {
        NasaApodResponse first = new NasaApodResponse(
                "First APOD", "首次",
                "https://apod.nasa.gov/first.jpg", null,
                "image", "2026-07-07", null);
        NasaApodResponse second = new NasaApodResponse(
                "Second APOD", "清缓存后",
                "https://apod.nasa.gov/second.jpg", null,
                "image", "2026-07-07", null);
        when(nasaApodClient.getApod()).thenReturn(first, second);

        // 首次 → first
        mockMvc.perform(get("/api/v1/apod"))
                .andExpect(jsonPath("$.data.title").value("First APOD"));

        // 清缓存(同包可访问包级方法)
        apodController.clearCache();

        // 第二次 → second(client 被再次调用)
        mockMvc.perform(get("/api/v1/apod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Second APOD"))
                .andExpect(jsonPath("$.data.url").value("https://apod.nasa.gov/second.jpg"));

        // client 被调用两次
        verify(nasaApodClient, times(2)).getApod();
    }
}
