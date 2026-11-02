package com.anb.root;

/**
 * Builds the exported diagnostics file. Plain string assembly, so the header format is
 * unit tested; the activity only supplies the values.
 */
public final class Diagnostics {
    private Diagnostics() { }

    public static String build(String version, int versionCode, String model, String kernel,
                               String phase, String boot, String timestamp, String log) {
        StringBuilder out = new StringBuilder(512);
        out.append("ANB Root 诊断记录\n");
        out.append("生成时间: ").append(value(timestamp)).append('\n');
        out.append("版本: ").append(value(version)).append(" (").append(versionCode).append(")\n");
        out.append("机型: ").append(value(model)).append('\n');
        out.append("内核: ").append(value(kernel)).append('\n');
        out.append("阶段: ").append(value(phase)).append('\n');
        out.append("开机编号: ").append(value(boot)).append('\n');
        out.append("说明: 由你选择保存位置；内容可能含设备型号与运行信息，分享前请检查。\n");
        out.append("---- 运行记录 ----\n");
        if (log != null) out.append(log);
        if (out.length() == 0 || out.charAt(out.length() - 1) != '\n') out.append('\n');
        return out.toString();
    }

    static String value(String text) {
        if (text == null || text.trim().isEmpty()) return "未知";
        return text.trim();
    }
}
