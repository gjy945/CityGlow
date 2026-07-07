package com.cityglow.service;

import com.cityglow.domain.FavoriteRequest;
import com.cityglow.domain.FavoriteResponse;
import com.cityglow.entity.FavoriteLocation;
import com.cityglow.repository.FavoriteLocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FavoriteService 单元测试。
 *
 * <p>用 Mockito mock {@link FavoriteLocationRepository},不真实写库。</p>
 *
 * <p>覆盖:</p>
 * <ul>
 *   <li>list:返回某用户全部收藏(倒序映射为 FavoriteResponse)</li>
 *   <li>add 新坐标:不存在 → 新建并保存</li>
 *   <li>add 幂等:已存在相同坐标 → 返回已有记录,不重复保存</li>
 *   <li>remove:委托 repository 按坐标删除</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteLocationRepository favoriteRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void list_returnsUserFavoritesAsResponses() {
        // given:用户 1 有两条收藏(倒序)
        FavoriteLocation first = buildFavorite(2L, 1L, "天津", 39.1, 117.2);
        FavoriteLocation second = buildFavorite(1L, 1L, "北京灵山", 39.9, 116.4);
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(first, second));

        // when
        List<FavoriteResponse> result = favoriteService.list(1L);

        // then:映射为 FavoriteResponse,顺序保持
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(2L);
        assertThat(result.get(0).name()).isEqualTo("天津");
        assertThat(result.get(1).id()).isEqualTo(1L);
        assertThat(result.get(1).name()).isEqualTo("北京灵山");
    }

    @Test
    void list_emptyWhenUserHasNoFavorites() {
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        List<FavoriteResponse> result = favoriteService.list(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void add_newCoordinates_savesAndReturnsResponse() {
        // given:坐标未收藏
        when(favoriteRepository.findByUserIdAndLatitudeAndLongitude(1L, 39.9, 116.4))
                .thenReturn(Optional.empty());
        // save 时模拟自增主键
        FavoriteLocation saved = buildFavorite(1L, 1L, "北京灵山", 39.9, 116.4);
        when(favoriteRepository.save(any(FavoriteLocation.class))).thenReturn(saved);

        // when
        FavoriteResponse result = favoriteService.add(1L,
                new FavoriteRequest("北京灵山", 39.9, 116.4));

        // then:返回保存后的记录(含 id),save 被调用一次
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("北京灵山");
        assertThat(result.latitude()).isEqualTo(39.9);
        assertThat(result.longitude()).isEqualTo(116.4);
        verify(favoriteRepository, times(1)).save(any(FavoriteLocation.class));
    }

    @Test
    void add_existingCoordinates_returnsExistingWithoutSave() {
        // given:相同坐标已存在(幂等)
        FavoriteLocation existing = buildFavorite(5L, 1L, "旧名称", 39.9, 116.4);
        when(favoriteRepository.findByUserIdAndLatitudeAndLongitude(1L, 39.9, 116.4))
                .thenReturn(Optional.of(existing));

        // when:即便请求体 name 不同,也返回已有记录(按坐标幂等)
        FavoriteResponse result = favoriteService.add(1L,
                new FavoriteRequest("新名称", 39.9, 116.4));

        // then:返回已有记录 id=5,不触发 save
        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.name()).isEqualTo("旧名称");
        verify(favoriteRepository, never()).save(any(FavoriteLocation.class));
    }

    @Test
    void remove_delegatesToRepositoryByCoordinates() {
        // when
        favoriteService.remove(1L, 39.9, 116.4);

        // then:委托 repository 按用户+坐标删除
        verify(favoriteRepository, times(1))
                .deleteByUserIdAndLatitudeAndLongitude(eq(1L), anyDouble(), anyDouble());
    }

    /**
     * 构造测试用 FavoriteLocation(模拟持久化后状态,id 非空)。
     */
    private FavoriteLocation buildFavorite(Long id, Long userId, String name,
                                           double latitude, double longitude) {
        FavoriteLocation entity = new FavoriteLocation();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setName(name);
        entity.setLatitude(latitude);
        entity.setLongitude(longitude);
        entity.setCreatedAt(LocalDateTime.of(2026, 7, 7, 22, 30));
        return entity;
    }
}
