package com.anb.root;

/** Shared by the UI and worker. Every prerequisite is rechecked before execution. */
public final class Readiness {
    /** Exact confirmed production build. Never infer compatibility from kernel alone. */
    public static final String MODEL="ANB-AN00";
    public static final String KERNEL="5.10.236-android12-9-gd671cf8c2b02";
    public static final String FINGERPRINT="HONOR/ANB-AN00/HNANB:15/HONORANB-AN00/9DLDLD230CHNC00E230:user/release-keys";
    public static boolean compatible(String model,String kernel,int sdk,String fingerprint) {
        return MODEL.equals(model) && KERNEL.equals(kernel) && sdk==35 && FINGERPRINT.equals(fingerprint);
    }
    public enum Gate { DEVICE, SHIZUKU_INSTALL, KSU_INSTALL, SHIZUKU_START, SHIZUKU_VERSION, SHIZUKU_PERMISSION, SHELL_MODE, ENVIRONMENT, READY }
    public static Gate gate(boolean compatible, boolean shizukuInstalled, boolean ksuInstalled,
                            boolean alive, boolean modern, boolean granted, boolean shell, boolean environment) {
        if(!compatible)return Gate.DEVICE;
        if(!shizukuInstalled)return Gate.SHIZUKU_INSTALL;
        if(!ksuInstalled)return Gate.KSU_INSTALL;
        if(!alive)return Gate.SHIZUKU_START;
        if(!modern)return Gate.SHIZUKU_VERSION;
        if(!granted)return Gate.SHIZUKU_PERMISSION;
        if(!shell)return Gate.SHELL_MODE;
        if(!environment)return Gate.ENVIRONMENT;
        return Gate.READY;
    }
    public static boolean validEnvironment(String output) {
        boolean uid=false,seccomp=false;
        for(String line:output.replace("\r", "").split("\n")) {
            if(line.startsWith("uid=2000(shell)") && line.contains("context=u:r:shell:s0"))uid=true;
            if(line.trim().matches("Seccomp:\\s*0"))seccomp=true;
        }
        return uid&&seccomp;
    }
    public static boolean resume(String savedBoot,String currentBoot,String directory) {
        return currentBoot.matches("[0-9]+") && currentBoot.equals(savedBoot)
                && directory.matches("/data/local/tmp/anb_root_runs/run_[0-9]+_[0-9]+_[a-f0-9]{32}");
    }
}
