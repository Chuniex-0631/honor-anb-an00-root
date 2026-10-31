package com.anb.root;

/**
 * Pure, deterministic presentation state for the three-tab recovery UI.
 * No Android dependencies: JVM tests cover precedence and CTA safety.
 */
public final class UiState {
    public enum Action {
        NONE, GET_SHIZUKU, GET_KSU, OPEN_SHIZUKU, GRANT_SHIZUKU,
        CHECK_ENVIRONMENT, COMPLETE_SETUP, RECOVER_OR_VERIFY
    }
    public enum Tone { NEUTRAL, GOOD, WARNING }

    public final String title, message, badge, primaryLabel;
    public final Action action;
    public final Tone tone;

    private UiState(String title, String message, String badge,
                    String primaryLabel, Action action, Tone tone) {
        this.title=title;
        this.message=message;
        this.badge=badge;
        this.primaryLabel=primaryLabel;
        this.action=action;
        this.tone=tone;
    }

    public boolean canAct() { return action!=Action.NONE; }

    private static UiState state(String title,String message,String badge,
                                 String primary,Action action,Tone tone) {
        return new UiState(title,message,badge,primary,action,tone);
    }

    public static UiState resolve(Readiness.Gate gate,boolean setupComplete,
                                  boolean checking,boolean busy,boolean modulePresent,
                                  String phase,String operationTitle,String operationMessage) {
        // An unsupported device must never receive a recovery action.
        if(gate==Readiness.Gate.DEVICE)
            return state("设备不受支持","仅支持已验证固件的 ANB-AN00，不能使用其他机型或系统的载荷。",
                    "不兼容","当前设备不可用",Action.NONE,Tone.WARNING);
        if(busy)
            return state(nonempty(operationTitle,"正在处理"),
                    nonempty(operationMessage,"恢复任务正在进行；不要重复启动。"),
                    "进行中","正在处理",Action.NONE,Tone.NEUTRAL);
        if(checking)
            return state("正在检查环境","正在确认 Shizuku 执行身份和设备条件。",
                    "检查中","正在检查",Action.NONE,Tone.NEUTRAL);
        switch(gate) {
            case SHIZUKU_INSTALL:
                return state("需要 Shizuku","安装官方 Shizuku 并通过无线调试启动。",
                        "待准备","下载 Shizuku",Action.GET_SHIZUKU,Tone.WARNING);
            case KSU_INSTALL:
                return state("需要 KernelSU","安装受支持的 KernelSU v3.3.0 管理器。",
                        "待准备","下载 KernelSU",Action.GET_KSU,Tone.WARNING);
            case SHIZUKU_START:
                return state("启动 Shizuku","在 Shizuku 中通过无线调试启动服务，每次完整重启后都需要重新启动。",
                        "待连接","打开 Shizuku",Action.OPEN_SHIZUKU,Tone.WARNING);
            case SHIZUKU_VERSION:
                return state("更新 Shizuku","需要 Shizuku 13 或更新版本。",
                        "待更新","更新 Shizuku",Action.GET_SHIZUKU,Tone.WARNING);
            case SHIZUKU_PERMISSION:
                return state("授予 Shizuku 权限","允许 ANB Root 使用 Shizuku 执行服务。",
                        "待授权","授予权限",Action.GRANT_SHIZUKU,Tone.WARNING);
            case SHELL_MODE:
                return state("切换为无线调试模式","停止当前 Shizuku 服务，再使用无线调试以 shell 身份启动。",
                        "模式不符","打开 Shizuku",Action.OPEN_SHIZUKU,Tone.WARNING);
            case ENVIRONMENT:
                return state("检查执行环境","先核验 shell 身份、Seccomp 和实际固件，再进行恢复。",
                        "待检查","检查环境",Action.CHECK_ENVIRONMENT,Tone.NEUTRAL);
            case READY: break;
            default:
                return state("条件尚未满足","请先完成设备和执行环境检查。",
                        "待准备","暂不可用",Action.NONE,Tone.WARNING);
        }
        // Do not display a stale SUCCESS when Shizuku or device gates have failed.
        if(!setupComplete)
            return state("完成首次准备","已满足设备、Shizuku 与执行环境条件，确认后即可恢复。",
                    "初次使用","确认准备完成",Action.COMPLETE_SETUP,Tone.NEUTRAL);
        if("SUCCESS".equals(phase))
            return state("Root 已验证","本次开机 Root 已验证；重启后需要重新恢复。",
                    "工作中","重新验证",Action.RECOVER_OR_VERIFY,Tone.GOOD);
        if("WAIT_AUTH".equals(phase))
            return state("等待 Root 授权","在 KernelSU 中允许 ANB Root 使用 Root，再返回验证。",
                    "待授权","验证 Root 授权",Action.RECOVER_OR_VERIFY,Tone.WARNING);
        if("FAILED".equals(phase))
            return state("尚未确认恢复成功","请先保存诊断、重新检查环境；此处不会自动重新启动提权。",
                    "需确认","重新检查环境",Action.CHECK_ENVIRONMENT,Tone.WARNING);
        if(modulePresent)
            return state("检测到 KernelSU","模块已加载，下一步验证 Root 权限与 SELinux 状态。",
                    "待验证","验证当前 Root",Action.RECOVER_OR_VERIFY,Tone.NEUTRAL);
        return state("可以恢复 Root","设备和执行环境已就绪。恢复只对本次开机有效。",
                "准备就绪","恢复 Root",Action.RECOVER_OR_VERIFY,Tone.NEUTRAL);
    }

    private static String nonempty(String value,String fallback) {
        return value==null||value.trim().isEmpty()?fallback:value;
    }
}
