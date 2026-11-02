package com.anb.root;

/** Shared by the UI and worker. Every prerequisite is rechecked before execution. */
public final class Readiness {
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
        if(currentBoot==null||!currentBoot.matches("[0-9]+")||!currentBoot.equals(savedBoot)||!validDirectory(directory))return false;
        return directory.startsWith("/data/local/tmp/anb_root_runs/run_"+currentBoot+"_");
    }
    public static boolean validDirectory(String directory) {
        return directory!=null&&directory.matches("/data/local/tmp/anb_root_runs/run_[0-9]+_[0-9]+_[a-f0-9]{32}");
    }
}
