package com.fimbleenterprises.torquepidcaster.presentation.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.receiver.MyTorqueBroadcastReceiver

class NotificationUtil {

    var NOTIFICATION_CHANNEL = "PIDCASTER_NOTIF_CHANNEL"
    var context: Context? = null
    var manager: NotificationManager? = null
    var notification: Notification? = null
    val START_ID = 1

    fun Notifications(context: Context) {
        this.context = context
        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    fun create(
        title: String?,
        text: String?,
        showProgress: Boolean,
        pClass: Class<*>?
    ): Notification? {
        (context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL,
                Context.NOTIFICATION_SERVICE,
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        val newIntent = Intent(context, pClass)

        // The PendingIntent to launch our activity if the user selects this notification
        val contentIntent = PendingIntent.getActivity(
            context,
            0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, MyTorqueBroadcastReceiver::class.java)
        snoozeIntent.action = "STOP_PIDCASTER"
        snoozeIntent.putExtra(Notification.EXTRA_NOTIFICATION_ID, 0)
        val snoozePendingIntent = PendingIntent.getBroadcast(context, 0, snoozeIntent, 0)

        val style = NotificationCompat.BigTextStyle()
            .bigText(text)
        notification = NotificationCompat.Builder(context!!, NOTIFICATION_CHANNEL)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true) // so when data is updated don't make sound and alert in android 8.0+
            .setOngoing(false)
            .setStyle(style)
            .setProgress(0, 0, showProgress)
            .setSmallIcon(R.drawable.iconplay)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_media_pause, context!!.getString(R.string.stop),
                snoozePendingIntent)
            .setLargeIcon(BitmapFactory.decodeResource(context!!.resources, R.mipmap.ic_launcher))
            .build()
        return notification
    }

    fun setAutoCancel(delayInMs: Int) {
        val h = Handler()
        h.postDelayed({ manager!!.cancel(START_ID) }, delayInMs.toLong())
    }

    fun cancel() {
        manager!!.cancel(START_ID)
    }

    fun update(notification: Notification?) {
        manager!!.notify(START_ID, notification)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun update(title: String?, msg: String?, showProgress: Boolean, pclass: Class<*>?) {
        val notif = create(title, msg, showProgress, pclass)
        manager!!.notify(START_ID, notif)
    }

    fun show() {
        manager!!.notify(START_ID, notification)
    }

}