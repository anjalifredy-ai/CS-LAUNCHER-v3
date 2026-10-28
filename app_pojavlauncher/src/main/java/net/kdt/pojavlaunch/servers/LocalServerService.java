package net.kdt.pojavlaunch.servers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;

/**
 * Keeps the process alive while a local dedicated server is running.
 */
public class LocalServerService extends Service {

    public static final String CHANNEL_ID = "cs_local_server";
    public static final int NOTIF_ID = 7101;

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_PORT = "port";

    public static void start(Context ctx, String name, int port) {
        Intent i = new Intent(ctx, LocalServerService.class);
        i.putExtra(EXTRA_NAME, name);
        i.putExtra(EXTRA_PORT, port);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }

    public static void stop(Context ctx) {
        if (ctx == null) return;
        try {
            ctx.stopService(new Intent(ctx, LocalServerService.class));
        } catch (Throwable ignored) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String name = intent != null ? intent.getStringExtra(EXTRA_NAME) : "Server";
        int port = intent != null ? intent.getIntExtra(EXTRA_PORT, 25565) : 25565;
        ensureChannel();

        Intent open = new Intent(this, LauncherActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int icon = android.R.drawable.stat_sys_data_share;
        try {
            // Prefer app icon if available
            icon = R.drawable.ic_menu_play_once;
        } catch (Throwable ignored) {}

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(name != null ? name : "CS Server")
                .setContentText("Port " + port + " · keep open · tunnel for distant friends")
                .setSmallIcon(icon)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(NOTIF_ID, n);
        return START_STICKY;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Local Minecraft Server",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("On-device dedicated server is running");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
