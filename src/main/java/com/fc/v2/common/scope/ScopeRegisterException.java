package com.fc.v2.common.scope;

/**
 * 立卡/过手续被顶回：必须说清拦在哪一栏，只甩一句「不合法」等于没说。
 *
 * @author fuce
 * @date 2026-09-27
 */
public class ScopeRegisterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 拦下的那一栏（空表示不归属具体栏的整卡顶回） */
    private final ScopeField field;

    /** 填报原值（浑数照原样驳回，追得到原笔） */
    private final String rawInput;

    public ScopeRegisterException(String message) {
        this(message, null, null);
    }

    public ScopeRegisterException(String message, ScopeField field) {
        this(message, field, null);
    }

    public ScopeRegisterException(String message, ScopeField field, String rawInput) {
        super(message);
        this.field = field;
        this.rawInput = rawInput;
    }

    public ScopeField getField() {
        return field;
    }

    public String getRawInput() {
        return rawInput;
    }
}
