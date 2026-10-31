package com.anb.root;

/** Pins the classifier to the messages the engine actually emits, so guidance cannot silently drift. */
public final class FailureTextTest {
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); }

    static void expect(FailureText.Kind expected, String message) {
        FailureText.Kind actual = FailureText.classify(message);
        check(actual == expected, "expected " + expected + " for: " + message + " (got " + actual + ")");
    }

    public static void main(String[] args) {
        expect(FailureText.Kind.BOOT_WAIT, "请等待开机满 90 秒后再恢复；约还需 12 秒。");
        expect(FailureText.Kind.BOOT_WAIT, "系统仍在启动，请进入桌面后再试。");
        expect(FailureText.Kind.CONCURRENT_RUN, "另一次恢复仍在运行。请等待；不要并发启动。");
        expect(FailureText.Kind.TIMEOUT, "等待超过 5 分钟，未确认完成。记录已保留；本次开机不会自动重跑，请重启后再试。");
        expect(FailureText.Kind.REBOOTED, "手机已重启。请重新启动 Shizuku，然后返回检查。");
        expect(FailureText.Kind.SHIZUKU, "未能连接执行服务，请重新启动 Shizuku 后重试");
        expect(FailureText.Kind.SHIZUKU, "执行环境尚未就绪");
        expect(FailureText.Kind.PERMISSION, "尚未授予 Shizuku 权限");
        expect(FailureText.Kind.COMPATIBILITY, "当前设备或系统版本不在支持范围内。");
        expect(FailureText.Kind.PAYLOAD, "内置文件校验失败：exploit");
        expect(FailureText.Kind.STORAGE, "无法准备文件目录。");
        expect(FailureText.Kind.PREPARE, "请先完成依赖检查。");
        expect(FailureText.Kind.PREPARE, "无法确认本次开机编号。");
        expect(FailureText.Kind.UNKNOWN, "某个尚未归类的新消息");
        expect(FailureText.Kind.UNKNOWN, "");
        expect(FailureText.Kind.UNKNOWN, "   ");
        expect(FailureText.Kind.UNKNOWN, null);

        check(!FailureText.specific(FailureText.Kind.UNKNOWN), "unknown is not specific");
        check(FailureText.specific(FailureText.Kind.TIMEOUT), "timeout is specific");
        System.out.println("PASS: 17 message classifications, timeout before reboot, unknown stays generic");
    }
}
