package net.kdt.pojavlaunch.edition;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.edition.host.BedrockHostRouter;
import net.kdt.pojavlaunch.fragments.BedrockHomeFragment;

/**
 * Java / Bedrock switch. Play goes through {@link BedrockHostRouter}.
 */
public final class EditionMode {
    public static final String PREF = "remom_edition";
    public static final String KEY = "mode";
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

    public static boolean isOfficialBedrockInstalled(Context c) {
        if (c == null) return false;
        try {
            c.getPackageManager().getPackageInfo(PKG_BEDROCK, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean looksLikePlayInstall(Context c) {
        if (!isOfficialBedrockInstalled(c)) return false;
        try {
            ApplicationInfo ai = c.getPackageManager().getApplicationInfo(PKG_BEDROCK, 0);
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
            private boolean first = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (first) {
                    first = false;
                    if (position == 1 && activity instanceof FragmentActivity) {
                        openBedrockUi((FragmentActivity) activity);
                    }
                    return;
                }
                if (position == 1) {
                    setMode(activity, "bedrock");
                    if (!isOfficialBedrockInstalled(activity)) {
                        Toast.makeText(activity,
                                "Bedrock: install official Minecraft from Play Store to play.",
                                Toast.LENGTH_LONG).show();
                    }
                    if (activity instanceof FragmentActivity) {
                        openBedrockUi((FragmentActivity) activity);
                    }
                } else {
                    setMode(activity, "java");
                    if (activity instanceof FragmentActivity) {
                        Tools.backToMainMenu((FragmentActivity) activity);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private static void openBedrockUi(FragmentActivity activity) {
        try {
            Tools.swapFragment(activity, BedrockHomeFragment.class, BedrockHomeFragment.TAG, null);
        } catch (Exception e) {
            Toast.makeText(activity, "Could not open Bedrock screen", Toast.LENGTH_SHORT).show();
        }
    }

    /** Play Bedrock via host router (Step 5). */
    public static boolean tryLaunchBedrock(Activity activity) {
        if (activity == null || !isBedrock(activity)) return false;
        return BedrockHostRouter.launchFromProfile(activity);
    }
}
