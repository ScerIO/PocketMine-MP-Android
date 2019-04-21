package io.scer.pocketmine

import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.app.Application
import androidx.annotation.RequiresApi

const val CHANNEL_ID = "pocketmine_service_channel"

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Pocketmine Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager!!.createNotificationChannel(serviceChannel)
    }
}