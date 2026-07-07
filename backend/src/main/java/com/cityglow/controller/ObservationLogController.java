package com.cityglow.controller;

import com.cityglow.domain.ApiResponse;
import com.cityglow.domain.PostcardResult;
import com.cityglow.entity.ObservationLog;
import com.cityglow.repository.ObservationLogRepository;
import com.cityglow.service.PostcardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 观星日志 REST 接口(设计文档第 4 节模块 4、第 5.1 节接口契约)。
 *
 * <p>暴露:</p>
 * <ul>
 *   <li>{@code POST /api/v1/logs/upload} (multipart):上传图片 + 经纬度 + 描述,
 *       委托 {@link PostcardService} 用结构化并发生成明信片,写 observation_logs 表,
 *       存盘 {@code uploads/<logId>.jpg},返回 {@code {logId, cardUrl}}。</li>
 *   <li>{@code GET /api/v1/logs}:列出全部日志,按 created_at 倒序。</li>
 *   <li>{@code GET /api/v1/logs/{id}}:按主键取单条,不存在返回 404 业务码。</li>
 * </ul>
 *
 * <p><b>MIME 校验</b>:仅允许 {@code image/jpeg}、{@code image/png},
 * 其余返回 {@code ApiResponse.error(400, "Only JPEG/PNG allowed")}。</p>
 *
 * <p><b>用户</b>:当前无登录态,userId 暂留 null(后续接入认证时补充)。</p>
 */
@RestController
@RequestMapping("/api/v1/logs")
public class ObservationLogController {

    private final PostcardService postcardService;
    private final ObservationLogRepository logRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public ObservationLogController(PostcardService postcardService,
                                    ObservationLogRepository logRepository) {
        this.postcardService = postcardService;
        this.logRepository = logRepository;
    }

    /**
     * 上传观星图片并生成明信片。
     *
     * @param image         图片文件(multipart,"image" 字段)
     * @param lat           纬度
     * @param lng           经度
     * @param locationName  地点名称(可选)
     * @param description   描述(可选)
     * @return 成功:{code:200, data:{logId, cardUrl}};MIME 非法:{code:400}
     * @throws IOException 文件落盘失败时由 Spring 转为 500
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("image") MultipartFile image,
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(value = "locationName", required = false) String locationName,
            @RequestParam(value = "description", required = false) String description) throws IOException {

        String contentType = image.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            return ApiResponse.error(400, "Only JPEG/PNG allowed");
        }

        // 结构化并发生成明信片(解码压缩 ‖ 月相/Bortle 元数据 → 画水印 → 编码)
        PostcardResult result = postcardService.generate(
                image.getBytes(), lat, lng, locationName, description);

        // 先存日志拿自增 id,再用 id 作为文件名
        ObservationLog log = new ObservationLog();
        log.setLocationName(locationName);
        log.setLatitude(BigDecimal.valueOf(lat));
        log.setLongitude(BigDecimal.valueOf(lng));
        log.setBortleLevel(result.watermarkInfo().bortleLevel());
        log.setDescription(description);
        ObservationLog saved = logRepository.save(log);

        // 落盘 uploads/<id>.jpg
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        String filename = saved.getId() + ".jpg";
        Files.write(uploadPath.resolve(filename), result.jpegBytes());

        // 回填 imageUrl 后再次保存
        saved.setImageUrl("/uploads/" + filename);
        logRepository.save(saved);

        return ApiResponse.success(Map.of(
                "logId", saved.getId(),
                "cardUrl", saved.getImageUrl()
        ));
    }

    /**
     * 列出全部观星日志,按 created_at 倒序(最新在前)。
     *
     * @return 日志列表
     */
    @GetMapping
    public ApiResponse<List<ObservationLog>> list() {
        return ApiResponse.success(
                logRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * 按主键取单条日志。
     *
     * @param id 主键
     * @return 200 + 日志,或 404 业务码(data=null)
     */
    @GetMapping("/{id}")
    public ApiResponse<ObservationLog> getById(@PathVariable Long id) {
        return logRepository.findById(id)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.error(404, "Log not found"));
    }
}
