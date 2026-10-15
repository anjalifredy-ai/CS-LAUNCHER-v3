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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.edition.host.BedrockHostRouter;
import net.kdt.pojavlaunch.fragments.BedrockHomeFragment;

/** Java / Bedrock edition switch — Remom header. */
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
        PackageManager pm = c.getPackageManager();
        String[] pkgs = new String[]{PKG_BEDROCK, "com.mojang.minecraftpe.unlocks"};
        for (String pkg : pkgs) {
            try {
                pm.getPackageInfo(pkg, 0);
                return true;
            } catch (Throwable ignored) {}
            try {
                pm.getApplicationInfo(pkg, 0);
                return true;
            } catch (Throwable ignored) {}
            try {
                if (pm.getLaunchIntentForPackage(pkg) != null) return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static void bindHeader(Activity activity) {
        if (activity == null) return;
        try {
            View javaBtn = activity.findViewById(R.id.btn_edition_java);
            View bedrockBtn = activity.findViewById(R.id.btn_edition_bedrock);
            if (javaBtn != null) {
                javaBtn.setClickable(true);
                javaBtn.setFocusable(true);
                javaBtn.setOnClickListener(v -> selectEdition(activity, false));
            }
            if (bedrockBtn != null) {
                bedrockBtn.setClickable(true);
                bedrockBtn.setFocusable(true);
                bedrockBtn.setOnClickListener(v -> selectEdition(activity, true));
            }
            Spinner sp = activity.findViewById(R.id.edition_spinner);
            if (sp != null) {
                String[] labels = new String[]{"Java", "Bedrock"};
                ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                        android.R.layout.simple_spinner_dropdown_item, labels);
                sp.setAdapter(adapter);
                sp.setSelection(isBedrock(activity) ? 1 : 0, false);
                sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    private boolean first = true;
                    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (first) { first = false; return; }
                        selectEdition(activity, position == 1);
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });
            }
            refreshPillStyles(activity);
        } catch (Throwable t) {
            Toast.makeText(activity, "Edition switch error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void selectEdition(Activity activity, boolean bedrock) {
        if (activity == null) return;
        if (bedrock) {
            setMode(activity, "bedrock");
            refreshPillStyles(activity);
            if (!isOfficialBedrockInstalled(activity)) {
                Toast.makeText(activity,
                        "Bedrock: Play Store se official Minecraft (owned) install karo — fir detect hoga.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(activity, "Official Minecraft PE detected ✓", Toast.LENGTH_SHORT).show();
            }
            openBedrockUi(activity);
        } else {
            setMode(activity, "java");
            refreshPillStyles(activity);
            if (activity instanceof FragmentActivity) {
                try {
                    FragmentManager fm = ((FragmentActivity) activity).getSupportFragmentManager();
                    fm.popBackStack(BedrockHomeFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    Tools.backToMainMenu((FragmentActivity) activity);
                } catch (Throwable t) {
                    Toast.makeText(activity, "Java mode", Toast.LENGTH_SHORT).show();
                }
            }
        }
        Spinner sp = activity.findViewById(R.id.edition_spinner);
        if (sp != null) {
            try { sp.setSelection(bedrock ? 1 : 0, false); } catch (Throwable ignored) {}
        }
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
            tv.setBackgroundResource(R.drawable.bg_remom_pill_on);
            tv.setTextColor(0xFF0A1628);
        } else {
            tv.setBackgroundResource(R.drawable.bg_remom_pill_off);
            tv.setTextColor(0xFFC8D8EC);
        }
    }

    private static void openBedrockUi(Activity activity) {
        if (!(activity instanceof FragmentActivity)) return;
        FragmentActivity fa = (FragmentActivity) activity;
        try {
            FragmentManager fm = fa.getSupportFragmentManager();
            Fragment existing = fm.findFragmentByTag(BedrockHomeFragment.TAG);
            if (existing != null && existing.isVisible()) {
                Toast.makeText(activity, "Bedrock", Toast.LENGTH_SHORT).show();
                return;
            }
            fm.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.container_fragment, new BedrockHomeFragment(), BedrockHomeFragment.TAG)
                    .addToBackStack(BedrockHomeFragment.TAG)
                    .commitAllowingStateLoss();
            Toast.makeText(activity, "Bedrock home", Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            try {
                Tools.swapFragment(fa, BedrockHomeFragment.class, BedrockHomeFragment.TAG, null);
            } catch (Throwable e2) {
                Toast.makeText(activity, "Bedrock open failed: " + e2.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public static boolean tryLaunchBedrock(Activity activity) {
        if (activity == null || !isBedrock(activity)) return false;
        return BedrockHostRouter.launchFromProfile(activity);
    }
}
