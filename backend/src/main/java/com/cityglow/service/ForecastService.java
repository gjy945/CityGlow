package com.cityglow.service;

import com.cityglow.domain.ForecastResult;
import com.cityglow.domain.OpenWeatherCurrentResponse;
import com.cityglow.domain.OpenWeatherOneCallResponse;
import com.cityglow.util.BortleEstimator;
import com.cityglow.util.MoonPhaseDescription;
import com.cityglow.util.StargazingIndex;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 观星预报服务(设计文档第 4 节模块 2、第 6 节虚拟线程)。
 *
 * <p>核心职责:用 <b>JDK 21 虚拟线程</b>并行调用 OpenWeatherMap 两个端点
 * ({@code /weather} 取云量,{@code /onecall} 取日出日落月相),合并后调
 * {@link StargazingIndex} 计算观星指数,返回 {@link ForecastResult}。</p>
 *
 * <p><b>虚拟线程</b>:用 {@link Executors#newVirtualThreadPerTaskExecutor()}
 * 创建一次性 executor,两个端点调用分别提交为独立任务并行执行,
 * 相比串行调用可显著降低端到端延迟(设计文档非功能需求:< 800ms)。
 * try-with-resources 确保 executor 在返回前关闭。</p>
 *
 * <p><b>异常处理</b>:</p>
 * <ul>
 *   <li>{@link InterruptedException}:重新 interrupt 当前线程,包装为 RuntimeException 抛出。</li>
 *   <li>{@link ExecutionException}:解包 cause 后包装为 RuntimeException 抛出。</li>
 * </ul>
 *
 * <p><b>兜底逻辑</b>:OpenWeather 端点偶发返回 null(JSON 缺字段),
 * 对云量兜底 100(全阴),moonPhase/sunrise/sunset 兜底 0。</p>
 */
@Service
public class ForecastService {

    private final OpenWeatherClient openWeatherClient;

    public ForecastService(OpenWeatherClient openWeatherClient) {
        this.openWeatherClient = openWeatherClient;
    }

    /**
     * 计算给定经纬度的观星预报。
     *
     * @param lat 纬度
     * @param lng 经度
     * @return 观星预报结果(含 score/cloudCover/moonPhase/bortleLevel/message/sunrise/sunset)
     * @throws RuntimeException 若并行调用被中断或子任务抛异常
     */
    public ForecastResult forecast(double lat, double lng) {
        int bortleLevel = BortleEstimator.estimate(lat, lng);

        // 用虚拟线程并行调两个气象端点(JDK 21 特性,设计文档第 6 节明确要求)
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<OpenWeatherCurrentResponse> weatherFuture =
                    executor.submit(() -> openWeatherClient.getCurrentWeather(lat, lng));
            Future<OpenWeatherOneCallResponse> oneCallFuture =
                    executor.submit(() -> openWeatherClient.getOneCall(lat, lng));

            OpenWeatherCurrentResponse current = weatherFuture.get();
            OpenWeatherOneCallResponse oneCall = oneCallFuture.get();

            // 兜底:null 响应时云量按全阴 100,moonPhase/sunrise/sunset 按 0
            double cloudCover = current != null ? current.getCloudCover() : 100;
            double moonPhase = (oneCall != null && oneCall.current() != null)
                    ? oneCall.current().moonPhase() : 0;
            double moonIlluminatedFraction = OpenWeatherClient.toMoonIlluminatedFraction(moonPhase);
            long sunrise = (oneCall != null && oneCall.current() != null)
                    ? oneCall.current().sunrise() : 0;
            long sunset = (oneCall != null && oneCall.current() != null)
                    ? oneCall.current().sunset() : 0;

            int score = StargazingIndex.calculate(cloudCover, moonIlluminatedFraction, bortleLevel);
            String message = StargazingIndex.getMessage(score);
            String moonPhaseDesc = MoonPhaseDescription.fromPhase(moonPhase);

            return new ForecastResult(
                    score, cloudCover, moonPhaseDesc, bortleLevel, message, sunrise, sunset);
        } catch (InterruptedException e) {
            // 重新设置中断标志,遵守 Java 中断协议
            Thread.currentThread().interrupt();
            throw new RuntimeException("Forecast interrupted", e);
        } catch (ExecutionException e) {
            // 解包子任务的真正异常,避免调用方看到包装层
            throw new RuntimeException("Forecast failed", e.getCause());
        }
    }
}
