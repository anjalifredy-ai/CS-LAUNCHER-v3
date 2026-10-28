package net.kdt.pojavlaunch.servers;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Full per-server screen: overview, console, settings, world, reach (playit one-time). */
public class ServerDetailFragment extends Fragment {

    public static final String TAG = "ServerDetailFragment";
    public static final String ARG_ID = "serverId";

    private final Handler ui = new Handler(Looper.getMainLooper());
    private LocalServerEntry entry;
    private String serverId;

    private FrameLayout content;
    private View panelOverview, panelConsole, panelSettings, panelWorld, panelReach;
    private TextView tabOverview, tabConsole, tabSettings, tabWorld, tabReach;
    private TextView statusPill, titleTv, subtitleTv;
    private ImageView iconView;

    private TextView consoleText;
    private ScrollView consoleScroll;
    private EditText consoleInput;
    private TextView worldListTv;
    private String selectedWorldName;

    private final LocalServerRunner.LogListener logListener = (sid, line) -> {
        if (serverId == null || !serverId.equals(sid)) return;
        ui.post(() -> appendConsole(line));
    };

    private final Runnable statusTick = new Runnable() {
        @Override public void run() {
            if (!isAdded()) return;
            refreshStatus();
            ui.postDelayed(this, 1500);
        }
    };

    public ServerDetailFragment() { super(R.layout.fragment_server_detail); }

