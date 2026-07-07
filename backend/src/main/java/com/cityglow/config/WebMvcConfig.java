package com.cityglow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置:静态资源映射(设计文档第 4 节模块 4)。
 *
 * <p>将 {@code /uploads/**} URL 前缀映射到本地文件系统上传目录,
 * 使前端可通过 {@code /uploads/<logId>.jpg} 访问生成的明信片图片。</p>
 *
 * <p>上传目录由 {@code app.upload.dir} 配置(默认 {@code uploads},
 * 相对工作目录)。Spring Boot 启动时 {@code file:} 协议定位绝对路径。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + Paths.get(uploadDir).toAbsolutePath() + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
