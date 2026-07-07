package com.cityglow.domain;

/**
 * 星座神话故事卡。
 *
 * @param constellation 星座英文标识(如 "orion")
 * @param culture       文化:"greek" 或 "chinese"
 * @param title         故事标题
 * @param story         故事正文(80-150 字)
 */
public record MythCard(
        String constellation,
        String culture,
        String title,
        String story
) {
}
