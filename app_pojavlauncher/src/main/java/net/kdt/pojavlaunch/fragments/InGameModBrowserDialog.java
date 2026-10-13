package net.kdt.pojavlaunch.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * In-game Modrinth mod installer for the current profile.
 * Installs into mods/; Fabric/Forge need a full game restart (Mojang screen).
 */
public final class InGameModBrowserDialog extends DialogFragment {

    public static final String TAG = "remom_ingame_mods";
    private static final String API = "https://api.modrinth.com/v2";
    private static final String UA = "RemomLauncher (github.com/anjalifredy-ai/CS-LAUNCHER-v3)";

    private LinearLayout mList;
    private ProgressBar mBusy;
    private EditText mSearch;
    @Nullable private String mMcVersion;
    @Nullable private String mProfileKey;

    public static void show(@NonNull FragmentActivity activity) {
        new InGameModBrowserDialog().show(activity.getSupportFragmentManager(), TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        resolveProfile();
        Dialog d = new Dialog(requireContext());
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(requireContext());
        title.setText("Mods (Modrinth)");
        title.setTextSize(16f);
        title.setTextColor(0xFFF3F3F7);
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("Current profile mods folder. After install → restart game to load.");
        sub.setTextSize(11f);
        sub.setTextColor(0xFF9A9AA6);
        sub.setPadding(0, dp(4), 0, dp(8));
        root.addView(sub);

        mSearch = new EditText(requireContext());
        mSearch.setHint("Search mods…");
        mSearch.setTextColor(0xFFF3F3F7);
        mSearch.setHintTextColor(0xFF6E7078);
        mSearch.setSingleLine(true);
        root.addView(mSearch);

        TextView go = new TextView(requireContext());
        go.setText("SEARCH");
        go.setTextColor(0xFF151518);
        go.setBackgroundColor(0xFFE8E8EC);
        go.setPadding(dp(12), dp(8), dp(12), dp(8));
        go.setOnClickListener(v -> fetch(mSearch.getText().toString().trim()));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gp.topMargin = dp(6);
        root.addView(go, gp);

        mBusy = new ProgressBar(requireContext());
        mBusy.setVisibility(android.view.View.GONE);
        root.addView(mBusy);

        ScrollView scroll = new ScrollView(requireContext());
        mList = new LinearLayout(requireContext());
        mList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(mList);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(280));
        sp.topMargin = dp(8);
        root.addView(scroll, sp);

        d.setContentView(root);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(0xF01C1C1C));
            d.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        fetch("");
        return d;
    }

    private void resolveProfile() {
        try {
            LauncherProfiles.load();
            String key = net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF
                    .getString(net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "");
            mProfileKey = key;
            MinecraftProfile p = LauncherProfiles.mainProfileJson.profiles.get(key);
            if (p != null && p.lastVersionId != null) {
                mMcVersion = p.lastVersionId;
                // strip loader suffixes like fabric-loader-... if present — keep simple
                if (mMcVersion.contains(" ")) mMcVersion = mMcVersion.split(" ")[0];
            }
        } catch (Throwable ignored) {}
    }

    private void fetch(String query) {
        mBusy.setVisibility(android.view.View.VISIBLE);
        mList.removeAllViews();
        PojavApplication.sExecutorService.execute(() -> {
            List<String[]> rows = new ArrayList<>();
            try {
                StringBuilder facets = new StringBuilder("[[\"project_type:mod\"]");
                if (mMcVersion != null && mMcVersion.matches("[0-9].*")) {
                    facets.append(",[\"versions:").append(mMcVersion).append("\"]");
                }
                facets.append("]");
                String url = API + "/search?limit=20&index=downloads&facets="
                        + URLEncoder.encode(facets.toString(), "UTF-8");
                if (query != null && !query.isEmpty()) {
                    url += "&query=" + URLEncoder.encode(query, "UTF-8");
                }
                JSONObject root = new JSONObject(httpGet(url));
                JSONArray hits = root.optJSONArray("hits");
                if (hits != null) {
                    for (int i = 0; i < hits.length(); i++) {
                        JSONObject h = hits.getJSONObject(i);
                        rows.add(new String[]{
                                h.optString("project_id"),
                                h.optString("title"),
                                h.optString("author")
                        });
                    }
                }
            } catch (Throwable t) {
                rows.add(new String[]{"", "Error: " + t.getMessage(), ""});
            }
            Tools.MAIN_HANDLER.post(() -> {
                if (!isAdded()) return;
                mBusy.setVisibility(android.view.View.GONE);
                for (String[] r : rows) {
                    addRow(r[0], r[1], r[2]);
                }
            });
        });
    }

    private void addRow(String id, String title, String author) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView t = new TextView(requireContext());
        t.setText(title + (author.isEmpty() ? "" : "  ·  " + author));
        t.setTextColor(0xFFF3F3F7);
        t.setTextSize(13f);
        row.addView(t);
        if (id != null && !id.isEmpty()) {
            TextView install = new TextView(requireContext());
            install.setText("INSTALL → mods/");
            install.setTextColor(0xFF7BE3A8);
            install.setPadding(0, dp(4), 0, 0);
            install.setOnClickListener(v -> installMod(id, title));
            row.addView(install);
        }
        mList.addView(row);
    }

    private void installMod(String projectId, String title) {
        Toast.makeText(requireContext(), "Downloading " + title + "…", Toast.LENGTH_SHORT).show();
        PojavApplication.sExecutorService.execute(() -> {
            try {
                String vurl = API + "/project/" + projectId + "/version";
                if (mMcVersion != null && mMcVersion.matches("[0-9].*")) {
                    vurl += "?game_versions=" + URLEncoder.encode("[\"" + mMcVersion + "\"]", "UTF-8");
                }
                JSONArray versions = new JSONArray(httpGet(vurl));
                JSONObject file = null;
                for (int i = 0; i < versions.length() && file == null; i++) {
                    JSONObject v = versions.optJSONObject(i);
                    JSONArray files = v != null ? v.optJSONArray("files") : null;
                    if (files == null || files.length() == 0) continue;
                    for (int f = 0; f < files.length(); f++) {
                        JSONObject fo = files.optJSONObject(f);
                        if (fo != null && fo.optBoolean("primary", false)) {
                            file = fo;
                            break;
                        }
                    }
                    if (file == null) file = files.optJSONObject(0);
                }
                if (file == null) throw new Exception("No file");
                String furl = file.getString("url");
                String profileKey = mProfileKey;
                Tools.MAIN_HANDLER.post(() -> {
                    ModDownloadHelper.downloadAndExtract(
                            requireContext(), title, furl, "mod", profileKey);
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Mod installed")
                            .setMessage(title + " → profile mods folder.\n\n"
                                    + "Mods only load after a full restart.\n"
                                    + "1) Force-close the game\n"
                                    + "2) Launch the same profile\n"
                                    + "3) Open your world again from the list\n\n"
                                    + net.kdt.pojavlaunch.GameSessionRestore.resumeHint(requireContext()))
                            .setPositiveButton("OK", null)
                            .show();
                });
            } catch (Throwable t) {
                Tools.MAIN_HANDLER.post(() ->
                        Toast.makeText(requireContext(), "Install failed: " + t.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestProperty("User-Agent", UA);
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        try (java.io.InputStream in = c.getInputStream();
             java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) bos.write(buf, 0, n);
            return bos.toString("UTF-8");
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
