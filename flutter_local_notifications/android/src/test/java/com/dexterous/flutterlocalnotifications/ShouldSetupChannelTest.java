package com.dexterous.flutterlocalnotifications;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dexterous.flutterlocalnotifications.models.NotificationChannelAction;
import org.junit.Test;

public class ShouldSetupChannelTest {
  @Test
  public void aMissingChannelIsCreatedWhateverTheAction() {
    assertTrue(FlutterLocalNotificationsPlugin.shouldSetupChannel(false, null));
    assertTrue(
        FlutterLocalNotificationsPlugin.shouldSetupChannel(
            false, NotificationChannelAction.CreateIfNotExists));
    assertTrue(
        FlutterLocalNotificationsPlugin.shouldSetupChannel(false, NotificationChannelAction.Update));
  }

  @Test
  public void anExistingChannelIsOnlyTouchedForUpdate() {
    assertTrue(
        FlutterLocalNotificationsPlugin.shouldSetupChannel(true, NotificationChannelAction.Update));
    assertFalse(
        FlutterLocalNotificationsPlugin.shouldSetupChannel(
            true, NotificationChannelAction.CreateIfNotExists));
    assertFalse(FlutterLocalNotificationsPlugin.shouldSetupChannel(true, null));
  }
}
