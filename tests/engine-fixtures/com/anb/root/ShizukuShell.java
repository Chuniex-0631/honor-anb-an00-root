package com.anb.root;
import java.io.*;import java.util.*;import java.util.concurrent.*;
public class ShizukuShell implements AutoCloseable {
 static int launches;static boolean module,locked,permissive,blockProbe,badCleanup,badStages;static long uptime=120;static CountDownLatch entered,release;static final List<String> commands=Collections.synchronizedList(new ArrayList<>());
 static void reset(){launches=0;module=locked=permissive=blockProbe=badCleanup=badStages=false;uptime=120;entered=new CountDownLatch(1);release=new CountDownLatch(1);commands.clear();}
 public static ShizukuShell open(android.content.Context c){return new ShizukuShell();}
 public String shell(String command)throws Exception {
  commands.add(command);
  if(command.startsWith("id;")){if(blockProbe){entered.countDown();if(!release.await(5,TimeUnit.SECONDS))throw new IOException("probe fixture timed out");}return "uid=2000(shell) gid=2000(shell) context=u:r:shell:s0\nSeccomp: 0\n";}
  if(command.startsWith("getprop ro.product.model;"))return DeviceProfile.MODEL+"\n35\n"+DeviceProfile.DISPLAY+"\n"+DeviceProfile.KERNEL+"\n";
  if(command.equals("settings get global boot_count"))return "88";
  if(command.startsWith("if grep -q '^kernelsu '"))return module?"YES":"NO";
  if(command.startsWith("cd ")){if(!module)throw new IOException("simulation: monitor stopped");return "ANB_STATUS=2\nMODULE=1\nFINISHED=1\nENFORCE=Enforcing\nCLEANUP="+(badCleanup?"0":"1")+"\nSTAGES="+(badStages?"0":"1")+"\nDETAILS\n";}
  if(command.startsWith("if [ -e"))return locked?"LOCKED":"CLEAR";
  if(command.equals("getprop sys.boot_completed"))return "1";
  if(command.startsWith("cut -d."))return Long.toString(uptime);
  if(command.startsWith("if pgrep"))return "IDLE";
  if(command.contains("nohup env")){launches++;throw new IOException("simulation: launch captured, never executed");}
  if(command.equals("getenforce"))return permissive?"Permissive":"Enforcing";
  if(command.startsWith("cmd package resolve-activity"))return "me.weishu.kernelsu/.ui.MainActivity";
  return "";
 }
 public static String quote(String s){return "'"+s.replace("'","'\"'\"'")+"'";}
 public void close(){}
}