package com.anb.root;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;

/** Created only by Shizuku. Not an exported Android service. */
public final class ShellUserService extends IShellService.Stub {
    private final int appUid;
    public ShellUserService(Context context) { appUid = context.getApplicationInfo().uid; }
    private void enforceCaller() {
        int caller = Binder.getCallingUid();
        if(caller != appUid && caller != 2000 && caller != 0)
            throw new SecurityException("Unexpected caller");
    }
    @Override public synchronized Bundle execute(String command,int timeoutMs) {
        enforceCaller();
        Bundle reply = new Bundle();
        try {
            if(command==null || command.length()>24000) throw new IllegalArgumentException("Invalid command");
            CommandRunner.Result r = CommandRunner.run(new String[]{"/system/bin/sh","-c",command},
                    Math.max(1000,Math.min(30000,timeoutMs)));
            reply.putInt("exit",r.exitCode);reply.putString("output",r.output);
            reply.putBoolean("timeout",r.timedOut);reply.putBoolean("truncated",r.truncated);
        } catch(Exception e) {
            reply.putInt("exit",-1);reply.putString("output",e.toString());
        }
        return reply;
    }
    @Override public void destroy() { enforceCaller(); System.exit(0); }
}
