package piuk.blockchain.android.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import piuk.blockchain.android.R;

public class NotificationsUtil {

    private NotificationManager notificationManager;
    private Context context = null;

    public NotificationsUtil(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void setNotification(String title, String marquee, String text, int drawablePostLollipop, int drawablePreLollipop, Class cls, int id) {

        int drawableCompat = drawablePreLollipop;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            drawableCompat = drawablePostLollipop;

        Intent notifyIntent = new Intent(context, cls);
        PendingIntent intent = PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(drawableCompat)
                .setColor(ContextCompat.getColor(context, R.color.blockchain_blue))
                .setContentTitle(title)
                .setContentIntent(intent)
                .setWhen(System.currentTimeMillis())
                .setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.beep))
                .setTicker(marquee)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setContentText(text);

        notificationManager.notify(id, mBuilder.build());
    }
}
