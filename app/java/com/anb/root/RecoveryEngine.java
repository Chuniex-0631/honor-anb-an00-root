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
    public static final String KERNEL="5.10.236-android12-9-gd671cf8c2b02";
    public static final String SHIZUKU="moe.shizuku.privileged.api", KSU="me.weishu.kernelsu";
    static final String ROOT="/data/local/tmp/anb_root_runs";
    static final String[] FILES={"exploit","kernelsu.ko","ksud","magiskpolicy","load_ko","kmsg_dumper","ksu_rules","ksu_loader.tmpl"};
    public enum Phase { IDLE,CHECKING,PREPARING,RUNNING,LOADING,VERIFYING,WAIT_AUTH,SUCCESS,FAILED }
    private static RecoveryEngine instance;
    public static synchronized RecoveryEngine get(Context c) {
        if(instance==null)instance=new RecoveryEngine(c.getApplicationContext()); return instance;
    }
    final Context context;
    final AtomicBoolean busy=new AtomicBoolean();
    public volatile boolean environmentReady, checking, modulePresent;
    public volatile Phase phase=Phase.IDLE;
    public volatile String title="先完成准备", message="检查所需应用，启动 Shizuku 并授予权限。";
    public volatile int step;
    private final StringBuilder log=new StringBuilder();
    private String dir;
    public interface Progress { void changed(String text); }
    private volatile Progress progress;
    private RecoveryEngine(Context c) {
        context=c;
        // Remove the retired client's local credentials; never import or use ADB keys in v2.
        context.getSharedPreferences("adbkey",0).edit().clear().apply();
        File ext=context.getExternalFilesDir(null);
        if(ext!=null)for(String name:new String[]{"adbkey","adb_pub.txt"})new File(ext,name).delete();
        try { log.append(read(new File(context.getFilesDir(),"recovery-v2.log"))); }
        catch(Exception ignored) { }
    }
    public boolean compatible() { return "ANB-AN00".equals(android.os.Build.MODEL) && KERNEL.equals(System.getProperty("os.version")); }
    public boolean installed(String pkg) {
        try { context.getPackageManager().getPackageInfo(pkg,0);return true; }
        catch(PackageManager.NameNotFoundException e) { return false; }
    }
    public String version(String pkg) {
        try{return context.getPackageManager().getPackageInfo(pkg,0).versionName;}
        catch(Exception e){return "未安装";}
    }
    public Readiness.Gate gate() {
        boolean alive=false,modern=false,grant=false,shell=false;
        try {
            alive=Shizuku.pingBinder();
            if(alive) { modern=Shizuku.getVersion()>=13;grant=Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED;shell=Shizuku.getUid()==2000; }
        } catch(Exception ignored) { }
        return Readiness.gate(compatible(),installed(SHIZUKU),installed(KSU),alive,modern,grant,shell,environmentReady);
    }
    public void invalidate() { environmentReady=false; }
    public synchronized String logs() { return log.toString(); }
    public synchronized void record(String text) {
        String line=new java.text.SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(new Date())+"  "+text+"\n";
        log.append(line);
        if(log.length()>180000)log.delete(0,log.length()-120000);
        try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),"recovery-v2.log"),false)) {
            out.write(log.toString().getBytes(StandardCharsets.UTF_8));
        }catch(IOException ignored){}
    }
    private void status(Phase p,int stage,String heading,String detail) {
        phase=p;step=stage;title=heading;message=detail;
        record(heading+"："+detail);Progress current=progress;if(current!=null)current.changed(heading);
    }
    public void checkEnvironment() {
        if(checking||busy.get())return;
        checking=true;
        new Thread(()->{
            try(ShizukuShell shell=ShizukuShell.open(context)) {
                probe(shell);
                modulePresent=hasModule(shell);
                status(Phase.IDLE,0,"准备就绪",modulePresent?"已发现 KernelSU；可以验证本次权限，无需重复恢复。":"点击恢复，本次开机内有效。");
            }catch(Exception e) {
                environmentReady=false;
                status(Phase.FAILED,0,"执行环境尚未就绪",friendly(e));
            }finally{checking=false;}
        },"environment-check").start();
    }
    private void probe(ShizukuShell shell) throws Exception {
        String output=shell.shell("id; sed -n '/^Seccomp:/p' /proc/self/status");
        record("Shizuku UserService 环境：\n"+output.trim());
        if(!Readiness.validEnvironment(output))throw new IOException("执行进程不是预期的 shell 环境；请通过无线调试重新启动 Shizuku。");
        if(!KERNEL.equals(shell.shell("uname -r").trim()) || !"ANB-AN00".equals(shell.shell("getprop ro.product.model").trim()))
            throw new IOException("当前设备或系统版本不在支持范围内。");
        environmentReady=true;
        File ext=context.getExternalFilesDir(null);
        if(ext!=null)write(new File(ext,"environment-shizuku.txt"),"transport=Shizuku UserService\n"+output+"kernel="+KERNEL+"\n");
    }
    public boolean start(Progress listener,Runnable finished) {
        if(!busy.compareAndSet(false,true))return false;
        progress=listener;
        new Thread(()->{
            try { recover(); }
            catch(Exception e){status(Phase.FAILED,step,"本次未完成",friendly(e));}
            finally{busy.set(false);progress=null;finished.run();}
        },"root-recovery").start();
        return true;
    }
    private void recover() throws Exception {
        if(!compatible() || !installed(SHIZUKU) || !installed(KSU))throw new IOException("请先完成依赖检查。");
        if(!context.getSharedPreferences("setup",0).getBoolean("completed",false))throw new IOException("请先完成首次使用准备。");
        status(Phase.CHECKING,0,"检查本次状态","正在确认执行环境和已有运行记录。");
        try(ShizukuShell shell=ShizukuShell.open(context)) {
            probe(shell);
            String boot=shell.shell("settings get global boot_count").trim();
            if(!boot.matches("[0-9]+"))throw new IOException("无法确认本次开机编号。");
            SharedPreferences state=context.getSharedPreferences("run",0);
            String previous=state.getString("dir","");
            boolean same=Readiness.resume(state.getString("boot",""),boot,previous);
            modulePresent=hasModule(shell);
            if(modulePresent) {
                if(same && !finished(shell,previous)) {
                    dir=previous;record("已有模块，但此次加载尚未收尾；继续等待原记录。");monitor(shell,boot);
                }else verify(shell,boot);
                return;
            }
            if(same) {dir=previous;record("同次开机已有尝试，只恢复查看进度，不重复启动。");monitor(shell,boot);return;}
            if(!"1".equals(shell.shell("getprop sys.boot_completed").trim()))throw new IOException("系统仍在启动，请进入桌面后再试。");
            long uptime=Long.parseLong(shell.shell("cut -d. -f1 /proc/uptime").trim());
            if(uptime<90)throw new IOException("请等待开机满 90 秒后再恢复；约还需 "+(90-uptime)+" 秒。");
            String active=shell.shell("if pgrep -f 'anb_root.*/explo[i]t|ksu_loade[r].sh|gl_ex[p]loit' >/dev/null; then echo ACTIVE; else echo IDLE; fi").trim();
            if(!"IDLE".equals(active))throw new IOException("另一次恢复仍在运行。请等待；不要并发启动。");
            // Match the verified upstream driver: do not leave a pre-load manager alive.
            shell.shell("am force-stop "+KSU);
            record("KernelSU 管理器已在加载前停止；完成后将重新启动。");
            dir=ROOT+"/run_"+boot+"_"+System.currentTimeMillis()+"_"+UUID.randomUUID().toString().replace("-","");
            status(Phase.PREPARING,1,"准备本次恢复","校验内置文件并创建独立运行目录。");
            stage(shell);
            save(boot,"launching");
            String launch="set -e; mkdir "+q(ROOT+"/boot_"+boot+".lock")+"; cd "+q(dir)
                    +"; nohup env KSU_RUNDIR="+q(dir)+" "+q(dir+"/exploit")+" >gl.log 2>&1 </dev/null &";
            shell.shell(launch);
            status(Phase.RUNNING,2,"正在恢复权限","通常需要 1–3 分钟。请勿重复启动；若手机重启，解锁后重新启动 Shizuku 再检查。");
            save(boot,"running");monitor(shell,boot);
        }
    }
    private boolean hasModule(ShizukuShell s)throws Exception {
        return s.shell("if grep -q '^kernelsu ' /proc/modules; then echo YES; else echo NO; fi").trim().equals("YES");
    }
    private String snapshot(ShizukuShell s,String directory)throws Exception {
        return s.shell("cd "+q(directory)+"; if grep -q '^kernelsu ' /proc/modules; then echo MODULE=1; fi; "
                +"if grep -q '^KernelSU module loaded$' ksu_load.log 2>/dev/null && grep -q '^=== re-enable SELinux enforcing (last step) ===$' ksu_load.log 2>/dev/null; then echo FINISHED=1; fi; "
                +"printf 'ENFORCE='; getenforce; tail -n 5 ksu_load.log 2>/dev/null; tail -n 4 gl.log 2>/dev/null; true");
    }
    private boolean finished(ShizukuShell s,String directory)throws Exception {return RunStatus.isComplete(snapshot(s,directory));}
    private void monitor(ShizukuShell shell,String boot)throws Exception {
        long end=SystemClock.elapsedRealtime()+300000;String last="";
        while(SystemClock.elapsedRealtime()<end) {
            if(!boot.equals(shell.shell("settings get global boot_count").trim()))throw new IOException("手机已重启。请重新启动 Shizuku，然后返回检查。");
            String out=snapshot(shell,dir);
            if(!last.equals(out)){record(out);last=out;}
            if(out.contains("MODULE=1") && phase!=Phase.LOADING)
                status(Phase.LOADING,3,"正在完成加载","模块已出现，正在等待恢复收尾；尚未确认成功。");
            if(RunStatus.isComplete(out)){modulePresent=true;verify(shell,boot);return;}
            Thread.sleep(3000);
        }
        save(boot,"timeout");
        throw new IOException("等待超过 5 分钟，未确认完成。记录已保留；本次开机不会自动重跑，请重启后再试。");
    }
    private void verify(ShizukuShell shell,String boot)throws Exception {
        status(Phase.VERIFYING,4,"验证 Root 与保护状态","正在通过 KernelSU 检查本 App 的权限。");
        String enforce=shell.shell("getenforce").trim();
        if(!"Enforcing".equals(enforce))throw new IOException("KernelSU 已加载，但保护状态尚未恢复。未判定成功；请保存日志。");
        CommandRunner.Result result;
        try {result=CommandRunner.run(new String[]{"/system/bin/su","-c","id"},20000);}
        catch(IOException e){waitAuth(shell,boot);return;}
        if(result.timedOut || result.exitCode!=0 || !result.output.contains("uid=0(root)")){waitAuth(shell,boot);return;}
        record("ROOT_PROOF "+result.output.trim()+"\nSELinux="+enforce+"\nboot_count="+boot);
        save(boot,"verified");
        File ext=context.getExternalFilesDir(null);
        if(ext!=null)write(new File(ext,"verification.txt"),"boot_count="+boot+"\ntransport=Shizuku UserService\nkernel="+KERNEL+"\n"+result.output+"SELinux="+enforce+"\n");
        retireLegacyTcp();
        boolean manager=refreshManager(shell);
        status(Phase.SUCCESS,5,"Root 已验证",manager
                ?"KernelSU 管理器已重新启动。请确认首页显示“工作中”；本次开机有效，SELinux 为 Enforcing。"
                :"Root 与 Enforcing 已验证；管理器刷新未完成，请在页面点击“刷新 KernelSU”。无需重新恢复。");
    }
    private void retireLegacyTcp() {
        SharedPreferences migration=context.getSharedPreferences("migration",0);
        if(migration.getBoolean("legacy_tcp_retired",false))return;
        try {
            CommandRunner.Result result=CommandRunner.run(new String[]{"/system/bin/su","-c",
                    "setprop persist.adb.tcp.port -1 && setprop service.adb.tcp.port -1 && getprop persist.adb.tcp.port && getprop service.adb.tcp.port"},8000);
            if(result.exitCode==0 && result.output.trim().equals("-1\n-1")) {
                migration.edit().putBoolean("legacy_tcp_retired",true).apply();record("旧版固定 TCP 设置已清理；无线调试由 Shizuku 管理。");
            } else record("旧版 TCP 设置清理未确认；App 仍只使用 Shizuku，不会回退旧连接。");
        }catch(Exception e){record("旧版设置清理未完成："+friendly(e));}
    }
    private void waitAuth(ShizukuShell shell,String boot)throws Exception {
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
        if(checking||!busy.compareAndSet(false,true))return;
        new Thread(()->{
            try(ShizukuShell shell=ShizukuShell.open(context)) {
                boolean ok=refreshManager(shell);
                message=ok?"KernelSU 管理器已重新启动，请查看首页状态。没有重新运行提权。":"刷新未完成，请检查 Shizuku 或导出记录。没有重新运行提权。";
            }catch(Exception e){message=friendly(e);record(message);}
            finally{busy.set(false);}
        },"manager-refresh").start();
    }
    private void save(String boot,String result)throws IOException {
        if(!context.getSharedPreferences("run",0).edit().putString("boot",boot).putString("dir",dir==null?"":dir).putString("phase",result).commit())
            throw new IOException("无法保存本次运行记录，已停止。");
    }
    private void stage(ShizukuShell shell)throws Exception {
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
        String loader=read(new File(files,"ksu_loader.tmpl")).replace("@RUNDIR@",dir)
                .replace("@STAMP@",String.valueOf(System.currentTimeMillis())).replace("@FREEZE_HISECD@","1")
                .replace("@ALLOW_SHELL@","0").replace("@STAGES@","1").replace("@ENFORCE@","1");
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
    static void write(File file,String text)throws IOException {
        try(FileOutputStream out=new FileOutputStream(file,false)){out.write(text.getBytes(StandardCharsets.UTF_8));}
    }
    static String friendly(Exception e) {
        if(e instanceof android.os.DeadObjectException)return "Shizuku 执行服务已断开。请重新启动 Shizuku；返回后只检查原进度，不会重复运行。";
        String text=e.getMessage();return text==null?e.getClass().getSimpleName():text;
    }
}
