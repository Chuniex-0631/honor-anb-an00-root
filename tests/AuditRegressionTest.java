package com.anb.root;

import java.io.File;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Regressions for the 2.3.2 audit, using production JVM classes. No Android/root commands. */
public final class AuditRegressionTest {
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);}
    public static void main(String[] args)throws Exception {
        if(args.length>0){
            if(args[0].equals("stdin")){check(System.in.read()==-1,"stdin EOF");System.out.print("EOF");return;}
            if(args[0].equals("child")){Path staged=Paths.get(args[1]+".ready");Files.write(staged,Long.toString(ProcessHandle.current().pid()).getBytes("UTF-8"));Files.move(staged,Paths.get(args[1]));Thread.sleep(30000);return;}
        }
        check(DeviceProfile.supports(DeviceProfile.MODEL,35,DeviceProfile.DISPLAY,DeviceProfile.KERNEL),"exact profile");
        check(!DeviceProfile.supports(DeviceProfile.MODEL,35,DeviceProfile.DISPLAY+"x",DeviceProfile.KERNEL),"same kernel different firmware blocked");
        check(!DeviceProfile.supports(DeviceProfile.MODEL,34,DeviceProfile.DISPLAY,DeviceProfile.KERNEL),"SDK blocked");
        check(!DeviceProfile.supports(null,35,null,DeviceProfile.KERNEL),"missing properties blocked");
        String probe=DeviceProfile.MODEL+"\n35\n"+DeviceProfile.DISPLAY+"\n"+DeviceProfile.KERNEL+"\n";
        check(DeviceProfile.supportsProbe(probe),"shell profile");
        check(!DeviceProfile.supportsProbe(probe.replace(DeviceProfile.DISPLAY,"other")),"shell firmware mismatch");
        check(!DeviceProfile.supportsProbe(probe+"unexpected\n"),"unexpected probe output fails closed");

        for(Readiness.Gate gate:new Readiness.Gate[]{Readiness.Gate.SHIZUKU_START,Readiness.Gate.SHIZUKU_PERMISSION,Readiness.Gate.ENVIRONMENT}){
            for(String phase:new String[]{"SUCCESS","WAIT_AUTH","FAILED"}){
                StatusModel.Snapshot state=StatusModel.of(gate,phase,false,false,true,true,FailureText.Kind.UNKNOWN);
                check(state.badge!=StatusModel.Badge.VERIFIED&&state.action!=StatusModel.Action.VERIFY,"current gate takes precedence: "+gate+phase);
            }
        }
        check(StatusModel.of(Readiness.Gate.READY,"FAILED",false,false,true,false,FailureText.Kind.BOOT_WAIT).action==StatusModel.Action.RESTORE,"boot wait can retry");
        check(StatusModel.of(Readiness.Gate.READY,"FAILED",false,false,true,false,FailureText.Kind.TIMEOUT).action==StatusModel.Action.CHECK_PROGRESS,"timeout only checks progress");
        check(StatusModel.of(Readiness.Gate.READY,"FAILED",false,false,true,true,FailureText.Kind.SECURITY_NOT_RESTORED).action==StatusModel.Action.SAVE_DIAGNOSTICS,"security failure never offers restore");
        check(FailureText.classify("KernelSU 已加载，但保护状态尚未恢复。未判定成功；请保存日志。") == FailureText.Kind.SECURITY_NOT_RESTORED,"legacy security message classified correctly");
        check(FailureText.classify("发生未知故障，请保存日志。") == FailureText.Kind.UNKNOWN,"save advice is not storage failure");

        String valid="ANB_STATUS=2\nMODULE=1\nFINISHED=1\nENFORCE=Enforcing\nCLEANUP=1\nSTAGES=1\nDETAILS\n";
        check(RunStatus.isComplete(valid),"strict completion");
        check(!RunStatus.isComplete(valid.replace("FINISHED=1","FINISHED=0")+"FINISHED=1\n"),"tail cannot complete header");
        check(!RunStatus.isComplete(valid.replace("ENFORCE=Enforcing","ENFORCE=Permissive")+"ENFORCE=Enforcing\n"),"tail cannot override security");
        check(!RunStatus.isComplete(valid.replace("CLEANUP=1","CLEANUP=0"))&&RunStatus.cleanupFailed(valid.replace("CLEANUP=1","CLEANUP=0")),"unfinished cleanup blocks completion");
        check(!RunStatus.isComplete(valid.replace("STAGES=1","STAGES=0"))&&RunStatus.stagesFailed(valid.replace("STAGES=1","STAGES=0")),"failed userspace stages block completion");
        check(RunStatus.protectionVerified("uid=0(root)\nANB_PROTECTION=1\n"),"positive root protection proof");
        check(!RunStatus.protectionVerified("uid=0(root)\n")&&!RunStatus.protectionVerified("ANB_PROTECTION=1\nANB_PROTECTION=0\n"),"missing or conflicting protection proof rejected");
        check(!RunStatus.isComplete("MODULE=1\nFINISHED=1\nENFORCE=Enforcing\n"),"unframed text rejected");
        check(!Readiness.resume("78","78","/data/local/tmp/anb_root_runs/run_77_1000_0123456789abcdef0123456789abcdef"),"directory boot must match");
        check(!Readiness.resume(null,null,null),"missing state rejected");

        TaskGate tasks=new TaskGate();
        for(TaskGate.Task task:TaskGate.Task.values()){
            TaskGate.Lease owner=tasks.acquire(task);check(owner!=null,"acquire "+task);
            for(TaskGate.Task other:TaskGate.Task.values())check(tasks.acquire(other)==null,"shared exclusion");
            owner.close();TaskGate.Lease next=tasks.acquire(TaskGate.Task.RECOVERY);owner.close();
            check(tasks.busy(),"old owner cannot unlock a new task");next.close();
        }
        check(tasks.elapsedMillis()==0,"idle task age is zero");TaskGate.Lease timed=tasks.acquire(TaskGate.Task.CHECK);Thread.sleep(20);check(tasks.elapsedMillis()>=10,"task age belongs to lease, independent of activity");timed.close();check(tasks.elapsedMillis()==0,"completed task age cleared");
        AtomicInteger active=new AtomicInteger(),max=new AtomicInteger();ExecutorService pool=Executors.newFixedThreadPool(8);
        for(int i=0;i<100;i++)pool.submit(()->{TaskGate.Lease owner=tasks.acquire(TaskGate.Task.CHECK);if(owner!=null)try{max.accumulateAndGet(active.incrementAndGet(),Math::max);Thread.yield();active.decrementAndGet();}finally{owner.close();}});
        pool.shutdown();check(pool.awaitTermination(5,TimeUnit.SECONDS)&&max.get()==1&&!tasks.busy(),"concurrent ownership");

        String java=new File(System.getProperty("java.home"),"bin/java").getAbsolutePath(),classpath=System.getProperty("java.class.path");
        CommandRunner.Result eof=CommandRunner.run(new String[]{java,"-cp",classpath,AuditRegressionTest.class.getName(),"stdin"},3000);
        check(!eof.timedOut&&!eof.incomplete&&eof.output.equals("EOF"),"control process stdin closed");
        Path pid=Files.createTempFile("anb-regression-",".pid");Files.delete(pid);AtomicReference<Throwable> error=new AtomicReference<>();
        Thread worker=new Thread(()->{try{CommandRunner.run(new String[]{java,"-cp",classpath,AuditRegressionTest.class.getName(),"child",pid.toString()},20000);}catch(Throwable e){error.set(e);}});
        worker.start();long end=System.nanoTime()+5_000_000_000L;
        while(!Files.exists(pid)&&System.nanoTime()<end)Thread.sleep(20);
        check(Files.exists(pid),"test child started");
        long childPid=Long.parseLong(new String(Files.readAllBytes(pid),"UTF-8"));
        ProcessHandle child=ProcessHandle.of(childPid).orElse(null);
        try{worker.interrupt();worker.join(3000);check(!worker.isAlive()&&error.get() instanceof InterruptedException,"interruption propagated");
            if(child!=null)child.onExit().get(3,TimeUnit.SECONDS);
            check(!ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false),"interrupted child cleaned");
        }finally{if(child!=null&&child.isAlive())child.destroyForcibly();Files.deleteIfExists(pid);}
        System.out.println("PASS: strict firmware, actionable failures, typed security, framed status, boot binding, shared task ownership, interrupted process cleanup");
    }
}
