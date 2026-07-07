package com.cityglow.service;

import com.cityglow.domain.MythCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConstellationDataService 单元测试。
 *
 * <p>直接 new 实例化并调用 load() 触发加载(不走 Spring 容器),验证:</p>
 * <ul>
 *   <li>启动加载:3 个 JSON 文件正确加载到内存</li>
 *   <li>星座查询:12 星座可按 name 获取,latin/chinese 字段正确</li>
 *   <li>亮星查询:按 HIP 单个/批量获取,未知 HIP 返回 empty</li>
 *   <li>神话查询:按 constellation+culture 获取,每星座 2 篇(希腊 + 中国)</li>
 * </ul>
 */
class ConstellationDataServiceTest {

    private ConstellationDataService service;

    @BeforeEach
    void setUp() {
        service = new ConstellationDataService();
        service.load();
    }

    @Test
    void load_startsSuccessfully() {
        assertThat(service).isNotNull();
        assertThat(service.getAllConstellationNames()).hasSize(12);
    }

    @Test
    void getAllConstellationNames_returns12Constellations() {
        assertThat(service.getAllConstellationNames()).hasSize(12);
    }

    @Test
    void getConstellation_orion_returnsData() {
        var opt = service.getConstellation("orion");

        assertThat(opt).isPresent();
        var orion = opt.get();
        assertThat(orion.latin()).isEqualTo("Orion");
        assertThat(orion.chinese()).isEqualTo("猎户座");
        assertThat(orion.starHips()).isNotEmpty();
        assertThat(orion.lines()).isNotEmpty();
    }

    @Test
    void getConstellation_unknownName_returnsEmpty() {
        assertThat(service.getConstellation("nonexistent")).isEmpty();
    }

    @Test
    void getStar_knownHip_returnsStar() {
        var opt = service.getStar(27989);

        assertThat(opt).isPresent();
        var star = opt.get();
        assertThat(star.hip()).isEqualTo(27989);
        assertThat(star.mag()).isEqualTo(0.5);
        assertThat(star.name()).contains("Betelgeuse");
    }

    @Test
    void getStar_unknownHip_returnsEmpty() {
        assertThat(service.getStar(999999)).isEmpty();
    }

    @Test
    void getMyth_orionGreek_returnsStory() {
        var opt = service.getMyth("orion", "greek");

        assertThat(opt).isPresent();
        MythCard card = opt.get();
        assertThat(card.title()).isNotBlank();
        assertThat(card.story().length()).isBetween(80, 200);
    }

    @Test
    void getMyth_orionChinese_returnsStory() {
        var opt = service.getMyth("orion", "chinese");

        assertThat(opt).isPresent();
        MythCard card = opt.get();
        assertThat(card.title()).isNotBlank();
        assertThat(card.story()).isNotBlank();
    }

    @Test
    void getMyth_unknownConstellation_returnsEmpty() {
        assertThat(service.getMyth("nonexistent", "greek")).isEmpty();
    }

    @Test
    void getMyths_orion_returns2Cards() {
        List<MythCard> cards = service.getMyths("orion");

        assertThat(cards).hasSize(2);
        assertThat(cards).extracting(MythCard::culture)
                .containsExactlyInAnyOrder("greek", "chinese");
    }

    @Test
    void getStars_batchByHips_returnsList() {
        List<ConstellationDataService.StarRecord> stars =
                service.getStars(List.of(27989, 24436));

        assertThat(stars).hasSize(2);
        assertThat(stars).extracting(s -> s.hip())
                .containsExactlyInAnyOrder(27989, 24436);
    }

    @Test
    void getStars_partialMatch_returnsOnlyFound() {
        List<ConstellationDataService.StarRecord> stars =
                service.getStars(List.of(27989, 999999));

        assertThat(stars).hasSize(1);
        assertThat(stars.get(0).hip()).isEqualTo(27989);
    }
}
