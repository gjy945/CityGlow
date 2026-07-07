package com.cityglow.repository;

import com.cityglow.entity.ObservationLog;
import com.cityglow.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ObservationLogRepository 集成测试。
 *
 * <p>验证保存观星日志后,能按 id 取回正确的经纬度(BigDecimal 精度),
 * 并验证地图视口范围查询与时间倒序排序。</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ObservationLogRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObservationLogRepository observationLogRepository;

    @Test
    void saveAndFindById_returnsCorrectLatitudeAndLongitude() {
        // 先存 user(模拟外键关系)
        User user = new User();
        user.setUsername("observer");
        User savedUser = userRepository.save(user);

        // 再存 log
        ObservationLog log = new ObservationLog();
        log.setUserId(savedUser.getId());
        log.setLocationName("北京灵山");
        log.setLatitude(new BigDecimal("39.9890000"));
        log.setLongitude(new BigDecimal("115.4320000"));
        log.setBortleLevel(3);
        log.setDescription("银河清晰可见");

        ObservationLog savedLog = observationLogRepository.save(log);
        Optional<ObservationLog> found = observationLogRepository.findById(savedLog.getId());

        assertThat(found).isPresent();
        // BigDecimal 用 compareTo 比较,避免 scale 差异导致 equals 失败
        assertThat(found.get().getLatitude()).isEqualByComparingTo(new BigDecimal("39.9890000"));
        assertThat(found.get().getLongitude()).isEqualByComparingTo(new BigDecimal("115.4320000"));
        assertThat(found.get().getBortleLevel()).isEqualTo(3);
        assertThat(found.get().getLocationName()).isEqualTo("北京灵山");
        // @CreationTimestamp 应自动填充
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void findByLatitudeBetweenAndLongitudeBetween_returnsOnlyInViewportAndOrderByCreatedAtDesc() {
        // 准备 user
        User user = new User();
        user.setUsername("observer");
        User savedUser = userRepository.save(user);

        // 范围内日志 1: 北京(39.9, 116.4)
        ObservationLog inRange1 = new ObservationLog();
        inRange1.setUserId(savedUser.getId());
        inRange1.setLocationName("北京");
        inRange1.setLatitude(new BigDecimal("39.9000000"));
        inRange1.setLongitude(new BigDecimal("116.4000000"));
        inRange1.setBortleLevel(7);
        observationLogRepository.save(inRange1);
        // 强制刷新,确保 created_at 有差异(@CreationTimestamp 精度到秒,需 flush + 短暂等待)
        observationLogRepository.flush();

        // 范围内日志 2: 天津(39.1, 117.2)
        ObservationLog inRange2 = new ObservationLog();
        inRange2.setUserId(savedUser.getId());
        inRange2.setLocationName("天津");
        inRange2.setLatitude(new BigDecimal("39.1000000"));
        inRange2.setLongitude(new BigDecimal("117.2000000"));
        inRange2.setBortleLevel(6);
        observationLogRepository.save(inRange2);
        observationLogRepository.flush();

        // 范围外日志: 上海(31.2, 121.5),纬度超出范围
        ObservationLog outOfRange = new ObservationLog();
        outOfRange.setUserId(savedUser.getId());
        outOfRange.setLocationName("上海");
        outOfRange.setLatitude(new BigDecimal("31.2000000"));
        outOfRange.setLongitude(new BigDecimal("121.5000000"));
        outOfRange.setBortleLevel(8);
        observationLogRepository.save(outOfRange);

        // 查询视口: 纬度 [39.0, 40.0],经度 [116.0, 118.0]
        List<ObservationLog> results = observationLogRepository
                .findByLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
                        new BigDecimal("39.0"), new BigDecimal("40.0"),
                        new BigDecimal("116.0"), new BigDecimal("118.0"));

        // 应只返回北京和天津两条,上海被排除
        assertThat(results).hasSize(2);
        assertThat(results).extracting(ObservationLog::getLocationName)
                .containsExactlyInAnyOrder("北京", "天津");
        // 全部在视口范围内
        assertThat(results).allSatisfy(log -> {
            assertThat(log.getLatitude()).isBetween(new BigDecimal("39.0"), new BigDecimal("40.0"));
            assertThat(log.getLongitude()).isBetween(new BigDecimal("116.0"), new BigDecimal("118.0"));
        });
    }
}
