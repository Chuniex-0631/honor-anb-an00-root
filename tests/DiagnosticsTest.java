package com.anb.root;

/** Pins the exported diagnostics header, including the redaction-free but honest "unknown" fallback. */
public final class DiagnosticsTest {
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); }

    public static void main(String[] args) {
        String text = Diagnostics.build("2.3.0", 7, "ANB-AN00", "5.10.236-test", "FAILED", "42",
                "2026-11-01 10:00:00", "12:00:00  开始\n12:00:05  失败\n");
        check(text.startsWith("ANB Root 诊断记录\n"), "title first");
        check(text.contains("版本: 2.3.0 (7)\n"), "version with code");
        check(text.contains("机型: ANB-AN00\n"), "model");
        check(text.contains("内核: 5.10.236-test\n"), "kernel");
        check(text.contains("阶段: FAILED\n"), "phase");
        check(text.contains("开机编号: 42\n"), "boot count");
        check(text.contains("生成时间: 2026-11-01 10:00:00\n"), "timestamp");
        check(text.contains("---- 运行记录 ----\n"), "log separator");
        check(text.endsWith("12:00:05  失败\n"), "log preserved verbatim");
        check(text.indexOf("版本:") < text.indexOf("---- 运行记录 ----"), "header precedes log");

        String sparse = Diagnostics.build(null, 0, null, null, null, "", null, null);
        check(sparse.contains("版本: 未知 (0)\n"), "missing version is explicit");
        check(sparse.contains("机型: 未知\n"), "missing model is explicit");
        check(sparse.endsWith("\n"), "output always ends with a newline");

        String noTrailing = Diagnostics.build("2.3.0", 7, "ANB-AN00", "k", "IDLE", "1", "t", "no newline");
        check(noTrailing.endsWith("no newline\n"), "adds the missing final newline");

        check("未知".equals(Diagnostics.value("  ")), "blank is unknown");
        check("x".equals(Diagnostics.value(" x ")), "trimmed");
        System.out.println("PASS: header order, optional fields, verbatim log, newline normalization");
    }
}
