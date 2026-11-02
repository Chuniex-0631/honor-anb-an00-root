package com.anb.root;
import java.io.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.concurrent.*;import android.content.*;
public class EngineIntegrationTest {
 static File state,assets;static int index;
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
 static RecoveryEngine fresh()throws Exception {Field f=RecoveryEngine.class.getDeclaredField("instance");f.setAccessible(true);f.set(null,null);ShizukuShell.reset();android.os.Build.DISPLAY=DeviceProfile.DISPLAY;Context c=new Context(new File(state,"case-"+(index++)),assets);c.getSharedPreferences("setup",0).edit().putBoolean("completed",true).commit();return RecoveryEngine.get(c);}
 static void run(RecoveryEngine e,boolean mayLaunch)throws Exception {CountDownLatch done=new CountDownLatch(1);check(e.start(s->{},done::countDown,mayLaunch),"request accepted");check(done.await(8,TimeUnit.SECONDS),"task completed");check(!e.isBusy(),"task released");}
 public static void main(String[] args)throws Exception {
  assets=new File(args[0]);state=Files.createTempDirectory(new File(args[1]).toPath(),"engine-").toFile();String old=System.getProperty("os.version");System.setProperty("os.version",DeviceProfile.KERNEL);
  try {
   RecoveryEngine e=fresh();run(e,false);check(e.failureKind==FailureText.Kind.PREPARE&&ShizukuShell.launches==0,"verification-only never starts a new chain");
   e=fresh();android.os.Build.DISPLAY="other firmware";run(e,true);check(e.failureKind==FailureText.Kind.COMPATIBILITY&&ShizukuShell.commands.isEmpty(),"wrong firmware denied before shell work");
   e=fresh();ShizukuShell.uptime=40;run(e,true);check(e.failureKind==FailureText.Kind.BOOT_WAIT&&ShizukuShell.launches==0,"boot wait before any launch");ShizukuShell.uptime=120;run(e,true);check(ShizukuShell.launches==1,"boot wait retry can start once");run(e,true);check(ShizukuShell.launches==1,"existing attempt never launches twice");
   e=fresh();ShizukuShell.locked=true;run(e,true);check(e.failureKind==FailureText.Kind.REBOOT_REQUIRED&&ShizukuShell.launches==0,"lost state plus boot lock denies rerun");
   e=fresh();ShizukuShell.module=true;ShizukuShell.permissive=true;run(e,false);check(e.failureKind==FailureText.Kind.SECURITY_NOT_RESTORED&&ShizukuShell.launches==0,"security failure typed and no launch");
   e=fresh();ShizukuShell.module=true;String dir="/data/local/tmp/anb_root_runs/run_88_1000_0123456789abcdef0123456789abcdef";e.context.getSharedPreferences("run",0).edit().putString("boot","88").putString("dir",dir).commit();run(e,false);check(e.phase==RecoveryEngine.Phase.SUCCESS&&dir.equals(e.context.getSharedPreferences("run",0).getString("dir","")),"verification retains the owned run directory");
   e=fresh();ShizukuShell.blockProbe=true;e.checkEnvironment();check(ShizukuShell.entered.await(3,TimeUnit.SECONDS),"probe entered");check(e.isChecking()&&!e.start(s->{},()->{},true)&&!e.selectAllowShell(true),"checking excludes recovery and options");ShizukuShell.release.countDown();long end=System.nanoTime()+3_000_000_000L;while(e.isBusy()&&System.nanoTime()<end)Thread.sleep(10);check(!e.isBusy(),"probe lease released");
   System.out.println("PASS: production engine with simulated shell: firmware gate, verification-only, boot wait/retry, no duplicate launch, lost-state lock, security failure, directory retention, shared exclusion");
  }finally{System.setProperty("os.version",old);}
 }
}