package com.cityglow.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 今晚最佳观测时段推荐结果(JDK 21 Record)。
 *
 * <p>由 {@link com.cityglow.service.BestWindowService} 综合云量、月相、月升月落、
 * Bortle 暗空等级、日出日落时间计算得出,返回今晚最适合观星的时间窗口。</p>
 *
 * <p><b>暗夜窗口</b>:从天文昏影终(日落 + ~1.5 小时)到天文晨光始(日出 - ~1.5 小时)。
 * 月亮在窗口内的位置影响观测质量:月落后天空更暗,加分;月亮在窗口期间升起则扣分。</p>
 *
 * @param date      推荐日期(今晚)
 * @param startTime 最佳观测开始时间(如 21:30)
 * @param endTime   最佳观测结束时间(如 04:15,可能跨日)
 * @param score     综合评分 0-100,越高越适合观测
 * @param message   人类可读消息(如 "今晚云量较高,不适合观星" 或 "今夜极佳!")
 * @param reasons   评分原因列表(如 ["月落于 23:15","云量仅 15%","Bortle 3"])
 */
public record BestWindowResult(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int score,
        String message,
        List<String> reasons
) {
}
