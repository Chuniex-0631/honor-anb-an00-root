package com.anb.root;
import android.app.*;
import android.content.*;
import android.os.*;

/** Keeps the visible, user-started recovery monitor alive while switching apps. */
public final class RecoveryService extends Service {
    private static final String CHANNEL="recovery";
    private final Handler main=new Handler(Looper.getMainLooper());
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Root 恢复进度",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("显示由你启动的恢复与验证进度");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    private Notification notification(String title) {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(title).setContentText("点击回到 ANB Root 查看进度")
                .setContentIntent(open).setOngoing(true).build();
    }
    public int onStartCommand(Intent intent,int flags,int startId) {
        startForeground(12,notification("正在检查恢复状态"));
        boolean accepted=RecoveryEngine.get(this).start(
                text->main.post(()->getSystemService(NotificationManager.class).notify(12,notification(text))),
                ()->main.post(()->{stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}));
        if(!accepted){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
        return START_NOT_STICKY;
    }
    public IBinder onBind(Intent intent){return null;}
}
