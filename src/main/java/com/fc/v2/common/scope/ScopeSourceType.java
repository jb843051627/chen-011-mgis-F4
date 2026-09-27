package com.fc.v2.common.scope;

/**
 * 卡的出处：市局抄送、单位自报。两边常常是同一件事，并在同一张卡上，
 * 账上一张卡每类出处至多一条——两条并成一条，不许留两笔。
 *
 * @author fuce
 * @date 2026-09-27
 */
public enum ScopeSourceType {
    /** 市局抄送 */
    CITY_COPY("市局抄送"),
    /** 单位自报 */
    SELF_REPORT("单位自报");

    private final String label;

    ScopeSourceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 认得即返；不识照 null，由调用处指栏顶回，不擅自归类 */
    public static ScopeSourceType tryOf(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        for (ScopeSourceType v : values()) {
            if (v.name().equalsIgnoreCase(t) || v.label.equals(t)) {
                return v;
            }
        }
        return null;
    }
}
