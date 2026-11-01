package net.kdt.pojavlaunch.remote;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import org.json.JSONObject;

/** Restored minimal-safe FirebaseSyncManager so release builds compile without Firebase runtime. */
public final class FirebaseSyncManager {

    private static final JSONObject sUpdate = new JSONObject();

    private FirebaseSyncManager() {}

    public static void onResume(Context ctx) {}

    public static String effectiveDbUrl(Context ctx) { return ""; }

    public static boolean isConfigured(Context ctx) { return false; }

    public static boolean isSponsorshipEnabled() { return false; }

    public static boolean hasUpdate() { return false; }

    public static boolean isNewerVersion(String remote, String local) { return false; }

    public static boolean isForceUpdate() { return false; }

    public static void checkForUpdateFromFcm(Activity act, String version, String url, String changelog) {}

    public static void checkForUpdate(Activity act) {}

    public static void showAnnouncements(Activity act) {}

    public static void showNotifications(Activity act) {}

    public static void applySponsorshipGates(Activity act) {}

    public static boolean gateSponsorView(View v) { return false; }

    public static void showMarkdownDialog(Activity act, String title, String markdown) {}

    public static void showMarkdownDialog(Activity act, String title, String markdown, boolean fullPage) {}

    public static class HomeBannerItem {
        public String id;
        public String title;
        public String body;
    }

    public static void setHomeBannerListener(Runnable listener) {}

    public static void dismissBanner(String id, Context ctx) {}

    public static void checkForUpdateManual(Activity act) {}
}
