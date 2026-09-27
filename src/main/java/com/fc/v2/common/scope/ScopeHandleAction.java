package com.fc.v2.common.scope;

/**
 * 卡上两道内部手续（加上立卡 REGISTER 共三道关）。
 * 每过一道手续记一条经手，只增不改；状态只顺着走：0待核对→1已核对→2已定档。
 *
 * @author fuce
 * @date 2026-09-27
 */
public enum ScopeHandleAction {
    /** 立卡时系统记第一条经手（非页面动作） */
    REGISTER("立卡", 0),
    /** 核对 */
    CHECK("核对", 1),
    /** 定档 */
    CONFIRM("定档", 2);

    private final String label;
    private final int statusAfter;

    ScopeHandleAction(String label, int statusAfter) {
        this.label = label;
        this.statusAfter = statusAfter;
    }

    public String getLabel() {
        return label;
    }

    /** 手续走完后卡的状态：0待核对 1已核对 2已定档 */
    public int getStatusAfter() {
        return statusAfter;
    }

    /** 认得 CHECK/CONFIRM 即返；REGISTER 不经承办门（立卡时系统自动记第一条） */
    public static ScopeHandleAction require(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ScopeRegisterException("手续未指：只认 核对/定档 两道", ScopeField.HANDLE_ACTION);
        }
        String t = raw.trim();
        for (ScopeHandleAction v : values()) {
            if (v == REGISTER) {
                continue;
            }
            if (v.name().equalsIgnoreCase(t) || v.label.equals(t)) {
                return v;
            }
        }
        throw new ScopeRegisterException("手续不识：" + raw, ScopeField.HANDLE_ACTION);
    }
}
