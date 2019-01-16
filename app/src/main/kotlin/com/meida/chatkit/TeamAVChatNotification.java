package com.meida.chatkit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.meida.freedconn.NetworkChatActivity;
import com.netease.nim.avchatkit.AVChatKit;
import com.meida.freedconn.R;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

/**
 * 群视频聊天后台时显示通知栏
 */
public class TeamAVChatNotification {

    private Context context;

    private NotificationManager notificationManager;
    private Notification callingNotification;
    private String roomName;
    private static final int CALLING_NOTIFY_ID = 111;
    private static final String CHANNEL_ONE_ID = "com.ruanmeng.chatkit.teamchatnotification";
    private static final String CHANNEL_ONE_NAME = "TeamAVChatNotification";

    public TeamAVChatNotification(Context context) {
        this.context = context;
    }

    public void init(String roomName) {
        this.roomName = roomName;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void buildCallingNotification() {
        if (callingNotification == null) {
            Intent localIntent = new Intent();
            localIntent.setClass(context, NetworkChatActivity.class);
            localIntent.putExtra("roomName", roomName);
            localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            String tickerText = context.getString(R.string.network_chat_notification);
            int iconId = AVChatKit.getAvChatOptions().notificationIconRes;

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    CALLING_NOTIFY_ID,
                    localIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            callingNotification = makeNotification(
                    pendingIntent,
                    context.getString(R.string.avchat_call),
                    tickerText,
                    tickerText,
                    iconId,
                    false,
                    false);
            callingNotification.flags |= Notification.FLAG_NO_CLEAR;
        }
    }

    private Notification makeNotification(PendingIntent pendingIntent, String title, String content, String tickerText,
                                          int iconId, boolean ring, boolean vibrate) {

        NotificationChannel notificationChannel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID, CHANNEL_ONE_NAME, IMPORTANCE_HIGH);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setSound(null, null);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentTitle(title)
                .setContentText(content)
                .setTicker(content)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setTicker(tickerText)
                .setLargeIcon(largeIconId())
                .setSmallIcon(iconId);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ONE_ID);
        }
        int defaults = Notification.DEFAULT_LIGHTS;
        if (vibrate) defaults |= Notification.DEFAULT_VIBRATE;
        if (ring) defaults |= Notification.DEFAULT_SOUND;
        builder.setDefaults(defaults);

        return builder.build();
    }

    private Bitmap largeIconId() {
        PackageManager pm = context.getPackageManager();
        Drawable drawable = context.getApplicationInfo().loadIcon(pm);
        if (drawable == null) return null;
        return drawable instanceof BitmapDrawable ? ((BitmapDrawable) drawable).getBitmap() : null;
    }

    public void activeCallingNotification(boolean active) {
        if (notificationManager != null) {
            if (active) {
                buildCallingNotification();
                notificationManager.notify(CALLING_NOTIFY_ID, callingNotification);
                AVChatKit.getNotifications().put(CALLING_NOTIFY_ID, callingNotification);
            } else {
                notificationManager.cancel(CALLING_NOTIFY_ID);
                AVChatKit.getNotifications().remove(CALLING_NOTIFY_ID);
            }
        }
    }
}
