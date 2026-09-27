package com.fc.v2.common.scope;

import java.math.BigDecimal;

/**
 * 层级档折算结果：挂用类别数撞上档案三道顶线，落在哪一档、系数几何。
 *
 * <p>等号算高一档：恰为一档顶线按二档说话，恰为二档顶线按三档说话；
 * 越过三档顶线由校验那一步拦下，不设四档。系数 = 挂用类别数 ÷ 所在档
 * 顶线，四舍五入留两位小数。</p>
 *
 * @author fuce
 * @date 2026-09-27
 */
public final class ScopeGrade {

    /** 层级档 1/2/3 */
    private final int level;

    /** 层级档系数（挂用类别数 ÷ 所在档顶线，两位小数） */
    private final BigDecimal score;

    /** 所在档顶线（项） */
    private final BigDecimal ceiling;

    public ScopeGrade(int level, BigDecimal score, BigDecimal ceiling) {
        this.level = level;
        this.score = score;
        this.ceiling = ceiling;
    }

    public int getLevel() {
        return level;
    }

    public BigDecimal getScore() {
        return score;
    }

    public BigDecimal getCeiling() {
        return ceiling;
    }
}
