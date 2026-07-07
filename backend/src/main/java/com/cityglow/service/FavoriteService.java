package com.cityglow.service;

import com.cityglow.domain.FavoriteRequest;
import com.cityglow.domain.FavoriteResponse;
import com.cityglow.entity.FavoriteLocation;
import com.cityglow.repository.FavoriteLocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 收藏观测点服务。
 *
 * <p>职责:</p>
 * <ul>
 *   <li>{@link #list}:列出某用户的全部收藏(按 created_at 倒序)</li>
 *   <li>{@link #add}:添加收藏,若已存在相同坐标则返回已有记录(幂等),否则新建</li>
 *   <li>{@link #remove}:按坐标删除收藏</li>
 * </ul>
 *
 * <p>所有异常一律包装为 RuntimeException(避免方法签名 throws 污染)。</p>
 */
@Service
public class FavoriteService {

    private final FavoriteLocationRepository favoriteRepository;

    public FavoriteService(FavoriteLocationRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    /**
     * 列出某用户的全部收藏,按 created_at 倒序。
     *
     * @param userId 用户 id
     * @return 收藏响应列表(可能为空)
     */
    public List<FavoriteResponse> list(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    /**
     * 添加收藏(幂等):若该用户已收藏相同坐标,返回已有记录;否则新建并返回。
     *
     * @param userId  用户 id
     * @param request {name, latitude, longitude}
     * @return 收藏响应
     */
    public FavoriteResponse add(Long userId, FavoriteRequest request) {
        // 幂等:相同坐标已存在则返回已有记录
        return favoriteRepository
                .findByUserIdAndLatitudeAndLongitude(userId, request.latitude(), request.longitude())
                .map(FavoriteResponse::from)
                .orElseGet(() -> {
                    FavoriteLocation entity = new FavoriteLocation();
                    entity.setUserId(userId);
                    entity.setName(request.name());
                    entity.setLatitude(request.latitude());
                    entity.setLongitude(request.longitude());
                    FavoriteLocation saved = favoriteRepository.save(entity);
                    return FavoriteResponse.from(saved);
                });
    }

    /**
     * 按坐标删除收藏(不存在时静默忽略,保证幂等)。
     *
     * @param userId    用户 id
     * @param latitude  纬度
     * @param longitude 经度
     */
    public void remove(Long userId, double latitude, double longitude) {
        favoriteRepository.deleteByUserIdAndLatitudeAndLongitude(userId, latitude, longitude);
    }
}
