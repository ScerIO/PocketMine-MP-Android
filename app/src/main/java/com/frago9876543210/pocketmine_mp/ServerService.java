package com.frago9876543210.pocketmine_mp;

import android.os.*;
import android.app.*;
import android.content.*;
import android.support.v4.app.NotificationCompat;

public class ServerService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, new NotificationCompat.Builder(getApplicationContext()).setOngoing(true).setSmallIcon(R.mipmap.ic_launcher).setContentIntent(PendingIntent.getActivity(this, 0, new Intent(getApplicationContext(), MainActivity.class), 0)).build());
        ServerUtils.run();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        ServerUtils.kill();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}