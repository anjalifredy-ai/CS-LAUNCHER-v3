package net.kdt.pojavlaunch.edition;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.edition.host.BedrockHostRouter;
import net.kdt.pojavlaunch.fragments.BedrockHomeFragment;

/** Java / Bedrock edition switch for the Remom header bar. */
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

    public static void bindHeader(Activity activity) {
        if (activity == null) return;
        bindPills(activity);
        bindSpinner(activity);
        refreshPillStyles(activity);
    }

    private static void bindPills(Activity activity) {
        View javaBtn = activity.findViewById(R.id.btn_edition_java);
        View bedrockBtn = activity.findViewById(R.id.btn_edition_bedrock);
        if (javaBtn == null && bedrockBtn == null) return;
        if (javaBtn != null) javaBtn.setOnClickListener(v -> selectEdition(activity, false));
        if (bedrockBtn != null) bedrockBtn.setOnClickListener(v -> selectEdition(activity, true));
    }

    private static void bindSpinner(Activity activity) {
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
                if (first) { first = false; return; }
                selectEdition(activity, position == 1);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private static void selectEdition(Activity activity, boolean bedrock) {
        if (bedrock) {
            setMode(activity, "bedrock");
            if (!isOfficialBedrockInstalled(activity)) {
                Toast.makeText(activity,
                        "Bedrock: install official Minecraft from Play Store (owned) to play.",
                        Toast.LENGTH_LONG).show();
            }
            if (activity instanceof FragmentActivity) {
                openBedrockUi((FragmentActivity) activity);
            }
        } else {
            setMode(activity, "java");
            if (activity instanceof FragmentActivity) {
                try {
                    Tools.backToMainMenu((FragmentActivity) activity);
                } catch (Throwable t) {
                    Toast.makeText(activity, "Switched to Java", Toast.LENGTH_SHORT).show();
                }
            }
        }
        refreshPillStyles(activity);
        Spinner sp = activity.findViewById(R.id.edition_spinner);
        if (sp != null) sp.setSelection(bedrock ? 1 : 0, false);
    }

    private static void refreshPillStyles(Activity activity) {
        boolean bedrock = isBedrock(activity);
        stylePill(activity.findViewById(R.id.btn_edition_java), !bedrock);
        stylePill(activity.findViewById(R.id.btn_edition_bedrock), bedrock);
    }

    private static void stylePill(TextView tv, boolean active) {
        if (tv == null) return;
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        if (active) {
            tv.setBackgroundResource(R.drawable.bg_cs_tab_pill_active);
            tv.setTextColor(0xFF151518);
        } else {
            tv.setBackgroundResource(R.drawable.bg_cs_tab_pill_idle);
            tv.setTextColor(0xFFE8E8EC);
        }
    }

    private static void openBedrockUi(FragmentActivity activity) {
        try {
            Tools.swapFragment(activity, BedrockHomeFragment.class, BedrockHomeFragment.TAG, null);
        } catch (Exception e) {
            Toast.makeText(activity, "Could not open Bedrock screen", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean tryLaunchBedrock(Activity activity) {
        if (activity == null || !isBedrock(activity)) return false;
        return BedrockHostRouter.launchFromProfile(activity);
    }
}
