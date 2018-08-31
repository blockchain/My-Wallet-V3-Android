package com.blockchain.notifications.models;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import timber.log.Timber;

public class NotificationPayload {

    private String title;
    private String body;
    private NotificationData data;

    public NotificationPayload(Map<String, String> map) {
        if (map.containsKey("title")) {
            title = map.get("title");
        }

        if (map.containsKey("body")) {
            body = map.get("body");
        }

        if (map.containsKey("data")) {
            data = new NotificationData(map.get("data"));
        }
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getBody() {
        return body;
    }

    @Nullable
    public String getAddress() {
        return data != null ? data.getAddress() : null;
    }

    @Nullable
    public NotificationType getType() {
        return data != null ? data.getType() : null;
    }

    @Nullable
    public String getMdid() {
        return data != null ? data.getMdid() : null;
    }

    private static class NotificationData {

        private String mdid;
        private NotificationType type;
        private String address;

        NotificationData(String data) {
            try {
                JSONObject jsonObject = new JSONObject(data);
                if (jsonObject.has("id")) {
                    mdid = jsonObject.getString("id");
                }

                if (jsonObject.has("type")) {
                    type = NotificationType.fromString(jsonObject.getString("type"));
                }

                if (jsonObject.has("address")) {
                    address = jsonObject.getString("address");
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Nullable
        public String getMdid() {
            return mdid;
        }

        @Nullable
        public NotificationType getType() {
            return type;
        }

        @Nullable
        public String getAddress() {
            return address;
        }
    }

    public enum NotificationType {
        PAYMENT("payment"),
        CONTACT_REQUEST("contact_request");

        private String name;

        NotificationType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Nullable
        public static NotificationType fromString(String string) {
            if (string != null) {
                for (NotificationType type : NotificationType.values()) {
                    if (type.getName().equalsIgnoreCase(string)) {
                        return type;
                    }
                }
            }
            return null;
        }
    }
}