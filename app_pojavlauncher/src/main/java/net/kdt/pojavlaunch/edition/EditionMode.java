package net.kdt.pojavlaunch.edition;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import net.kdt.pojavlaunch.R;

/**
 * Remom edition switch: Java (in-launcher, existing path) vs Bedrock.
 *
 * Bedrock is closed-source (Mojang). Legitimate launchers (LeviLaunchroid, etc.)
 * use the official Minecraft APK / package — they do not reimplement Bedrock from scratch.
 * This class launches {@code com.mojang.minecraftpe} when installed, otherwise Play Store.
 */
public final class EditionMode {
    public static final String PREF = "remom_edition";
    public static final String KEY = "mode"; // "java" | "bedrock"
    public static final String PKG_BEDROCK = "com.mojang.minecraftpe";

    private EditionMode() {}

    public static boolean isBedrock(Context c) {
        return "bedrock".equals(prefs(c).getString(KEY, "java"));
    }

    public static void setMode(Context c, String mode) {
        prefs(c).edit().putString(KEY, mode).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /** Bind header Spinner if present in layout. Safe no-op when view missing. */
    public static void bindHeader(Activity activity) {
        if (activity == null) return;
        Spinner sp = activity.findViewById(R.id.edition_spinner);
        if (sp == null) return;

        String[] labels = new String[]{"Java", "Bedrock"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, labels);
        sp.setAdapter(adapter);

        boolean bedrock = isBedrock(activity);
        sp.setSelection(bedrock ? 1 : 0, false);

        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    setMode(activity, "bedrock");
                    Toast.makeText(activity,
                            "Bedrock: official Minecraft required. Play opens the Bedrock app.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    setMode(activity, "java");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
 * Call when user presses Play while Bedrock mode is selected.
 * @return true if handled (caller should not start Java game)
 */
    public static boolean tryLaunchBedrock(Activity activity) {
        if (activity == null || !isBedrock(activity)) return false;

        PackageManager pm = activity.getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(PKG_BEDROCK);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(launch);
            return true;
        }

        // Not installed → Play Store / browser
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + PKG_BEDROCK)));
        } catch (Exception e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + PKG_BEDROCK)));
        }
        Toast.makeText(activity,
                "Install official Minecraft (Bedrock) from Play Store, then try again.",
                Toast.LENGTH_LONG).show();
        return true;
    }
}
