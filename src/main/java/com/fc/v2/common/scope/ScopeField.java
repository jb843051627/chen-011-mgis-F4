package com.fc.v2.common.scope;

/**
 * 立卡被拦时指向的栏目。顶回必须指栏——只甩一句「不合法」等于没说。
 *
 * @author fuce
 * @date 2026-09-27
 */
public enum ScopeField {
    /** 资质单位编号 */
    UNIT_NO("单位编号"),
    /** 单位名称（只按名称匹配不作数） */
    UNIT_NAME("单位名称"),
    /** 出处类型 抄送/自报 */
    SOURCE_TYPE("出处"),
    /** 来文/自报单号 */
    SOURCE_DOC_NO("出处单号"),
    /** 挂用类别列表 */
    CATEGORIES("挂用类别"),
    /** 类别代号 */
    CAT_CODE("类别代号"),
    /** 挂用类别数 */
    SCOPE_COUNT("挂用类别数"),
    /** 层级档（不许手填） */
    GRADE_LEVEL("层级档"),
    /** 卡上手续（核对/定档） */
    HANDLE_ACTION("卡上手续"),
    /** 卡号（系统给号） */
    CARD_NO("CARD_NO");

    private final String label;

    ScopeField(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
