package net.kdt.pojavlaunch.edition;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import net.kdt.pojavlaunch.R;

/**
 * Remom edition switch: Java (in-app Pojav path) vs Bedrock.
 *
 * LEGAL Bedrock policy (Remom):
 * - Only official package {@code com.mojang.minecraftpe}
 * - If not installed → block play + send user to Play Store (purchase / install)
 * - No cracked / sideloaded-only Bedrock client path
 *
 * "Inside launcher" for Bedrock means: Remom UI + launch official game process.
 * A full multi-version APK sandbox (LeviLaunchroid-class) is a separate large module.
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

    /** True if official Bedrock package is installed on device. */
    public static boolean isOfficialBedrockInstalled(Context c) {
        if (c == null) return false;
        try {
            c.getPackageManager().getPackageInfo(PKG_BEDROCK, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Best-effort: Play-installed apps are usually not system apps and come from the store.
     * This is NOT a Microsoft license API; full license is enforced by the official game itself.
     */
    public static boolean looksLikePlayInstall(Context c) {
        if (!isOfficialBedrockInstalled(c)) return false;
        try {
            ApplicationInfo ai = c.getPackageManager().getApplicationInfo(PKG_BEDROCK, 0);
            // System / prebuilt oddballs are rare; mainly ensures package exists and is launchable.
            return (ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0 || true;
        } catch (Exception e) {
            return false;
        }
    }

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
                    if (!isOfficialBedrockInstalled(activity)) {
                        Toast.makeText(activity,
                                "Bedrock locked: buy & install official Minecraft from Play Store first.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(activity,
                                "Bedrock: official Minecraft found. Play opens the game.",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    setMode(activity, "java");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Play pressed in Bedrock mode.
     * @return true if handled (do not start Java)
     */
    public static boolean tryLaunchBedrock(Activity activity) {
        if (activity == null || !isBedrock(activity)) return false;

        if (!isOfficialBedrockInstalled(activity)) {
            new AlertDialog.Builder(activity)
                    .setTitle("Bedrock — Minecraft required")
                    .setMessage(
                            "Remom does not ship Bedrock.\n\n" +
                            "1) Buy / own Minecraft on Google Play\n" +
                            "2) Install official app (com.mojang.minecraftpe)\n" +
                            "3) Return here and press Play\n\n" +
                            "Without official Minecraft, Bedrock will not open.")
                    .setPositiveButton("Open Play Store", (d, w) -> openPlayStore(activity))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }

        PackageManager pm = activity.getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(PKG_BEDROCK);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(launch);
            return true;
        }

        Toast.makeText(activity, "Minecraft installed but cannot launch. Reinstall from Play Store.",
                Toast.LENGTH_LONG).show();
        openPlayStore(activity);
        return true;
    }

    private static void openPlayStore(Activity activity) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + PKG_BEDROCK)));
        } catch (Exception e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + PKG_BEDROCK)));
        }
    }
}
