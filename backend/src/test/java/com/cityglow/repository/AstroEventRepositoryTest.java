package com.cityglow.repository;

import com.cityglow.entity.AstroEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AstroEventRepository 集成测试。
 *
 * <p>验证 findByEventTimeAfterOrderByEventTimeAsc 只返回未来事件,且按时间升序排列。</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class AstroEventRepositoryTest {

    @Autowired
    private AstroEventRepository astroEventRepository;

    @Test
    void findByEventTimeAfterOrderByEventTimeAsc_returnsOnlyFutureEventsSortedAsc() {
        // 存 3 个事件:过去 / 现在 / 未来
        LocalDateTime past = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime now = LocalDateTime.of(2026, 7, 7, 12, 0);
        LocalDateTime future1 = LocalDateTime.of(2026, 8, 12, 3, 0);
        LocalDateTime future2 = LocalDateTime.of(2026, 12, 14, 9, 30);

        AstroEvent pastEvent = new AstroEvent();
        pastEvent.setTitle("英仙座流星雨(已过)");
        pastEvent.setEventTime(past);
        pastEvent.setEventType("METEOR");

        AstroEvent nowEvent = new AstroEvent();
        nowEvent.setTitle("今日天象");
        nowEvent.setEventTime(now);
        nowEvent.setEventType("PLANET");

        AstroEvent futureEvent1 = new AstroEvent();
        futureEvent1.setTitle("英仙座流星雨极大");
        futureEvent1.setEventTime(future1);
        futureEvent1.setEventType("METEOR");

        AstroEvent futureEvent2 = new AstroEvent();
        futureEvent2.setTitle("双子座流星雨");
        futureEvent2.setEventTime(future2);
        futureEvent2.setEventType("METEOR");

        astroEventRepository.save(pastEvent);
        astroEventRepository.save(nowEvent);
        astroEventRepository.save(futureEvent1);
        astroEventRepository.save(futureEvent2);

        // 查询 now 之后的事件:严格大于 now,应只返回 future1 和 future2,升序
        List<AstroEvent> result = astroEventRepository
                .findByEventTimeAfterOrderByEventTimeAsc(now);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AstroEvent::getTitle)
                .containsExactly("英仙座流星雨极大", "双子座流星雨");
        assertThat(result.get(0).getEventTime()).isEqualTo(future1);
        assertThat(result.get(1).getEventTime()).isEqualTo(future2);
    }

    @Test
    void findByEventType_returnsOnlyMatchingType() {
        AstroEvent meteor = new AstroEvent();
        meteor.setTitle("流星雨");
        meteor.setEventTime(LocalDateTime.of(2026, 8, 12, 3, 0));
        meteor.setEventType("METEOR");

        AstroEvent eclipse = new AstroEvent();
        eclipse.setTitle("月食");
        eclipse.setEventTime(LocalDateTime.of(2026, 9, 7, 2, 0));
        eclipse.setEventType("ECLIPSE");

        astroEventRepository.save(meteor);
        astroEventRepository.save(eclipse);

        List<AstroEvent> meteors = astroEventRepository.findByEventType("METEOR");

        assertThat(meteors).hasSize(1);
        assertThat(meteors.get(0).getEventType()).isEqualTo("METEOR");
    }
}