    public static Bundle args(String serverId) {
        Bundle b = new Bundle();
        b.putString(ARG_ID, serverId);
        return b;
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle saved) {
        super.onViewCreated(root, saved);
        serverId = getArguments() != null ? getArguments().getString(ARG_ID) : null;
        entry = findEntry(serverId);
        if (entry == null) {
            Toast.makeText(requireContext(), "Server not found", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        content = root.findViewById(R.id.sd_content);
        statusPill = root.findViewById(R.id.sd_status_pill);
        titleTv = root.findViewById(R.id.sd_title);
        subtitleTv = root.findViewById(R.id.sd_subtitle);
        iconView = root.findViewById(R.id.sd_icon);

        tabOverview = root.findViewById(R.id.sd_tab_overview);
        tabConsole = root.findViewById(R.id.sd_tab_console);
        tabSettings = root.findViewById(R.id.sd_tab_settings);
        tabWorld = root.findViewById(R.id.sd_tab_world);
        tabReach = root.findViewById(R.id.sd_tab_reach);

        LayoutInflater inf = LayoutInflater.from(requireContext());
        panelOverview = inf.inflate(R.layout.panel_server_overview, content, false);
        panelConsole = inf.inflate(R.layout.panel_server_console, content, false);
        panelSettings = inf.inflate(R.layout.panel_server_settings, content, false);
        panelWorld = inf.inflate(R.layout.panel_server_world, content, false);
        panelReach = inf.inflate(R.layout.panel_server_reach, content, false);

        root.findViewById(R.id.sd_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        tabOverview.setOnClickListener(v -> showTab(0));
        tabConsole.setOnClickListener(v -> showTab(1));
        tabSettings.setOnClickListener(v -> showTab(2));
        tabWorld.setOnClickListener(v -> showTab(3));
        tabReach.setOnClickListener(v -> showTab(4));

        bindOverview();
        bindConsole();
        bindSettings();
        bindWorld();
        bindReach();

        titleTv.setText(entry.name != null ? entry.name : "Server");
        subtitleTv.setText(entry.softwareLabel() + " · " + entry.versionId);
        loadIcon();

        LocalServerRunner.addLogListener(logListener);
        showTab(0);
        refreshStatus();
        ui.postDelayed(statusTick, 1500);
    }

    @Override
    public void onDestroyView() {
        ui.removeCallbacks(statusTick);
        LocalServerRunner.removeLogListener(logListener);
        super.onDestroyView();
    }

    private LocalServerEntry findEntry(String id) {
        if (id == null || getContext() == null) return null;
        for (LocalServerEntry e : LocalServerStore.load(requireContext())) {
            if (id.equals(e.id)) return e;
        }
        return null;
    }

    private void reloadEntry() {
        LocalServerEntry e = findEntry(serverId);
        if (e != null) entry = e;
    }

    private void showTab(int index) {
        content.removeAllViews();
        View panel;
        switch (index) {
            case 1: panel = panelConsole; break;
            case 2: panel = panelSettings; break;
            case 3: panel = panelWorld; break;
            case 4: panel = panelReach; break;
            default: panel = panelOverview; break;
        }
        content.addView(panel);
        styleTab(tabOverview, index == 0);
        styleTab(tabConsole, index == 1);
        styleTab(tabSettings, index == 2);
        styleTab(tabWorld, index == 3);
        styleTab(tabReach, index == 4);
        if (index == 1 && consoleText != null) {
            List<String> snap = LocalServerRunner.getLogSnapshot(serverId);
            StringBuilder sb = new StringBuilder();
            for (String l : snap) sb.append(l).append('\n');
            if (sb.length() == 0) sb.append("Console idle. Start server to see logs.\n");
            consoleText.setText(sb.toString());
        }
        if (index == 3) refreshWorldList();
    }

    private void styleTab(TextView t, boolean on) {
        if (t == null) return;
        t.setBackgroundResource(on ? R.drawable.bg_remom_pill_on : R.drawable.bg_remom_pill_off);
        t.setTextColor(0xFFF0F6FF);
    }

    private void bindOverview() {
        panelOverview.findViewById(R.id.sd_btn_start).setOnClickListener(v -> startServer());
        panelOverview.findViewById(R.id.sd_btn_stop).setOnClickListener(v -> stopServer());
        panelOverview.findViewById(R.id.sd_btn_prepare).setOnClickListener(v -> prepareServer());
        panelOverview.findViewById(R.id.sd_btn_copy_addr).setOnClickListener(v -> copyBestAddress());
    }

    private void bindConsole() {
        consoleText = panelConsole.findViewById(R.id.sd_console_text);
        consoleScroll = panelConsole.findViewById(R.id.sd_console_scroll);
        consoleInput = panelConsole.findViewById(R.id.sd_console_input);
        panelConsole.findViewById(R.id.sd_console_send).setOnClickListener(v -> {
            String cmd = consoleInput != null ? consoleInput.getText().toString().trim() : "";
            if (cmd.isEmpty()) return;
            if (!LocalServerRunner.isRunning(serverId)) {
                Toast.makeText(requireContext(), "Server offline", Toast.LENGTH_SHORT).show();
                return;
            }
            LocalServerRunner.sendCommand(serverId, cmd);
            appendConsole("> " + cmd);
            consoleInput.setText("");
        });
    }

    private void bindSettings() {
        EditText name = panelSettings.findViewById(R.id.sd_set_name);
        EditText motd = panelSettings.findViewById(R.id.sd_set_motd);
        EditText port = panelSettings.findViewById(R.id.sd_set_port);
        EditText players = panelSettings.findViewById(R.id.sd_set_players);
        EditText ram = panelSettings.findViewById(R.id.sd_set_ram);
        SwitchCompat online = panelSettings.findViewById(R.id.sd_set_online);
        SwitchCompat pvp = panelSettings.findViewById(R.id.sd_set_pvp);
        SwitchCompat wl = panelSettings.findViewById(R.id.sd_set_whitelist);
        SwitchCompat hard = panelSettings.findViewById(R.id.sd_set_hardcore);
        SwitchCompat flight = panelSettings.findViewById(R.id.sd_set_flight);
        Spinner diff = panelSettings.findViewById(R.id.sd_set_difficulty);
        Spinner gm = panelSettings.findViewById(R.id.sd_set_gamemode);
        String[] diffs = {"peaceful", "easy", "normal", "hard"};
        String[] gms = {"survival", "creative", "adventure", "spectator"};
        if (diff != null) diff.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, diffs));
        if (gm != null) gm.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, gms));
        if (name != null) name.setText(entry.name);
        if (motd != null) motd.setText(entry.description != null ? entry.description : "");
        if (port != null) port.setText(String.valueOf(entry.port));
        if (players != null) players.setText(String.valueOf(entry.maxPlayers));
        if (ram != null) ram.setText(String.valueOf(entry.memoryMb));
        if (online != null) online.setChecked(entry.onlineMode);
        if (pvp != null) pvp.setChecked(entry.pvp);
        if (wl != null) wl.setChecked(entry.whitelist);
        if (hard != null) hard.setChecked(entry.hardcore);
        if (flight != null) flight.setChecked(entry.allowFlight);
        setSpinner(diff, diffs, entry.difficulty);
        setSpinner(gm, gms, entry.gamemode);
        View saveBtn = panelSettings.findViewById(R.id.sd_btn_save_settings);
        if (saveBtn != null) saveBtn.setOnClickListener(v -> {
            try {
                if (name != null) entry.name = name.getText().toString().trim();
                if (motd != null) entry.description = motd.getText().toString().trim();
                if (port != null) entry.port = parseInt(port.getText().toString(), 25565);
                if (players != null) entry.maxPlayers = parseInt(players.getText().toString(), 10);
                if (ram != null) entry.memoryMb = Math.max(512, Math.min(4096, parseInt(ram.getText().toString(), 1024)));
                if (online != null) entry.onlineMode = online.isChecked();
                if (pvp != null) entry.pvp = pvp.isChecked();
                if (wl != null) entry.whitelist = wl.isChecked();
                if (hard != null) entry.hardcore = hard.isChecked();
                if (flight != null) entry.allowFlight = flight.isChecked();
                if (diff != null) entry.difficulty = (String) diff.getSelectedItem();
                if (gm != null) entry.gamemode = (String) gm.getSelectedItem();
                LocalServerStore.update(requireContext(), entry);
                LocalServerRunner.applySettings(requireContext(), entry);
                titleTv.setText(entry.name);
                Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show();
                refreshStatus();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindWorld() {
        worldListTv = panelWorld.findViewById(R.id.sd_world_list);
        View refresh = panelWorld.findViewById(R.id.sd_btn_refresh_worlds);
        View importBtn = panelWorld.findViewById(R.id.sd_btn_import_world);
        if (refresh != null) refresh.setOnClickListener(v -> refreshWorldList());
        if (importBtn != null) importBtn.setOnClickListener(v -> importSelectedWorld());
        if (worldListTv != null) {
            worldListTv.setOnClickListener(v -> pickWorldDialog());
            worldListTv.setOnLongClickListener(v -> { pickWorldDialog(); return true; });
        }
    }

    private void bindReach() {
        EditText pub = panelReach.findViewById(R.id.sd_reach_public);
        TextView status = panelReach.findViewById(R.id.sd_playit_status);
        String def = PlayitAccountStore.getDefaultPublicAddress(requireContext());
        if (pub != null) {
            if (entry.publicAddress != null && !entry.publicAddress.isEmpty()) pub.setText(entry.publicAddress);
            else if (def != null && !def.isEmpty()) pub.setText(def);
        }
        if (status != null) status.setText(PlayitAccountStore.statusLine(requireContext()));

        View link = panelReach.findViewById(R.id.sd_btn_link_playit);
        if (link != null) link.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://playit.gg/login")));
                PlayitAccountStore.setLinked(requireContext(), true);
                if (status != null) status.setText(PlayitAccountStore.statusLine(requireContext()));
                Toast.makeText(requireContext(),
                        "Sign in → Tunnel → Minecraft Java → 127.0.0.1 + port → copy address → paste here",
                        Toast.LENGTH_LONG).show();
            } catch (Throwable t) {
                Toast.makeText(requireContext(), "Open https://playit.gg", Toast.LENGTH_SHORT).show();
            }
        });

        View save = panelReach.findViewById(R.id.sd_btn_save_reach);
        if (save != null) save.setOnClickListener(v -> {
            String a = pub != null ? pub.getText().toString().trim() : "";
            if (a.isEmpty()) {
                Toast.makeText(requireContext(), "Public address paste karo", Toast.LENGTH_SHORT).show();
                return;
            }
            entry.publicAddress = a;
            LocalServerStore.update(requireContext(), entry);
            PlayitAccountStore.setDefaultPublicAddress(requireContext(), a);
            if (status != null) status.setText(PlayitAccountStore.statusLine(requireContext()));
            Toast.makeText(requireContext(), "Saved — har server Start pe auto", Toast.LENGTH_LONG).show();
            refreshStatus();
        });

        View copy = panelReach.findViewById(R.id.sd_btn_copy_public);
        if (copy != null) copy.setOnClickListener(v -> {
            String a = pub != null ? pub.getText().toString().trim() : "";
            if (a.isEmpty()) a = entry.publicAddress;
            if (a == null || a.isEmpty()) a = PlayitAccountStore.getDefaultPublicAddress(requireContext());
            if (a == null || a.isEmpty()) {
                Toast.makeText(requireContext(), "No public address", Toast.LENGTH_SHORT).show();
                return;
            }
            copyText(a);
        });

        View clear = panelReach.findViewById(R.id.sd_btn_clear_playit);
        if (clear != null) clear.setOnClickListener(v -> {
            PlayitAccountStore.clear(requireContext());
            if (status != null) status.setText(PlayitAccountStore.statusLine(requireContext()));
            Toast.makeText(requireContext(), "Playit unlinked", Toast.LENGTH_SHORT).show();
        });
    }

