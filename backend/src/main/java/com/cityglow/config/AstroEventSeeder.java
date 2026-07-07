package com.cityglow.config;

import com.cityglow.entity.AstroEvent;
import com.cityglow.repository.AstroEventRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 天文事件种子数据初始化器(设计文档第 4 节模块 3)。
 *
 * <p>启动时检查 astro_events 表,若为空则插入 2026 全年主要天文事件,
 * 供前端时间轴展示。已存在数据时跳过,保证幂等。</p>
 *
 * <p>用 {@code @Profile("!test")} 限制只在非测试 profile 运行,
 * 避免污染 {@code @DataJpaTest}(后者默认不加载 {@code @Component},
 * 双重保险)。不依赖 data.sql 执行机制,与 ddl-auto=update 兼容。</p>
 */
@Configuration
@Profile("!test")
public class AstroEventSeeder {

    /**
     * 启动时若表空则批量插入种子事件。
     *
     * @param repo AstroEvent 仓库
     * @return CommandLineRunner
     */
    @Bean
    CommandLineRunner seedAstroEvents(AstroEventRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;  // 已有数据则跳过,保证幂等
            }
            repo.saveAll(List.of(
                    event("象限仪座流星雨极大", LocalDateTime.of(2026, 1, 4, 3, 0),
                            "每小时天顶流星数约 80,辐射点位于牧夫座,后半夜观测最佳。月相近新月,观测条件优良。", "METEOR"),
                    event("金星西大距", LocalDateTime.of(2026, 2, 14, 19, 0),
                            "金星在太阳以西达到最大角距约 46°,日落后西方低空可见,呈弦月状相位。", "PLANET"),
                    event("日偏食", LocalDateTime.of(2026, 3, 17, 12, 30),
                            "2026 年首次日偏食,中国境内不可见,主要见于北大西洋与西伯利亚。", "ECLIPSE"),
                    event("天琴座流星雨极大", LocalDateTime.of(2026, 4, 22, 14, 0),
                            "中等强度流星雨,天顶流星数约 18,辐射点位于织女星附近,4 月下旬后半夜观测。", "METEOR"),
                    event("宝瓶座 η 流星雨极大", LocalDateTime.of(2026, 5, 6, 10, 0),
                            "哈雷彗星遗留流星雨,天顶流星数约 50,南半球更佳,北半球黎明前可见。", "METEOR"),
                    event("土星冲日", LocalDateTime.of(2026, 7, 21, 0, 0),
                            "土星与太阳黄经相差 180°,整夜可见,环系倾角适中,望远镜观测最佳时机。", "PLANET"),
                    event("英仙座流星雨极大", LocalDateTime.of(2026, 8, 13, 3, 0),
                            "北半球三大流星雨之一,天顶流星数约 100,夏季夜空观测热门,月光影响小。", "METEOR"),
                    event("月偏食", LocalDateTime.of(2026, 8, 28, 4, 13),
                            "2026 年唯一可见月食,中国境内可见偏食阶段,最大食分约 0.93。", "ECLIPSE"),
                    event("木星冲日", LocalDateTime.of(2026, 9, 19, 0, 0),
                            "木星与太阳黄经相差 180°,视星等约 -2.9,整夜可见,伽利略卫星观测良机。", "PLANET"),
                    event("猎户座流星雨极大", LocalDateTime.of(2026, 10, 21, 12, 0),
                            "哈雷彗星遗留流星雨,天顶流星数约 20,辐射点位于猎户座 γ 附近,10 月下旬后半夜观测。", "METEOR"),
                    event("金星东大距", LocalDateTime.of(2026, 11, 13, 0, 0),
                            "金星在太阳以东达到最大角距约 47°,日落后西方天空可见,呈弦月状相位。", "PLANET"),
                    event("狮子座流星雨极大", LocalDateTime.of(2026, 11, 17, 8, 0),
                            "坦普尔-塔特尔彗星遗留流星雨,天顶流星数约 15,辐射点位于狮子座,11 月中旬后半夜观测。", "METEOR"),
                    event("双子座流星雨极大", LocalDateTime.of(2026, 12, 14, 9, 30),
                            "北半球三大流星雨之一,天顶流星数约 120,流星明亮且速度较慢,年度最佳观测目标。", "METEOR")
            ));
        };
    }

    /**
     * 私有工厂方法,构造一个 AstroEvent 实例。
     */
    private AstroEvent event(String title, LocalDateTime time, String description, String eventType) {
        AstroEvent e = new AstroEvent();
        e.setTitle(title);
        e.setEventTime(time);
        e.setDescription(description);
        e.setEventType(eventType);
        return e;
    }
}
