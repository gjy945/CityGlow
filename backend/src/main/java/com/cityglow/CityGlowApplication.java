package com.cityglow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CityGlow 城市光影 · 夜间光污染监测与星空寻回地图
 *
 * <p>Spring Boot 3.2 + JDK 21 入口类。虚拟线程由
 * {@code spring.threads.virtual.enabled=true} 自动开启,
 * 无需额外 Bean 配置(Tomcat 将使用虚拟线程处理请求)。</p>
 */
@SpringBootApplication
public class CityGlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CityGlowApplication.class, args);
    }
}