    private void startServer() {
        Toast.makeText(requireContext(), "Starting " + entry.name + "…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                LocalServerRunner.start(requireContext(), entry);
                PlayitAccountStore.applyDefaultToServer(requireContext(), entry);
                ui.post(() -> {
                    reloadEntry();
                    String msg = "Server started";
                    if (entry.publicAddress != null && !entry.publicAddress.isEmpty()) {
                        msg = "Started · Public: " + entry.publicAddress;
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                    refreshStatus();
                    showTab(1);
                });
            } catch (Exception e) {
                ui.post(() -> Toast.makeText(requireContext(),
                        "Start failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "sd-start").start();
    }

    private void stopServer() {
        LocalServerRunner.stop(serverId);
        entry.running = false;
        LocalServerStore.update(requireContext(), entry);
        try { LocalServerService.stop(requireContext()); } catch (Throwable ignored) {}
        Toast.makeText(requireContext(), "Stopped", Toast.LENGTH_SHORT).show();
        refreshStatus();
    }

    private void prepareServer() {
        Toast.makeText(requireContext(), "Preparing…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                LocalServerRunner.prepare(requireContext(), entry);
                ui.post(() -> Toast.makeText(requireContext(), "Ready", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                ui.post(() -> Toast.makeText(requireContext(), "Prepare failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "sd-prep").start();
    }

    private void refreshStatus() {
        if (!isAdded() || entry == null) return;
        boolean on = LocalServerRunner.isRunning(serverId);
        entry.running = on;
        if (statusPill != null) {
            statusPill.setText(on ? "ONLINE" : "OFFLINE");
            statusPill.setBackgroundResource(on ? R.drawable.bg_remom_pill_on : R.drawable.bg_remom_pill_off);
        }
        TextView ram = panelOverview.findViewById(R.id.sd_stat_ram);
        TextView port = panelOverview.findViewById(R.id.sd_stat_port);
        TextView soft = panelOverview.findViewById(R.id.sd_stat_soft);
        TextView bg = panelOverview.findViewById(R.id.sd_stat_bg);
        TextView lan = panelOverview.findViewById(R.id.sd_addr_lan);
        TextView pub = panelOverview.findViewById(R.id.sd_addr_public);
        if (ram != null) ram.setText("RAM: " + entry.memoryMb + " MB" + (on ? " (live)" : ""));
        if (port != null) port.setText("Port: " + entry.port + " · online-mode: " + entry.onlineMode);
        if (soft != null) soft.setText(entry.softwareLabel() + " · MC " + entry.versionId);
        if (bg != null) bg.setText(on ? "Background: FGS on" : "Background: idle");
        String lanIp = LanHostHelper.getLanIpv4(requireContext());
        if (lan != null) lan.setText("LAN: " + entry.displayAddress(lanIp));
        if (pub != null) {
            pub.setText(entry.publicAddress != null && !entry.publicAddress.isEmpty()
                    ? "Public: " + entry.publicAddress
                    : "Public: Reach tab → playit once");
        }
    }

    private void appendConsole(String line) {
        if (consoleText == null) return;
        consoleText.append(line);
        consoleText.append("\n");
        if (consoleScroll != null) consoleScroll.post(() -> consoleScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void loadIcon() {
        if (iconView == null) return;
        try {
            if (entry.iconPath != null) {
                File f = new File(entry.iconPath);
                if (f.exists()) { iconView.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath())); return; }
            }
            File icon = new File(LocalServerRunner.serverDir(requireContext(), entry), "server-icon.png");
            if (icon.exists()) iconView.setImageBitmap(BitmapFactory.decodeFile(icon.getAbsolutePath()));
        } catch (Throwable ignored) {}
    }

    private void copyBestAddress() {
        String addr = (entry.publicAddress != null && !entry.publicAddress.trim().isEmpty())
                ? entry.publicAddress.trim()
                : entry.displayAddress(LanHostHelper.getLanIpv4(requireContext()));
        copyText(addr);
    }

    private void copyText(String t) {
        try {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("addr", t));
            Toast.makeText(requireContext(), "Copied: " + t, Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            Toast.makeText(requireContext(), t, Toast.LENGTH_LONG).show();
        }
    }

    private void refreshWorldList() {
        if (worldListTv == null || getContext() == null) return;
        File saves = new File(net.kdt.pojavlaunch.Tools.DIR_GAME_NEW, "saves");
        File[] dirs = saves.exists() ? saves.listFiles(File::isDirectory) : null;
        if (dirs == null || dirs.length == 0) {
            worldListTv.setText("No worlds in saves/");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (File d : dirs) sb.append("• ").append(d.getName()).append('\n');
        worldListTv.setText(sb.toString());
    }

    private void pickWorldDialog() {
        File saves = new File(net.kdt.pojavlaunch.Tools.DIR_GAME_NEW, "saves");
        File[] dirs = saves.exists() ? saves.listFiles(File::isDirectory) : null;
        if (dirs == null || dirs.length == 0) {
            Toast.makeText(requireContext(), "No worlds", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[dirs.length];
        for (int i = 0; i < dirs.length; i++) names[i] = dirs[i].getName();
        new AlertDialog.Builder(requireContext())
                .setTitle("Select world")
                .setItems(names, (d, which) -> {
                    selectedWorldName = names[which];
                    if (worldListTv != null) worldListTv.setText("Selected: " + selectedWorldName);
                }).show();
    }

    private void importSelectedWorld() {
        if (selectedWorldName == null || selectedWorldName.isEmpty()) {
            pickWorldDialog();
            Toast.makeText(requireContext(), "Pehle world select karo", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(), "Importing " + selectedWorldName + "…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                entry.worldName = selectedWorldName;
                LocalServerStore.update(requireContext(), entry);
                LocalServerRunner.applySettings(requireContext(), entry);
                ui.post(() -> Toast.makeText(requireContext(), "World set: " + selectedWorldName, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                ui.post(() -> Toast.makeText(requireContext(), "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private static void setSpinner(Spinner sp, String[] values, String current) {
        if (sp == null || values == null) return;
        if (current == null) return;
        for (int i = 0; i < values.length; i++) {
            if (current.equalsIgnoreCase(values[i])) { sp.setSelection(i); return; }
        }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}
