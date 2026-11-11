package com.anb.root;
public class CommandRunner {
 public static boolean denied,dropModule,dropEnforcement,badProtection;
 public static class Result {public int exitCode;public String output;public boolean timedOut,truncated,incomplete;}
 public static Result run(String[] c,int timeout)throws Exception {
  if(dropModule)ShizukuShell.module=false;
  if(dropEnforcement)ShizukuShell.permissive=true;
  Result r=new Result();r.exitCode=denied?1:0;
  r.output=denied?"Permission denied":"uid=0(root) gid=0(root) context=u:r:ksu:s0\nANB_PROTECTION="+(badProtection?"0":"1")+"\n";
  return r;
 }
}
