package com.cityglow.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MythCard record 单元测试。
 *
 * <p>验证:</p>
 * <ul>
 *   <li>accessor 方法返回构造时传入的值</li>
 *   <li>record 自带的 equals/hashCode 行为</li>
 * </ul>
 */
class MythCardTest {

    @Test
    void accessors_returnConstructorValues() {
        MythCard card = new MythCard(
                "orion",
                "greek",
                "猎人与蝎子",
                "俄里翁是希腊神话中的伟大猎人,因夸口能猎尽天下野兽而触怒大地女神,被巨蝎刺死。"
        );

        assertThat(card.constellation()).isEqualTo("orion");
        assertThat(card.culture()).isEqualTo("greek");
        assertThat(card.title()).isEqualTo("猎人与蝎子");
        assertThat(card.story()).startsWith("俄里翁");
    }

    @Test
    void equalsAndHashCode_followRecordSemantics() {
        MythCard a = new MythCard("orion", "greek", "title", "story");
        MythCard b = new MythCard("orion", "greek", "title", "story");
        MythCard c = new MythCard("orion", "chinese", "title", "story");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }
}
