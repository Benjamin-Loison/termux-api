package com.termux.api.apis;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

public class NotificationSendIntentAPI {

    private static final String LOG_TAG = "NotificationSendIntentAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

		try {
			sendIntent(intent);
		} catch (Exception exception) {
			Logger.logDebug(LOG_TAG, "onNotificationSendIntent error default");
		}
		ResultReturner.returnData(apiReceiver, intent, out -> {});
    }


    static void sendIntent(Intent intent) throws Exception {
        NotificationListAPI.NotificationService notificationService = NotificationListAPI.NotificationService.get();
        StatusBarNotification[] notifications = notificationService.getActiveNotifications();
		String wantedNotificationPackageName = intent.getStringExtra("package-name");
		int wantedNotificationid = intent.getIntExtra("notification-id", -1);

        for (StatusBarNotification n : notifications) {
			if (n.getPackageName().equals(wantedNotificationPackageName) && n.getId() == wantedNotificationid) {
				Notification.Action[] actions = n.getNotification().actions;
				String wantedNotificationActionName = intent.getStringExtra("action-name");

				// Can happen, see https://codeberg.org/Benjamin_Loison/Voice_assistant/issues/83#issuecomment-25262110
				if (actions != null) {
					for (Notification.Action action : actions) {
						if (wantedNotificationActionName.contentEquals(action.title)) {
							action.actionIntent.send();
							return;
						}
					}
				}
			}
        }
    }
}
