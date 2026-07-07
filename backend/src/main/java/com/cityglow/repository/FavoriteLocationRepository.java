package com.cityglow.repository;

import com.cityglow.entity.FavoriteLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 收藏观测点 Repository。
 *
 * <p>派生查询方法覆盖:列出、存在性判断、按坐标查单条、按坐标删除。</p>
 */
public interface FavoriteLocationRepository extends JpaRepository<FavoriteLocation, Long> {

    /**
     * 列出某用户的全部收藏,按创建时间倒序(最新在前)。
     *
     * @param userId 用户 id
     * @return 收藏列表(可能为空)
     */
    List<FavoriteLocation> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 判断某用户是否已收藏指定坐标(幂等判断)。
     *
     * @param userId    用户 id
     * @param latitude  纬度
     * @param longitude 经度
     * @return true 已收藏;false 未收藏
     */
    boolean existsByUserIdAndLatitudeAndLongitude(Long userId, double latitude, double longitude);

    /**
     * 按用户与坐标查单条收藏(幂等 add 时返回已有记录)。
     *
     * @param userId    用户 id
     * @param latitude  纬度
     * @param longitude 经度
     * @return 已存在记录的 Optional;不存在返回 empty
     */
    Optional<FavoriteLocation> findByUserIdAndLatitudeAndLongitude(Long userId, double latitude, double longitude);

    /**
     * 按用户与坐标删除收藏。
     *
     * @param userId    用户 id
     * @param latitude  纬度
     * @param longitude 经度
     */
    void deleteByUserIdAndLatitudeAndLongitude(Long userId, double latitude, double longitude);
}
