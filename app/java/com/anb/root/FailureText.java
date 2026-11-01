package com.anb.root;

/**
 * Classifies a failed recovery by <em>what the user has to do next</em>, instead of the
 * activity re-deriving that from Chinese substrings of the message. The message patterns
 * are the exact strings the (frozen) engine emits today; the default is UNKNOWN so an
 * unrecognised message still gets a safe, generic instruction rather than a wrong one.
 */
public final class FailureText {
    public enum Kind {
        BOOT_WAIT, CONCURRENT_RUN, REBOOTED, TIMEOUT, SHIZUKU, PERMISSION,
        COMPATIBILITY, PAYLOAD, STORAGE, PREPARE, UNKNOWN
    }

    private FailureText() { }

    public static Kind classify(String message) {
        if (message == null) return Kind.UNKNOWN;
        String m = message.trim();
        if (m.isEmpty()) return Kind.UNKNOWN;
        if (has(m, "90 秒") || has(m, "仍在启动") || has(m, "进入桌面")) return Kind.BOOT_WAIT;
        if (has(m, "另一次") || has(m, "并发") || has(m, "仍在运行")) return Kind.CONCURRENT_RUN;
        if (has(m, "已重启") || has(m, "手机已重启")) return Kind.REBOOTED;
        if (has(m, "5 分钟") || has(m, "超时") || has(m, "timed out") || has(m, "未确认完成")) return Kind.TIMEOUT;
        if (has(m, "未授予") || has(m, "权限")) return Kind.PERMISSION;
        if (has(m, "Shizuku") || has(m, "执行环境") || has(m, "执行服务") || has(m, "shell")) return Kind.SHIZUKU;
        if (has(m, "不在支持范围") || has(m, "不受支持") || has(m, "版本")) return Kind.COMPATIBILITY;
        if (has(m, "校验失败") || has(m, "内置文件") || has(m, "PAYLOAD")) return Kind.PAYLOAD;
        if (has(m, "文件目录") || has(m, "存储") || has(m, "保存")) return Kind.STORAGE;
        if (has(m, "请先") || has(m, "开机编号")) return Kind.PREPARE;
        return Kind.UNKNOWN;
    }

    /** True when the message maps to a specific cause; UNKNOWN is treated as generic. */
    public static boolean specific(Kind kind) { return kind != Kind.UNKNOWN; }

    private static boolean has(String message, String needle) { return message.contains(needle); }
}
