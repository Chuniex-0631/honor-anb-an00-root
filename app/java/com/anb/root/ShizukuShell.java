package com.anb.root;

import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import rikka.shizuku.Shizuku;

public final class ShizukuShell implements AutoCloseable {
    private volatile IShellService service;
    private final AtomicBoolean closed=new AtomicBoolean();
    private final CountDownLatch connected = new CountDownLatch(1);
    private final Shizuku.UserServiceArgs args;
    private final ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name,IBinder binder) {
            if(closed.get()){unbind();connected.countDown();return;}
            service=IShellService.Stub.asInterface(binder);connected.countDown();
        }
        public void onServiceDisconnected(ComponentName name) { service=null;connected.countDown(); }
    };
    private ShizukuShell(Context context) {
        args=new Shizuku.UserServiceArgs(new ComponentName(context,ShellUserService.class))
                .tag("anb-shell-v2").version(10).daemon(true).processNameSuffix("shell");
    }
    public static ShizukuShell open(Context context) throws Exception {
        if(Looper.myLooper()==Looper.getMainLooper()) throw new IllegalStateException("Use a worker thread");
        if(!Shizuku.pingBinder()) throw new IOException("Shizuku 已停止，请先通过无线调试启动");
        if(Shizuku.getVersion()<13) throw new IOException("请更新 Shizuku 到 13 或更新版本");
        if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED)
            throw new IOException("尚未授予 Shizuku 权限");
        if(Shizuku.getUid()!=2000) throw new IOException("请在 Shizuku 中通过无线调试启动 shell 服务");
        ShizukuShell shell=new ShizukuShell(context);
        Handler main=new Handler(Looper.getMainLooper());
        main.post(() -> {
            if(shell.closed.get()){shell.connected.countDown();return;}
            try { Shizuku.bindUserService(shell.args,shell.connection); }
            catch(Exception e) { shell.connected.countDown(); }
        });
        try {
            if(!shell.connected.await(15,TimeUnit.SECONDS) || shell.service==null) {
                shell.close();throw new IOException("未能连接执行服务，请重新启动 Shizuku 后重试");
            }
        }catch(InterruptedException e){shell.close();Thread.currentThread().interrupt();throw e;}
        return shell;
    }
    public synchronized String shell(String command) throws Exception {
        IShellService current=service;
        if(closed.get() || current==null || !current.asBinder().isBinderAlive()) throw new IOException("Shizuku 执行服务已断开");
        Bundle result=current.execute(command,15000);
        String output=result.getString("output","");
        if(result.getBoolean("timeout")) throw new IOException("命令超时；未自动重试");
        if(result.getBoolean("truncated")) throw new IOException("命令输出超出限制");
        if(result.getBoolean("incomplete")) throw new IOException("命令输出未完整读取；未判定成功");
        if(result.getInt("exit",-1)!=0) throw new IOException("执行失败："+output.trim());
        return output;
    }
    public static String quote(String value) { return "'"+value.replace("'","'\"'\"'")+"'"; }
    public void close() {
        if(!closed.compareAndSet(false,true))return;
        service=null;unbind();
    }
    private void unbind() {
        new Handler(Looper.getMainLooper()).post(() -> {
            try { if(Shizuku.pingBinder())Shizuku.unbindUserService(args,connection,false); }
            catch(Exception ignored) { }
        });
    }
}
