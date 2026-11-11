package com.anb.root;

import android.content.*;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import rikka.shizuku.Shizuku;

public final class RecoveryEngine {
    public static final String KERNEL=DeviceProfile.KERNEL;
    public static final String SHIZUKU="moe.shizuku.privileged.api", KSU="me.weishu.kernelsu";
    static final String ROOT="/data/local/tmp/anb_root_runs";
    static final String LOG_NAME="recovery.log";
    static final String[] FILES={"exploit","kernelsu.ko","ksud","magiskpolicy","load_ko","kmsg_dumper","ksu_rules","ksu_loader.tmpl"};
    public enum Phase { IDLE,CHECKING,PREPARING,RUNNING,LOADING,VERIFYING,WAIT_AUTH,SUCCESS,FAILED }
    private static RecoveryEngine instance;
    public static synchronized RecoveryEngine get(Context c) {
        if(instance==null)instance=new RecoveryEngine(c.getApplicationContext()); return instance;
    }
    final Context context;
    private final TaskGate tasks=new TaskGate();
    private final AtomicBoolean warnedOptions=new AtomicBoolean(),warnedSetup=new AtomicBoolean();
    public volatile boolean environmentReady, modulePresent;
    public volatile String bootCount="未知";
    public volatile FailureText.Kind failureKind=FailureText.Kind.UNKNOWN;
    public volatile Phase phase=Phase.IDLE;
    public volatile String title="先完成准备", message="检查所需应用，启动 Shizuku 并授予权限。";
    public volatile int step;
    private final StringBuilder log=new StringBuilder();
    private String dir;
    public interface Progress { void changed(String text); }
    private volatile Progress progress;
    private RecoveryEngine(Context c) {
        context=c;
        try (RandomAccessFile file=new RandomAccessFile(new File(context.getFilesDir(),LOG_NAME),"r")) {
            long start=Math.max(0,file.length()-540000);file.seek(start);
            byte[] bytes=new byte[(int)(file.length()-start)];file.readFully(bytes);
            log.append(new String(bytes,StandardCharsets.UTF_8));
        }
        catch(Exception ignored) { }
    }
    public boolean compatible() { return DeviceProfile.supports(android.os.Build.MODEL,android.os.Build.VERSION.SDK_INT,
            android.os.Build.DISPLAY,System.getProperty("os.version")); }
    public boolean isBusy(){return tasks.busy();}
    public boolean isChecking(){return tasks.checking();}
    public TaskGate.Task currentTask(){return tasks.current();}
    public long taskElapsedMillis(){return tasks.elapsedMillis();}
    /** Cached binder liveness for presentation; the worker performs the real IPC probe. */
    public static boolean shizukuAlive(){android.os.IBinder binder=Shizuku.getBinder();return binder!=null&&binder.isBinderAlive();}
    public boolean installed(String pkg) {
        try { context.getPackageManager().getPackageInfo(pkg,0);return true; }
        catch(PackageManager.NameNotFoundException e) { return false; }
    }
    public Readiness.Gate gate() {
        boolean alive=false,modern=false,grant=false,shell=false;
        try {
            alive=shizukuAlive();
            if(alive) { modern=Shizuku.getVersion()>=13;grant=Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED;shell=Shizuku.getUid()==2000; }
        } catch(Exception ignored) { }
        return Readiness.gate(compatible(),installed(SHIZUKU),installed(KSU),alive,modern,grant,shell,environmentReady);
    }
    public void invalidate() { environmentReady=false; }
    public boolean allowShellSelected() {
        try{return context.getSharedPreferences(RecoveryOptions.PREFERENCES,0)
                .getBoolean(RecoveryOptions.ALLOW_SHELL,RecoveryOptions.DEFAULT_ALLOW_SHELL);}
        catch(ClassCastException e){
            if(warnedOptions.compareAndSet(false,true))record("恢复选项记录格式异常，已按安全默认值关闭调试工具 Root；可在恢复前重新保存选择。");
            return false;
        }
    }
    public boolean setupCompleted() {
        try{return context.getSharedPreferences("setup",0).getBoolean("completed",false);}
        catch(ClassCastException e){
            if(warnedSetup.compareAndSet(false,true))record("首次准备记录格式异常，请按首页提示重新完成首次准备。");
            return false;
        }
    }
    public boolean selectAllowShell(boolean enabled) {
        TaskGate.Lease owner=tasks.acquire(TaskGate.Task.SETTINGS);if(owner==null)return false;
        try {
            if(modulePresent)return false;
            return context.getSharedPreferences(RecoveryOptions.PREFERENCES,0).edit()
                    .putBoolean(RecoveryOptions.ALLOW_SHELL,enabled).commit();
        }finally{owner.close();}
    }
    public synchronized String logs() { return log.toString(); }
    /**
     * Append the line instead of rewriting the whole file: a recovery writes dozens of lines and
     * the old version re-serialised up to 180 KB each time. Rewrite only when the buffer is trimmed.
     */
    public synchronized void record(String text) {
        String line=new java.text.SimpleDateFormat("HH:mm:ss.SSS",Locale.CHINA).format(new Date())+"  "+text+"\n";
        log.append(line);
        File file=new File(context.getFilesDir(),LOG_NAME);
        if(log.length()>180000) {
            log.delete(0,log.length()-120000);
            try { write(file,log.toString()); } catch(IOException ignored) { }
            return;
        }
        try(OutputStream out=new FileOutputStream(file,true)) {
            out.write(line.getBytes(StandardCharsets.UTF_8));
        }catch(IOException ignored){}
    }
    private void status(Phase p,int stage,String heading,String detail) {
        if(p!=Phase.FAILED)failureKind=FailureText.Kind.UNKNOWN;
        phase=p;step=stage;title=heading;message=detail;
        record(heading+"："+detail);Progress current=progress;
        if(current!=null)try{current.changed(heading);}catch(RuntimeException e){record("进度显示未更新；后台检查继续。");}
    }
    public void checkEnvironment() {
        TaskGate.Lease owner=tasks.acquire(TaskGate.Task.CHECK);if(owner==null)return;
        startWorker(owner,"environment-check",()->{
            String previousBoot=bootCount;
            try(ShizukuShell shell=ShizukuShell.open(context)) {
                probe(shell);
                modulePresent=hasModule(shell);
                // Rechecking prerequisites must not erase a same-boot safety failure or
                // a verified/authorization state. A new boot or a missing module clears old proof.
                boolean sameBoot=bootCount.equals(previousBoot);
                boolean retainFailure=phase==Phase.FAILED&&failureKind!=FailureText.Kind.BOOT_WAIT
                        &&failureKind!=FailureText.Kind.SHIZUKU&&failureKind!=FailureText.Kind.PERMISSION
                        &&failureKind!=FailureText.Kind.PREPARE;
                if(sameBoot&&(retainFailure||modulePresent&&(phase==Phase.SUCCESS||phase==Phase.WAIT_AUTH)))
                    record("执行环境检查通过；保留本次开机的验证结果或处理提示。");
                else status(Phase.IDLE,0,"准备就绪",modulePresent?"已发现 KernelSU；可以验证本次权限，无需重复恢复。":"点击恢复，本次开机内有效。");
            }catch(Exception e) {
                environmentReady=false;
                failed(e,0,"执行环境尚未就绪");
            }finally{owner.close();}
        });
    }
    private void probe(ShizukuShell shell) throws Exception {
        String output=shell.shell("id; sed -n '/^Seccomp:/p' /proc/self/status");
        record("Shizuku UserService 环境：\n"+output.trim());
        if(!Readiness.validEnvironment(output))throw new IOException("执行进程不是预期的 shell 环境；请通过无线调试重新启动 Shizuku。");
        if(!DeviceProfile.supportsProbe(shell.shell("getprop ro.product.model; getprop ro.build.version.sdk; getprop ro.build.display.id; uname -r")))
            throw new RecoveryFailure(FailureText.Kind.COMPATIBILITY,"当前设备、固件或内核不在支持范围内。");
        bootCount=shell.shell("settings get global boot_count").trim();
        if(!bootCount.matches("[0-9]+"))throw new RecoveryFailure(FailureText.Kind.PREPARE,"无法确认本次开机编号。");
        environmentReady=true;
        File ext=context.getExternalFilesDir(null);
        if(ext!=null)writeDiagnostic(new File(ext,"environment-shizuku.txt"),"transport=Shizuku UserService\n"+output+"kernel="+KERNEL+"\n");
    }
    public boolean start(Progress listener,Runnable finished,boolean allowNewRecovery) {
        TaskGate.Lease owner=tasks.acquire(TaskGate.Task.RECOVERY);if(owner==null)return false;
        boolean launched=false;
        try {
            final boolean allowShell=allowShellSelected();
            progress=listener;
            new Thread(()->{
                try { recover(allowShell,allowNewRecovery); }
                catch(Exception e){failed(e,step,"本次未完成");}
                finally{
                    progress=null;owner.close();
                    if(finished!=null)try{finished.run();}catch(RuntimeException e){record("完成提示未更新；本次任务已结束。");}
                }
            },"root-recovery").start();
            launched=true;return true;
        }catch(RuntimeException e){
            failed(new RecoveryFailure(FailureText.Kind.PREPARE,"无法启动本次任务，请重新打开应用并检查准备状态。"),0,"本次任务未启动");
            return false;
        }finally{if(!launched){progress=null;owner.close();}}
    }
    private void recover(boolean allowShell,boolean allowNewRecovery) throws Exception {
        dir=null;
        if(!compatible())throw new RecoveryFailure(FailureText.Kind.COMPATIBILITY,"当前设备、固件或内核不在支持范围内。");
        if(!installed(SHIZUKU) || !installed(KSU))throw new RecoveryFailure(FailureText.Kind.PREPARE,"请先完成依赖检查。");
        if(!setupCompleted())throw new RecoveryFailure(FailureText.Kind.PREPARE,"请先完成首次使用准备。");
        status(Phase.CHECKING,0,"检查本次状态","正在确认执行环境和已有运行记录。");
        try(ShizukuShell shell=ShizukuShell.open(context)) {
            probe(shell);
            String boot=shell.shell("settings get global boot_count").trim();
            if(!boot.matches("[0-9]+"))throw new IOException("无法确认本次开机编号。");
            SharedPreferences state=context.getSharedPreferences("run",0);
            String savedBoot,previous;
            try{savedBoot=state.getString("boot","");previous=state.getString("dir","");}
            catch(ClassCastException e){throw corruptRunRecord();}
            // Invalid persisted ownership cannot become trustworthy merely by rebooting.
            // Validate even an older boot's record, before any module/launch fast path.
            if(savedBoot==null||previous==null||!savedBoot.isEmpty()&&!savedBoot.matches("[0-9]+")
                    ||!previous.isEmpty()&&!Readiness.resume(savedBoot,savedBoot,previous))throw corruptRunRecord();
            boolean same=Readiness.resume(savedBoot,boot,previous);
            if(same)dir=previous;
            // A loaded module does not prove that an orphaned loader has finished.
            String lock=ROOT+"/boot_"+boot+".lock";
            if(!same&&!"CLEAR".equals(shell.shell("if [ -e "+q(lock)+" ]; then echo LOCKED; else echo CLEAR; fi").trim()))
                throw new RecoveryFailure(FailureText.Kind.REBOOT_REQUIRED,"本次开机已有恢复尝试，但记录不可用。请保存诊断并完整重启后再恢复。");
            modulePresent=hasModule(shell);
            if(modulePresent) {
                if(same && !finished(shell,previous)) {
                    dir=previous;record("已有模块，但此次加载尚未收尾；继续等待原记录。");monitor(shell,boot);
                }else verify(shell,boot);
                return;
            }
            if(same) {dir=previous;record("同次开机已有尝试，只恢复查看进度，不重复启动。");monitor(shell,boot);return;}
            if(!allowNewRecovery)throw new RecoveryFailure(FailureText.Kind.PREPARE,"没有可验证的恢复记录。请先检测执行环境；如需恢复，请使用“恢复 Root”。");
            if(!"1".equals(shell.shell("getprop sys.boot_completed").trim()))throw new IOException("系统仍在启动，请进入桌面后再试。");
            long uptime=Long.parseLong(shell.shell("cut -d. -f1 /proc/uptime").trim());
            if(uptime<90)throw new RecoveryFailure(FailureText.Kind.BOOT_WAIT,"请等待开机满 90 秒后再恢复；约还需 "+(90-uptime)+" 秒。");
            String active=shell.shell("if pgrep -f 'anb_root.*/explo[i]t|ksu_loade[r].sh|gl_ex[p]loit' >/dev/null; then echo ACTIVE; else echo IDLE; fi").trim();
            if(!"IDLE".equals(active))throw new IOException("另一次恢复仍在运行。请等待；不要并发启动。");
            if(!state.edit().putString("options_boot",boot).putBoolean("allow_shell",allowShell).commit())
                throw new IOException("无法保存恢复选项，已停止。");
            record("本次恢复选项：允许调试工具使用 Root="+(allowShell?"开启":"关闭")+"。已有尝试恢复进度时不会更改选项。");
            // Match the verified upstream driver: do not leave a pre-load manager alive.
            shell.shell("am force-stop "+KSU);
            record("KernelSU 管理器已在加载前停止；完成后将重新启动。");
            dir=ROOT+"/run_"+boot+"_"+System.currentTimeMillis()+"_"+UUID.randomUUID().toString().replace("-","");
            status(Phase.PREPARING,1,"准备本次恢复","校验内置文件并创建独立运行目录。");
            stage(shell,allowShell);
            save(boot,"launching");
            String launch="set -e; mkdir "+q(ROOT+"/boot_"+boot+".lock")+"; cd "+q(dir)
                    +"; nohup env KSU_RUNDIR="+q(dir)+" "+q(dir+"/exploit")+" >gl.log 2>&1 </dev/null &";
            shell.shell(launch);
            status(Phase.RUNNING,2,"正在恢复权限","通常需要 1–3 分钟。请勿重复启动；若手机重启，解锁后重新启动 Shizuku 再检查。");
            save(boot,"running");monitor(shell,boot);
        }
    }
    private boolean hasModule(ShizukuShell s)throws Exception {
        return s.shell("if grep -q '^kernelsu ' /proc/modules; then echo YES; else code=$?; [ \"$code\" = 1 ] || exit \"$code\"; echo NO; fi").trim().equals("YES");
    }
    private String snapshot(ShizukuShell s,String directory)throws Exception {
        if(!Readiness.validDirectory(directory))throw new RecoveryFailure(FailureText.Kind.REBOOT_REQUIRED,"恢复目录无效，请完整重启后再恢复。");
        return s.shell("cd "+q(directory)+" || exit 1; printf 'ANB_STATUS=2\\n'; "
                +"if grep -q '^kernelsu ' /proc/modules; then echo MODULE=1; else code=$?; [ \"$code\" = 1 ] || exit \"$code\"; echo MODULE=0; fi; "
                +"if grep -q '^KernelSU module loaded$' ksu_load.log 2>/dev/null && grep -q '^=== re-enable SELinux enforcing (last step) ===$' ksu_load.log 2>/dev/null; then echo FINISHED=1; else echo FINISHED=0; fi; "
                +"printf 'ENFORCE='; getenforce; "
                +"if grep -Eq '^sig_enforce=(Y|1)$' ksu_load.log && grep -q '^umount OK$' ksu_load.log "
                +"&& grep -Eq '^kptr restored=[0-9]+$' ksu_load.log && grep -Eq '^panic_on_oops=[0-9]+$' ksu_load.log "
                +"&& { ! grep -q '^hisecd frozen pid=' ksu_load.log || grep -q '^hisecd resumed pid=' ksu_load.log; }; "
                +"then echo CLEANUP=1; else echo CLEANUP=0; fi; "
                +"if grep -q '^post-fs-data exit=0$' ksu_load.log && grep -q '^services exit=0$' ksu_load.log "
                +"&& grep -q '^boot-completed exit=0$' ksu_load.log && grep -q '^install exit=0$' ksu_load.log; "
                +"then echo STAGES=1; else echo STAGES=0; fi; "
                +"echo DETAILS; tail -n 5 ksu_load.log 2>/dev/null; tail -n 4 gl.log 2>/dev/null; true");
    }
    private boolean finished(ShizukuShell s,String directory)throws Exception {return RunStatus.isComplete(snapshot(s,directory));}
    private void monitor(ShizukuShell shell,String boot)throws Exception {
        long end=SystemClock.elapsedRealtime()+300000;String last="";
        while(SystemClock.elapsedRealtime()<end) {
            if(!boot.equals(shell.shell("settings get global boot_count").trim()))throw new IOException("手机已重启。请重新启动 Shizuku，然后返回检查。");
            String out=snapshot(shell,dir);
            modulePresent=RunStatus.modulePresent(out);
            if(!last.equals(out)){record(out);last=out;}
            if(RunStatus.modulePresent(out) && phase!=Phase.LOADING)
                status(Phase.LOADING,3,"正在完成加载","模块已出现，正在等待恢复收尾；尚未确认成功。");
            if(RunStatus.cleanupFailed(out))throw new RecoveryFailure(FailureText.Kind.SECURITY_NOT_RESTORED,
                    "恢复已到收尾阶段，但未能确认所有保护设置和后台服务已恢复。请保存记录并完整重启手机。");
            if(RunStatus.stagesFailed(out))throw new RecoveryFailure(FailureText.Kind.REBOOT_REQUIRED,
                    "模块已加载，但 KernelSU 初始化阶段未全部完成。请保存记录并完整重启后再恢复。");
            if(RunStatus.isComplete(out)){modulePresent=true;verify(shell,boot);return;}
            Thread.sleep(3000);
        }
        save(boot,"timeout");
        if(RunStatus.securityNotRestored(last))throw new RecoveryFailure(FailureText.Kind.SECURITY_NOT_RESTORED,
                "等待结束后 SELinux 未恢复为 Enforcing。请保存记录并完整重启手机；本次未判定成功。");
        throw new RecoveryFailure(FailureText.Kind.TIMEOUT,"等待超过 5 分钟，未确认完成。记录已保留；本次开机不会自动重跑，请重启后再试。");
    }
    private void verify(ShizukuShell shell,String boot)throws Exception {
        status(Phase.VERIFYING,4,"验证 Root 与保护状态","正在通过 KernelSU 检查本 App 的权限。");
        String enforce=shell.shell("getenforce").trim();
        if(!"Enforcing".equals(enforce))throw new RecoveryFailure(FailureText.Kind.SECURITY_NOT_RESTORED,
                "KernelSU 已加载，但 SELinux 未恢复为 Enforcing。请保存记录并完整重启；本次未判定成功。");
        CommandRunner.Result result;
        try {result=CommandRunner.run(new String[]{"/system/bin/su","-c",rootVerificationCommand(dir)},20000);}
        catch(IOException e){waitAuth(shell,boot);return;}
        if(result.timedOut || result.truncated || result.incomplete || result.exitCode!=0
                || !result.output.matches("(?s)(?:.*\\n)?uid=0\\(root\\)(?:\\s.*)?")){waitAuth(shell,boot);return;}
        if(!RunStatus.protectionVerified(result.output))throw new RecoveryFailure(FailureText.Kind.SECURITY_NOT_RESTORED,
                "Root 权限可用，但未能确认模块签名校验、内核保护设置或后台服务已恢复。请保存记录并完整重启手机。");
        modulePresent=hasModule(shell);
        if(!modulePresent)throw new RecoveryFailure(FailureText.Kind.PREPARE,"模块状态已变化，请重新检测执行环境。");
        if(!"Enforcing".equals(shell.shell("getenforce").trim()))
            throw new RecoveryFailure(FailureText.Kind.SECURITY_NOT_RESTORED,"验证期间 SELinux 未保持 Enforcing。请保存记录并完整重启手机。");
        record("ROOT_PROOF "+result.output.trim()+"\nSELinux="+enforce+"\nboot_count="+boot);
        save(boot,"verified");
        File ext=context.getExternalFilesDir(null);
        if(ext!=null)writeDiagnostic(new File(ext,"verification.txt"),"boot_count="+boot+"\ntransport=Shizuku UserService\nkernel="+KERNEL+"\n"+result.output+"SELinux="+enforce+"\n");
        boolean manager=refreshManager(shell);
        status(Phase.SUCCESS,5,"Root 已验证",manager
                ?"KernelSU 管理器已重新启动。请确认首页显示“工作中”；本次开机有效，SELinux 为 Enforcing。"
                :"Root 与 Enforcing 已验证；管理器刷新未完成，请在页面点击“刷新 KernelSU”。无需重新恢复。");
    }
    static String rootVerificationCommand(String directory) {
        String command="id; ok=1; sig=$(cat /sys/module/module/parameters/sig_enforce); "
                +"printf 'sig_enforce=%s\n' \"$sig\"; case \"$sig\" in Y|1) ;; *) ok=0;; esac; "
                +"mounts=$(cat /proc/self/mountinfo) || ok=0; case \"$mounts\" in *' /proc/kallsyms '*) ok=0;; esac; "
                +"for p in $(pidof hisecd); do state=$(awk '/^State:/{print $2}' /proc/$p/status); "
                +"case \"$state\" in T|t|'') ok=0;; esac; done; ";
        if(Readiness.validDirectory(directory))command+="kr=$(sed -n 's/^kptr was=//p' "+q(directory+"/ksu_load.log")+" | head -n 1); "
                +"po=$(sed -n 's/^panic_on_oops(before)=//p' "+q(directory+"/ksu_load.log")+" | head -n 1); "
                +"[ -n \"$kr\" ] && [ \"$kr\" = \"$(cat /proc/sys/kernel/kptr_restrict)\" ] || ok=0; "
                +"[ -n \"$po\" ] && [ \"$po\" = \"$(cat /proc/sys/kernel/panic_on_oops)\" ] || ok=0; ";
        return command+"[ \"$(getenforce)\" = Enforcing ] || ok=0; printf 'ANB_PROTECTION=%s\n' \"$ok\"";
    }
    private void waitAuth(ShizukuShell shell,String boot)throws Exception {
        modulePresent=hasModule(shell);
        if(!modulePresent)throw new RecoveryFailure(FailureText.Kind.PREPARE,"模块状态已变化，请重新检测执行环境。");
        if(!"Enforcing".equals(shell.shell("getenforce").trim()))
            throw new RecoveryFailure(FailureText.Kind.SECURITY_NOT_RESTORED,"验证期间 SELinux 未保持 Enforcing。请保存记录并完整重启手机。");
        save(boot,"waiting_authorization");
        refreshManager(shell);
        status(Phase.WAIT_AUTH,4,"KernelSU 已就绪，等待授权","请打开 KernelSU → 超级用户，给 ANB Root 开启授权；返回后点击“验证 Root”。无需重新运行恢复链。");
    }
    private boolean refreshManager(ShizukuShell shell) {
        try {
            if(!hasModule(shell)||!"Enforcing".equals(shell.shell("getenforce").trim()))
                throw new IOException("模块或保护状态尚未就绪。");
            shell.shell("am force-stop "+KSU);
            String resolved=shell.shell("cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "+KSU).trim();
            String[] lines=resolved.split("\n");String activity=lines[lines.length-1].trim();
            if(!activity.matches("me\\.weishu\\.kernelsu/[A-Za-z0-9_.$]+"))throw new IOException("无法找到管理器启动页面。");
            String launch=shell.shell("am start -W -n "+q(activity));
            if(launch.contains("Error:")||launch.contains("Exception"))throw new IOException("系统未能打开管理器。");
            record("MANAGER_REFRESH_OK "+activity+"\n"+launch.trim());
            return true;
        }catch(Exception e){record("管理器刷新未完成："+friendly(e));return false;}
    }
    public void refreshManagerOnly() {
        TaskGate.Lease owner=tasks.acquire(TaskGate.Task.MANAGER);if(owner==null)return;
        startWorker(owner,"manager-refresh",()->{
            try(ShizukuShell shell=ShizukuShell.open(context)) {
                boolean ok=refreshManager(shell);
                message=ok?"KernelSU 管理器已重新启动，请查看首页状态。没有重新运行提权。":"刷新未完成，请检查 Shizuku 或导出记录。没有重新运行提权。";
            }catch(Exception e){message=friendly(e);record(message);}
            finally{owner.close();}
        });
    }
    private void startWorker(TaskGate.Lease owner,String name,Runnable work) {
        boolean launched=false;
        try{new Thread(work,name).start();launched=true;}
        catch(RuntimeException e){failed(new RecoveryFailure(FailureText.Kind.PREPARE,"无法启动检查任务，请重新打开应用后再试。"),0,"本次任务未启动");}
        finally{if(!launched)owner.close();}
    }
    private void failed(Exception error,int stage,String heading) {
        failureKind=error instanceof RecoveryFailure?((RecoveryFailure)error).kind:FailureText.classify(friendly(error));
        status(Phase.FAILED,stage,heading,friendly(error));
    }
    public void serviceUnavailable(Exception error) {
        TaskGate.Lease owner=tasks.acquire(TaskGate.Task.CHECK);
        if(owner==null){record("进度服务未启动："+friendly(error));return;}
        try {failed(new RecoveryFailure(FailureText.Kind.PREPARE,"系统未允许启动进度服务。请返回首页重新检查后再试。"),0,"进度服务未启动");}
        finally{owner.close();}
    }
    private static RecoveryFailure corruptRunRecord() {
        return new RecoveryFailure(FailureText.Kind.REBOOT_REQUIRED,
                "保存的恢复记录格式异常，无法安全继续。请先保存诊断，完整重启手机，再清除 ANB Root 应用数据并重新完成首次准备；仅重启不会修复此记录。");
    }
    private void save(String boot,String result)throws IOException {
        if(!context.getSharedPreferences("run",0).edit().putString("boot",boot).putString("dir",dir==null?"":dir).putString("phase",result).commit())
            throw new IOException("无法保存本次运行记录，已停止。");
    }
    private void stage(ShizukuShell shell,boolean allowShell)throws Exception {
        File ext=context.getExternalFilesDir(null);
        if(ext==null)throw new IOException("手机存储尚不可用，请解锁后重试。");
        File files=new File(ext,"payload-v2");if(!files.isDirectory()&&!files.mkdirs())throw new IOException("无法准备文件目录。");
        Properties hashes=new Properties();try(InputStream in=context.getAssets().open("payload-hashes.properties")){hashes.load(in);}
        StringBuilder commands=new StringBuilder("set -e; mkdir -p ").append(q(ROOT)).append("; mkdir ").append(q(dir)).append("; ");
        for(String name:FILES) {
            File file=new File(files,name);MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(InputStream in=context.getAssets().open("payload/"+name);FileOutputStream out=new FileOutputStream(file,false)) {
                byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);digest.update(buf,0,n);}
            }
            StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(Locale.US,"%02x",b&255));
            if(!hex.toString().equals(hashes.getProperty(name)))throw new IOException("内置文件校验失败："+name);
            commands.append("cp ").append(q(file.getAbsolutePath())).append(" ").append(q(dir+"/"+name)).append("; ");
            commands.append("echo ").append(q(hashes.getProperty(name)+"  "+dir+"/"+name)).append(" | sha256sum -c -; ");
        }
        String loader=RecoveryOptions.renderLoader(read(new File(files,"ksu_loader.tmpl")),dir,
                String.valueOf(System.currentTimeMillis()),allowShell);
        File rendered=new File(files,"ksu_loader.sh");write(rendered,loader);
        commands.append("cp ").append(q(rendered.getAbsolutePath())).append(" ").append(q(dir+"/ksu_loader.sh")).append("; ");
        for(String name:new String[]{"exploit","load_ko","magiskpolicy","ksud","kmsg_dumper","ksu_loader.sh"})
            commands.append("chmod 755 ").append(q(dir+"/"+name)).append("; ");
        shell.shell(commands.toString());record("PAYLOAD_SHA256_OK "+dir);
    }
    static String q(String text){return ShizukuShell.quote(text);}
    static String read(File file)throws IOException {
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private void writeDiagnostic(File file,String text) {
        try {write(file,text);}
        catch(IOException e){record("辅助诊断保存未完成："+file.getName()+"；不影响本次检查或验证。");}
    }
    static void write(File file,String text)throws IOException {
        try(FileOutputStream out=new FileOutputStream(file,false)){out.write(text.getBytes(StandardCharsets.UTF_8));}
    }
    static String friendly(Exception e) {
        if(e instanceof android.os.DeadObjectException)return "Shizuku 执行服务已断开。请重新启动 Shizuku；返回后只检查原进度，不会重复运行。";
        String text=e.getMessage();return text==null?e.getClass().getSimpleName():text;
    }
}
