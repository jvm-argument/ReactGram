/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import org.telegram.ui.LaunchActivity;

public class NotificationsService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationLoader.postInitApplication();
        startKeepAliveForeground();
    }

    private boolean foreground;

    // ReactGram: keep the service in the foreground with a permanent notification (feature from AyuGram's push service)
    private void startKeepAliveForeground() {
        if (!com.reactgram.features.FeatureConfig.keepAlive() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        try {
            final String channelId = "reactgram_push";
            final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            final NotificationChannel channel = new NotificationChannel(channelId, "ReactGram", NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
            final PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, LaunchActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            final Notification notification = new Notification.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(LocaleController.getString(R.string.ReactGramKeepAliveTitle))
                .setContentText(LocaleController.getString(R.string.ReactGramKeepAliveText))
                .setContentIntent(open)
                .setOngoing(true)
                .build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(7237, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(7237, notification);
            }
            foreground = true;
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        if (foreground) {
            stopForeground(true);
        }
        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        if (preferences.getBoolean("pushService", true)) {
            Intent intent = new Intent("org.telegram.start");
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        }
    }
}
