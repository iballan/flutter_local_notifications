package com.dexterous.flutterlocalnotifications;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Keep;
import androidx.core.app.NotificationManagerCompat;

import com.dexterous.flutterlocalnotifications.models.NotificationDetails;
import com.dexterous.flutterlocalnotifications.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/** Created by michaelbui on 24/3/18. */
@Keep
public class ScheduledNotificationReceiver extends BroadcastReceiver {

  private static final String TAG = "ScheduledNotificationReceiver";

  @Override
  @SuppressWarnings("deprecation")
  @CallSuper
  public void onReceive(final Context context, Intent intent) {
    String notificationDetailsJson =
        intent.getStringExtra(FlutterLocalNotificationsPlugin.NOTIFICATION_DETAILS);
    if (StringUtils.isNullOrEmpty(notificationDetailsJson)) {
      // This logic is needed for apps that used the plugin prior to 0.3.4

      Notification notification;
      int notificationId = intent.getIntExtra("notification_id", 0);

      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        notification = intent.getParcelableExtra("notification", Notification.class);
      } else {
        notification = intent.getParcelableExtra("notification");
      }

      if (notification == null) {
        // This means the notification is corrupt
        FlutterLocalNotificationsPlugin.removeNotificationFromCache(context, notificationId);
        Log.e(TAG, "Failed to parse a notification from  Intent. ID: " + notificationId);
        return;
      }

      notification.when = System.currentTimeMillis();
      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
      notificationManager.notify(notificationId, notification);
      boolean repeat = intent.getBooleanExtra("repeat", false);
      if (!repeat) {
        FlutterLocalNotificationsPlugin.removeNotificationFromCache(context, notificationId);
      }
    } else {
      Gson gson = FlutterLocalNotificationsPlugin.buildGson();
      Type type = new TypeToken<NotificationDetails>() {}.getType();
      NotificationDetails notificationDetails = gson.fromJson(notificationDetailsJson, type);

      if (FlutterLocalNotificationsPlugin.isStale(
          notificationDetails,
          System.currentTimeMillis(),
          FlutterLocalNotificationsPlugin.STALE_GRACE_MS)) {
        // Delivered far too late (clock jumped forward, or a very late OEM delivery): drop it.
        FlutterLocalNotificationsPlugin.removeNotificationFromCache(context, notificationDetails.id);
        Log.w(TAG, "Dropped stale notification " + notificationDetails.id);
        return;
      }

      if (shouldShowNotification(context, notificationDetails)) {
        FlutterLocalNotificationsPlugin.showNotification(context, notificationDetails);
      }
      FlutterLocalNotificationsPlugin.scheduleNextNotification(context, notificationDetails);
    }
  }

  /**
   * The notification the plugin would post for {@code notificationDetails} (creating its channel
   * if needed), for a subclass that posts it itself, e.g. as a foreground service's notification.
   * Tapping it reaches Dart exactly like a plugin-posted one.
   */
  protected static Notification buildNotification(
      Context context, NotificationDetails notificationDetails) {
    return FlutterLocalNotificationsPlugin.createNotification(context, notificationDetails);
  }

  /** A subclass can handle an alarm natively while retaining normal one-shot cache cleanup. */
  protected boolean shouldShowNotification(Context context, NotificationDetails notificationDetails) {
    return true;
  }
}
