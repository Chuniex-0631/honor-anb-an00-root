package com.anb.root;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class RecoveryOptionsTest {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception {
        check(!RecoveryOptions.DEFAULT_ALLOW_SHELL,"fresh installs and upgrades default to off");
        String template=new String(Files.readAllBytes(Paths.get(args[0])),StandardCharsets.UTF_8);
        String dir="/data/local/tmp/anb_root_runs/run_80_1000_0123456789abcdef0123456789abcdef";
        String off=RecoveryOptions.renderLoader(template,dir,"1000",false);
        String on=RecoveryOptions.renderLoader(template,dir,"1000",true);
        check(!off.contains("@")&&!on.contains("@"),"all loader placeholders resolved");
        String offBranch="if [ 0 = 1 ]; then KSU_PARAMS=\"allow_shell=1 \"; else KSU_PARAMS=\"\"; fi";
        String onBranch="if [ 1 = 1 ]; then KSU_PARAMS=\"allow_shell=1 \"; else KSU_PARAMS=\"\"; fi";
        check(off.contains(offBranch),"off keeps original empty module-parameter branch");
        check(on.contains(onBranch),"on enables the upstream allow_shell parameter");
        check(off.replace(offBranch,onBranch).equals(on),"the permission branch is the only loader difference");
        check(off.contains("bootid_ctl=$BOOTID_CTL_RT bootid_buf=$BOOTID_BUF_RT"),"boot-id repair preserved");
        check(off.contains("if grep -qi kernelsu /proc/modules && [ 1 = 1 ]; then"),"userspace bring-up preserved");
        check(off.contains("=== re-enable SELinux enforcing (last step) ==="),"enforcement completion preserved");
        check(on.equals(RecoveryOptions.renderLoader(template,dir,"1000",true)),"repeat rendering does not mutate template or prior selection");
        System.out.println("PASS: default off, both real loader branches, complete rendering, unchanged repair/bring-up/enforcement");
    }
}
