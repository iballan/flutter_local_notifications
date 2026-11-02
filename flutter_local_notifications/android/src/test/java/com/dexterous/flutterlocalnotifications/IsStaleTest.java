package com.dexterous.flutterlocalnotifications;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dexterous.flutterlocalnotifications.models.DateTimeComponents;
import com.dexterous.flutterlocalnotifications.models.NotificationDetails;
import com.dexterous.flutterlocalnotifications.models.RepeatInterval;
import com.dexterous.flutterlocalnotifications.models.ScheduledNotificationRepeatFrequency;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;

public class IsStaleTest {
  private static final long NOW = 1_800_000_000_000L;
  private static final long GRACE = FlutterLocalNotificationsPlugin.STALE_GRACE_MS;
  private static final String ZONE = "Asia/Riyadh";

  private static NotificationDetails oneShot(long dueMs) {
    NotificationDetails d = new NotificationDetails();
    d.id = 1;
    d.millisecondsSinceEpoch = dueMs;
    return d;
  }

  private static NotificationDetails zoned(long dueMs) {
    NotificationDetails d = new NotificationDetails();
    d.id = 2;
    d.timeZoneName = ZONE;
    d.scheduledDateTime =
        Instant.ofEpochMilli(dueMs).atZone(ZoneId.of(ZONE)).toLocalDateTime().toString();
    return d;
  }

  @Test
  public void future_isNotStale() {
    assertFalse(FlutterLocalNotificationsPlugin.isStale(oneShot(NOW + 60_000), NOW, GRACE));
  }

  @Test
  public void lateWithinGrace_isNotStale() {
    assertFalse(FlutterLocalNotificationsPlugin.isStale(oneShot(NOW - GRACE + 1), NOW, GRACE));
    assertFalse(FlutterLocalNotificationsPlugin.isStale(oneShot(NOW - GRACE), NOW, GRACE));
  }

  @Test
  public void pastGrace_isStale() {
    assertTrue(FlutterLocalNotificationsPlugin.isStale(oneShot(NOW - GRACE - 1), NOW, GRACE));
    assertTrue(FlutterLocalNotificationsPlugin.isStale(oneShot(NOW - 3 * 3600_000L), NOW, GRACE));
  }

  @Test
  public void repeating_isNeverStale() {
    NotificationDetails interval = oneShot(NOW - 3 * 3600_000L);
    interval.repeatInterval = RepeatInterval.Daily;
    assertFalse(FlutterLocalNotificationsPlugin.isStale(interval, NOW, GRACE));

    NotificationDetails matching = zoned(NOW - 3 * 3600_000L);
    matching.matchDateTimeComponents = DateTimeComponents.Time;
    assertFalse(FlutterLocalNotificationsPlugin.isStale(matching, NOW, GRACE));

    NotificationDetails frequency = zoned(NOW - 3 * 3600_000L);
    frequency.scheduledNotificationRepeatFrequency = ScheduledNotificationRepeatFrequency.Daily;
    assertFalse(FlutterLocalNotificationsPlugin.isStale(frequency, NOW, GRACE));
  }

  @Test
  public void unknownTime_isNotStale() {
    assertFalse(FlutterLocalNotificationsPlugin.isStale(new NotificationDetails(), NOW, GRACE));

    NotificationDetails zonedNoTime = new NotificationDetails();
    zonedNoTime.timeZoneName = ZONE;
    assertFalse(FlutterLocalNotificationsPlugin.isStale(zonedNoTime, NOW, GRACE));

    NotificationDetails badZone = zoned(NOW - 3 * 3600_000L);
    badZone.timeZoneName = "Not/AZone";
    assertFalse(FlutterLocalNotificationsPlugin.isStale(badZone, NOW, GRACE));
  }

  @Test
  public void zoned_usesScheduledDateTimeInItsZone() {
    assertFalse(FlutterLocalNotificationsPlugin.isStale(zoned(NOW + 60_000), NOW, GRACE));
    assertFalse(FlutterLocalNotificationsPlugin.isStale(zoned(NOW - 60_000), NOW, GRACE));
    assertTrue(FlutterLocalNotificationsPlugin.isStale(zoned(NOW - GRACE - 1000), NOW, GRACE));
    // A zoned entry ignores a stale millisecondsSinceEpoch left in the cache.
    NotificationDetails future = zoned(NOW + 60_000);
    future.millisecondsSinceEpoch = NOW - 3 * 3600_000L;
    assertFalse(FlutterLocalNotificationsPlugin.isStale(future, NOW, GRACE));
  }
}
