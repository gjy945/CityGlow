package com.cityglow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上下文加载冒烟测试:验证 Spring ApplicationContext 能成功启动,
 * 且核心 Bean(DataSource)可用。
 *
 * <p>使用 test profile(H2 内存库),不依赖外部 MySQL。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class CityGlowApplicationTests {

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        // 验证数据源 Bean 可解析,确保 JPA + 数据源链路就绪
        assertThat(applicationContext.getBean(DataSource.class)).isNotNull();
    }
}
