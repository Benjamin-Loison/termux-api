package com.termux.api.apis;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.JsonWriter;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.api.util.ResultReturner.ResultJsonWriter;
import com.termux.shared.logger.Logger;

import android.media.session.MediaSessionManager;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.media.session.MediaController;
import java.util.List;
import android.content.ComponentName;

import java.io.StringWriter;
import java.util.Set;
import java.io.FileWriter;
import com.termux.shared.termux.TermuxConstants;

public class NotificationListAPI {

    private static final String LOG_TAG = "NotificationListAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

		if (Set.of("socket_input", "socket_output", "api_method").containsAll(intent.getExtras().keySet())) {
			ResultReturner.returnData(apiReceiver, intent, new ResultJsonWriter() {
				@Override
				public void writeJson(JsonWriter out) {
					try {
						listNotifications(context, out);
					} catch (Exception exception) {
                        Logger.logDebug(LOG_TAG, "onNotificationPosted error default");
                    }
                }
            });
		} else if (intent.hasExtra("media")) {
			ResultReturner.returnData(apiReceiver, intent, new ResultJsonWriter() {
				@Override
				public void writeJson(JsonWriter out) {
					try {
						listMedias(context, out);
					} catch (Exception exception) {
						Logger.logDebug(LOG_TAG, "onNotificationPosted error media");
					}
				}
			});
		} else if (intent.hasExtra("start-listen")) {
            NotificationService.listening = true;
			ResultReturner.returnData(apiReceiver, intent, out -> {});
		} else if (intent.hasExtra("stop-listen")) {
			NotificationService.listening = false;
			ResultReturner.returnData(apiReceiver, intent, out -> {});
		}
    }


    static void listNotifications(Context context, JsonWriter out) throws Exception {
        NotificationService notificationService = NotificationService.get();
        StatusBarNotification[] notifications = notificationService.getActiveNotifications();

        out.beginArray();
        for (StatusBarNotification n : notifications) {
			writeStatusBarNotification(n, out);
        }
        out.endArray();
    }

	static void writeStatusBarNotification(StatusBarNotification n, JsonWriter out) throws Exception {
		int id = n.getId();
		String key = "";
		String title = "";
		String text = "";
		CharSequence[] lines = null;
		String packageName = "";
		String tag = "";
		String group = "";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String when = dateFormat.format(new Date(n.getNotification().when));

		if (n.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE) != null) {
			title = n.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString();
		}
		if (n.getNotification().extras.getCharSequence(Notification.EXTRA_BIG_TEXT) != null) {
			text = n.getNotification().extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString();
		} else if (n.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT) != null) {
			text = n.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT).toString();
		}
		if (n.getNotification().extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) != null) {
			lines = n.getNotification().extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
		}
		if (n.getTag() != null) {
			tag = n.getTag();
		}
		if (n.getNotification().getGroup() != null) {
			group = n.getNotification().getGroup();
		}
		if (n.getKey() != null) {
			key = n.getKey();
		}
		if (n.getPackageName() != null) {
			packageName = n.getPackageName();
		}
		out.beginObject()
				.name("id").value(id)
				.name("tag").value(tag)
				.name("key").value(key)
				.name("group").value(group)
				.name("packageName").value(packageName)
				.name("title").value(title)
				.name("content").value(text)
				.name("when").value(when);
		if (lines != null) {
			out.name("lines").beginArray();
			for (CharSequence line : lines) {
				out.value(line.toString());
			}
			out.endArray();
		}
		out.endObject();
	}

    static void listMedias(Context context, JsonWriter out) throws Exception {
        MediaSessionManager mediaSessionManager = (MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE);

        ComponentName listenerComponent = new ComponentName(NotificationService.get(), NotificationService.class);

        List<MediaController> controllers = mediaSessionManager.getActiveSessions(listenerComponent);

        out.beginArray();
        for (MediaController controller : controllers) {
            MediaMetadata metadata = controller.getMetadata();
            PlaybackState state = controller.getPlaybackState();

            if (metadata != null) {
                out.beginObject()
                    .name("packageName").value(controller.getPackageName())
                    .name("state").value(getStateString(state.getState()))
                    .name("title").value(metadata.getString(MediaMetadata.METADATA_KEY_TITLE))
                    .name("artist").value(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST))
                    .name("duration").value(metadata.getLong(MediaMetadata.METADATA_KEY_DURATION))
                    .name("bufferedPosition").value(state.getBufferedPosition())
                    .name("lastPositionUpdateTime").value(state.getLastPositionUpdateTime())
                    .name("playbackSpeed").value(state.getPlaybackSpeed())
                    .name("position").value(state.getPosition());
                out.endObject();
            }
        }
        out.endArray();
    }

	static String getStateString(int state) {
		switch(state) {
			case PlaybackState.STATE_BUFFERING:
				return "buffering";
			case PlaybackState.STATE_CONNECTING:
                return "connecting";
			case PlaybackState.STATE_ERROR:
                return "error";
			case PlaybackState.STATE_FAST_FORWARDING:
                return "fast_forwarding";
			case PlaybackState.STATE_NONE:
                return "none";
			case PlaybackState.STATE_PAUSED:
                return "paused";
			case PlaybackState.STATE_PLAYING:
                return "playing";
			case PlaybackState.STATE_REWINDING:
                return "rewinding";
			case PlaybackState.STATE_SKIPPING_TO_NEXT:
                return "skipping_to_next";
			case PlaybackState.STATE_SKIPPING_TO_PREVIOUS:
                return "skipping_to_previous";
            case PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM:
                return "skipping_to_queue_item";
			case PlaybackState.STATE_STOPPED:
                return "stopped";
			default:
				return "unknown";
		}
	}

    public static class NotificationService extends NotificationListenerService {
        static NotificationService _this;
		static boolean listening;

        public static NotificationService get() {
            return _this;
        }

        @Override
        public void onListenerConnected() {
			Logger.logDebug(LOG_TAG, "onListenerConnected");
            _this = this;
			listening = false;
        }

        @Override
        public void onListenerDisconnected() {
			Logger.logDebug(LOG_TAG, "onListenerDisconnected");
            _this = null;
        }

		@Override
        public void onNotificationPosted(StatusBarNotification statusBarNotification) {
			Logger.logDebug(LOG_TAG, "onNotificationPosted");
			if (listening) {
				Logger.logDebug(LOG_TAG, "onNotificationPosted listening");
				StringWriter stringWriter = new StringWriter();
				JsonWriter jsonWriter = new JsonWriter(stringWriter);
				try {
					writeStatusBarNotification(statusBarNotification, jsonWriter);
					FileWriter fileWriter = new FileWriter("/data/data/" + TermuxConstants.TERMUX_PACKAGE_NAME + "/files/home/termux-notification-list_listen.json");
					fileWriter.write(stringWriter.toString() + "\n");
					fileWriter.close();
				} catch(Exception exception) {
					exception.printStackTrace();
                }
			}
        }
    }

}
