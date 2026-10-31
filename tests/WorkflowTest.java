package com.anb.root;
import java.io.*;

public final class WorkflowTest {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    static String javaCommand(){return new File(System.getProperty("java.home"),"bin/java").getAbsolutePath();}
    static String[] child(String mode){return new String[]{javaCommand(),"-Dfile.encoding=UTF-8","-cp",System.getProperty("java.class.path"),WorkflowTest.class.getName(),mode};}
    public static void main(String[] args)throws Exception {
        if(args.length>0){
            if(args[0].equals("unicode")){System.out.print("中文输出\n");return;}
            if(args[0].equals("error")){System.out.print("failure");System.exit(7);}
            if(args[0].equals("wait")){Thread.sleep(10000);return;}
            if(args[0].equals("large")){for(int i=0;i<100000;i++)System.out.print('x');return;}
        }
        Readiness.Gate[] expected={Readiness.Gate.DEVICE,Readiness.Gate.SHIZUKU_INSTALL,Readiness.Gate.KSU_INSTALL,Readiness.Gate.SHIZUKU_START,
                Readiness.Gate.SHIZUKU_VERSION,Readiness.Gate.SHIZUKU_PERMISSION,Readiness.Gate.SHELL_MODE,Readiness.Gate.ENVIRONMENT};
        for(int i=0;i<8;i++){
            boolean[] b={true,true,true,true,true,true,true,true};b[i]=false;
            check(Readiness.gate(b[0],b[1],b[2],b[3],b[4],b[5],b[6],b[7])==expected[i],"gate "+i);
        }
        check(Readiness.gate(true,true,true,true,true,true,true,true)==Readiness.Gate.READY,"ready");
        String env="uid=2000(shell) gid=2000(shell) context=u:r:shell:s0\nSeccomp:\t0\n";
        check(Readiness.validEnvironment(env),"shell ready");
        check(!Readiness.validEnvironment(env.replace("Seccomp:\t0","Seccomp:\t2")),"app filter must block");
        check(!Readiness.validEnvironment(env.replace("uid=2000(shell)","uid=0(root)")),"root backend must not mask shell test");
        String path="/data/local/tmp/anb_root_runs/run_77_1000_0123456789abcdef0123456789abcdef";
        check(Readiness.resume("77","77",path),"same-boot resume");
        check(!Readiness.resume("77","78",path),"never use prior boot result");
        check(!Readiness.resume("77","77",path+"; id"),"reject invalid saved path");
        check(!Readiness.resume("77","77","/data/local/tmp/other"),"restrict run directory");
        check(!RunStatus.isComplete("MODULE=1\nENFORCE=Enforcing\n"),"module alone not success");
        check(!RunStatus.isComplete("MODULE=1\nFINISHED=1\nENFORCE=Permissive\n"),"wait for enforcement");
        check(RunStatus.isComplete("MODULE=1\nFINISHED=1\nENFORCE=Enforcing\n"),"full completion");
        CommandRunner.Result r=CommandRunner.run(child("unicode"),3000);
        check(r.exitCode==0&&r.output.equals("中文输出\n"),"UTF-8 process output");
        r=CommandRunner.run(child("error"),3000);check(r.exitCode==7,"nonzero status");
        r=CommandRunner.run(child("wait"),150);check(r.timedOut&&r.exitCode==-1,"bounded command");
        r=CommandRunner.run(child("large"),8000);check(r.truncated&&r.output.length()==65536,"bounded, drained output");
        System.out.println("PASS: 8 prerequisite gates, shell/seccomp, same-boot recovery, path validation, completion, process output/exit/timeout/limit");
    }
}
