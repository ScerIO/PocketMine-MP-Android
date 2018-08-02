package io.scer.pocketmine;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class ServerService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(getApplicationContext(), MainActivity.class), 0);

        startForeground(
                1,
                new NotificationCompat.Builder(getApplicationContext())
                        .setOngoing(true)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntent).build());
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