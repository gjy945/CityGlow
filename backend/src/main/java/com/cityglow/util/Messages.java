package com.cityglow.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

/**
 * 多语言消息源工具类(设计文档第 4 节模块 2 - 多语言支持)。
 *
 * <p>根据 HTTP {@code Accept-Language} header 解析 Locale,支持 zh/en/ja,
 * 默认 zh。其他工具类(MoonPhaseDescription / BortleEstimator / StargazingIndex)
 * 通过本类获取对应语言的描述字符串。</p>
 *
 * <p>月相、Bortle、观星三类消息各维护三种语言字表,
 * 索引由具体工具类按业务规则映射后传入。</p>
 */
public final class Messages {

    /** 默认 Locale(无法识别 Accept-Language 时使用)。 */
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    private Messages() {
        // 工具类,禁止实例化
    }

    /**
     * 从 HTTP 请求解析 Locale(支持 zh/en/ja,默认 zh)。
     *
     * @param request HTTP 请求(可能为 null,返回默认 zh)
     * @return 解析后的 Locale
     */
    public static Locale resolveLocale(HttpServletRequest request) {
        if (request == null) {
            return DEFAULT_LOCALE;
        }
        String acceptLang = request.getHeader("Accept-Language");
        return resolveFromHeader(acceptLang);
    }

    /**
     * 直接从 Accept-Language header 字符串解析 Locale。
     *
     * @param acceptLang Accept-Language header 值(如 "zh-CN,zh;q=0.9,en;q=0.8")
     * @return 解析后的 Locale(支持 zh/en/ja,默认 zh)
     */
    public static Locale resolveFromHeader(String acceptLang) {
        if (acceptLang == null || acceptLang.isBlank()) {
            return DEFAULT_LOCALE;
        }
        String lower = acceptLang.toLowerCase(Locale.ROOT);
        if (lower.startsWith("en")) {
            return Locale.ENGLISH;
        }
        if (lower.startsWith("ja")) {
            return Locale.JAPANESE;
        }
        // zh 或其他未支持语言一律回退到中文
        return DEFAULT_LOCALE;
    }

    /**
     * 月相描述:8 种月相 × 3 语言。
     *
     * <p>索引顺序:</p>
     * <ol>
     *   <li>新月(New Moon)</li>
     *   <li>蛾眉月(Waxing Crescent)</li>
     *   <li>上弦月(First Quarter)</li>
     *   <li>盈凸月(Waxing Gibbous)</li>
     *   <li>满月(Full Moon)</li>
     *   <li>亏凸月(Waning Gibbous)</li>
     *   <li>下弦月(Last Quarter)</li>
     *   <li>残月(Waning Crescent)</li>
     * </ol>
     *
     * @param phaseIndex 月相索引 0-7
     * @param locale     语言
     * @return 月相描述字符串
     */
    public static String moonPhase(int phaseIndex, Locale locale) {
        String[] zh = {"新月", "蛾眉月", "上弦月", "盈凸月", "满月", "亏凸月", "下弦月", "残月"};
        String[] en = {"New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
                "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"};
        String[] ja = {"新月", "三日月", "上弦", "十三夜月", "満月", "十六夜月", "下弦", "二十六夜月"};
        int i = Math.max(0, Math.min(7, phaseIndex));
        return pick(zh, en, ja, locale)[i];
    }

    /**
     * Bortle 等级描述(1-9 分四档)。
     *
     * <p>档位:</p>
     * <ul>
     *   <li>1 → 极佳暗空 / Excellent dark sky / 素晴らしい暗夜</li>
     *   <li>2-3 → 暗空良好 / Good dark sky / 良好な暗夜</li>
     *   <li>4-5 → 郊区天空 / Suburban sky / 郊外の空</li>
     *   <li>6-7 → 郊区 / Bright suburban / 明るい郊外</li>
     *   <li>8-9 → 城市 / City sky / 都市の空</li>
     * </ul>
     *
     * @param bortleLevel Bortle 等级 1-9
     * @param locale      语言
     * @return Bortle 描述字符串
     */
    public static String bortleDescription(int bortleLevel, Locale locale) {
        String[] zh = {"极佳暗空", "暗空良好", "郊区天空", "郊区", "城市"};
        String[] en = {"Excellent dark sky", "Good dark sky", "Suburban sky", "Bright suburban", "City sky"};
        String[] ja = {"素晴らしい暗夜", "良好な暗夜", "郊外の空", "明るい郊外", "都市の空"};
        int i = bortleIndex(bortleLevel);
        return pick(zh, en, ja, locale)[i];
    }

    /**
     * 观星指数消息(四档)。
     *
     * <p>档位:</p>
     * <ul>
     *   <li>≥80 → 今夜极佳! / Excellent tonight! / 今夜は最高!</li>
     *   <li>≥60 → 适合观星 / Good for stargazing / 観星に適す</li>
     *   <li>≥40 → 一般 / Fair / 普通</li>
     *   <li>&lt;40 → 不建议 / Not recommended / お勧めしない</li>
     * </ul>
     *
     * @param score 观星指数 0-100
     * @param locale 语言
     * @return 观星消息字符串
     */
    public static String stargazingMessage(int score, Locale locale) {
        String[] zh = {"今夜极佳!", "适合观星", "一般", "不建议"};
        String[] en = {"Excellent tonight!", "Good for stargazing", "Fair", "Not recommended"};
        String[] ja = {"今夜は最高!", "観星に適す", "普通", "お勧めしない"};
        int i = messageIndex(score);
        return pick(zh, en, ja, locale)[i];
    }

    /**
     * 根据三种语言数组与 locale 选其一。
     */
    private static String[] pick(String[] zh, String[] en, String[] ja, Locale locale) {
        if (Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) {
            return en;
        }
        if (Locale.JAPANESE.getLanguage().equals(locale.getLanguage())) {
            return ja;
        }
        return zh;
    }

    /**
     * Bortle 1-9 → 档位索引 0-4。
     */
    private static int bortleIndex(int bortleLevel) {
        if (bortleLevel <= 1) return 0;
        if (bortleLevel <= 3) return 1;
        if (bortleLevel <= 5) return 2;
        if (bortleLevel <= 7) return 3;
        return 4;
    }

    /**
     * 观星分数 → 消息索引 0-3。
     */
    private static int messageIndex(int score) {
        if (score >= 80) return 0;
        if (score >= 60) return 1;
        if (score >= 40) return 2;
        return 3;
    }
}
