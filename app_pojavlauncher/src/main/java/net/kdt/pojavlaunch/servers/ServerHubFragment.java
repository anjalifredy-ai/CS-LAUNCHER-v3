package net.kdt.pojavlaunch.servers;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import java.util.ArrayList;
import java.util.List;

public class ServerHubFragment extends Fragment {
    public static final String TAG = "ServerHubFragment";
    private String profileKey;
    private RecyclerView list;
    private EditText input;
    private EditText nameInput;
    private TextView status, empty, lanIpView, localListView;
    private View addBtn, refreshBtn;
    private ServerAdapter adapter;
    private final List<ServerEntry> data = new ArrayList<>();
    private String lastLanIp;

    public ServerHubFragment(){ super(R.layout.fragment_server_hub); }

    @Override public void onCreate(@Nullable Bundle s){
        super.onCreate(s);
        Bundle a = getArguments();
        profileKey = a != null ? a.getString("profileKey") : null;
        if (profileKey == null) profileKey = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
    }

    @Override public void onViewCreated(@NonNull View root, @Nullable Bundle s){
        View back = root.findViewById(R.id.server_hub_back);
        if (back != null) back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        TextView title = root.findViewById(R.id.server_hub_title);
        if (title != null && profileKey != null) {
            try {
                LauncherProfiles.load();
                String name = LauncherProfiles.mainProfileJson.profiles.get(profileKey) != null
                        ? LauncherProfiles.mainProfileJson.profiles.get(profileKey).name : profileKey;
                title.setText(name + " — Servers");
            } catch (Exception ignored) {}
        }
        list = root.findViewById(R.id.server_hub_list);
        input = root.findViewById(R.id.server_hub_input);
        nameInput = root.findViewById(R.id.server_hub_name);
        status = root.findViewById(R.id.server_hub_status);
        empty = root.findViewById(R.id.server_hub_empty);
        addBtn = root.findViewById(R.id.server_hub_add);
        refreshBtn = root.findViewById(R.id.server_hub_refresh);
        localListView = root.findViewById(R.id.local_servers_status);

        View aternos = root.findViewById(R.id.free_host_aternos);
        View minehut = root.findViewById(R.id.free_host_minehut);
        View help = root.findViewById(R.id.free_host_help);
        if (aternos != null) aternos.setOnClickListener(v -> openHostInApp(true));
        if (minehut != null) minehut.setOnClickListener(v -> openHostInApp(false));
        if (help != null) help.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Offline / cracked join")
                .setMessage(
                        "1) Offline account select in header\n" +
                        "2) Host panel: online-mode=false + Restart\n" +
                        "3) Version match\n" +
                        "4) Name + IP yahan ADD → list me save\n\n" +
                        "Public 'cracked' servers bhi kabhi online-mode ON rakhte hain — tab premium account chahiye.")
                .setPositiveButton("OK", null)
                .show());

        View createLocal = root.findViewById(R.id.btn_create_local_server);
        View startLocal = root.findViewById(R.id.btn_start_local_server);
        View stopLocal = root.findViewById(R.id.btn_stop_local_server);
        if (createLocal != null) createLocal.setOnClickListener(v -> showCreateLocalDialog());
        if (startLocal != null) {
            startLocal.setOnClickListener(v -> startFirstLocal(false));
            startLocal.setOnLongClickListener(v -> { startFirstLocal(true); return true; });
        }
        if (stopLocal != null) stopLocal.setOnClickListener(v -> stopAllLocal());

        lanIpView = root.findViewById(R.id.lan_host_ip);
        View copyLan = root.findViewById(R.id.lan_copy_ip);
        View refreshLan = root.findViewById(R.id.lan_refresh_ip);
        refreshLanIp();
        if (copyLan != null) copyLan.setOnClickListener(v -> {
            if (lastLanIp == null || lastLanIp.isEmpty()) {
                Toast.makeText(requireContext(), "Solo: 127.0.0.1", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("lan_ip", lastLanIp));
                Toast.makeText(requireContext(), "Wi‑Fi IP copied", Toast.LENGTH_LONG).show();
            } catch (Throwable t) {
                Toast.makeText(requireContext(), lastLanIp, Toast.LENGTH_LONG).show();
            }
        });
        if (refreshLan != null) refreshLan.setOnClickListener(v -> { refreshLanIp(); refreshLocalStatus(); });

        if (list != null) {
            list.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new ServerAdapter(data, new ServerAdapter.Listener() {
                @Override public void onJoin(ServerEntry e){
                    if (getContext()==null) return;
                    ServerPlayDialog.show(requireContext(), e, new ServerPlayDialog.Action() {
                        @Override public void onPlayNow(ServerEntry entry) {
                            queue(entry);
                            try{ if (profileKey != null) LauncherPreferences.DEFAULT_PREF.edit().putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey).apply(); }catch(Exception ignored){}
                            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                        }
                        @Override public void onSaveOnly(ServerEntry entry) {
                            queue(entry);
                            if (getContext()==null) return;
                            ServerStore.add(requireContext(), profileKey, entry);
                            ServerListSync.syncToGameDir(requireContext(), profileKey);
                            refresh();
                        }
                    });
                }
                @Override public void onRemove(ServerEntry e){
                    if (getContext()==null) return;
                    if (e.pinned || PartnerServer.is(e.address)) {
                        Toast.makeText(requireContext(), PartnerServer.NAME + " stays", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ServerStore.remove(requireContext(), profileKey, e.address);
                    ServerListSync.syncToGameDir(requireContext(), profileKey);
                    refresh();
                }
                @Override public void onEdit(ServerEntry e){
                    if (e.pinned || PartnerServer.is(e.address)) { onJoin(e); return; }
                    if (nameInput != null && e.name != null) nameInput.setText(e.name);
                    if (input != null) input.setText(e.address);
                }
            });
            list.setAdapter(adapter);
        }
        if (addBtn != null) addBtn.setOnClickListener(v -> addFromInput());
        if (refreshBtn != null) refreshBtn.setOnClickListener(v -> { refresh(); pingAll(); refreshLocalStatus(); });

        // Import any in-game servers so list never looks empty after restart
        try { ServerListSync.importFromGameDir(requireContext(), profileKey); } catch (Throwable ignored) {}
        refresh();
        pingAll();
        refreshLocalStatus();
    }

    private void openHostInApp(boolean aternos) {
        try {
            HostWebFragment f = aternos ? HostWebFragment.aternos() : HostWebFragment.minehut();
            Tools.swapFragment(requireActivity(), HostWebFragment.class, HostWebFragment.TAG, f.getArguments());
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "Host open failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showCreateLocalDialog() {
        Context ctx = requireContext();
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, 0);
        final EditText nameIn = new EditText(ctx);
        nameIn.setHint("Server name");
        nameIn.setText("Remom Solo");
        box.addView(nameIn);
        final EditText verIn = new EditText(ctx);
        verIn.setHint("Version");
        verIn.setText("1.20.4");
        box.addView(verIn);
        final EditText portIn = new EditText(ctx);
        portIn.setHint("Port");
        portIn.setInputType(InputType.TYPE_CLASS_NUMBER);
        portIn.setText("25565");
        box.addView(portIn);
        new AlertDialog.Builder(ctx)
                .setTitle("Create local server")
                .setMessage("Solo → 127.0.0.1\nSame Wi‑Fi → phone IP\nOffline friends → online-mode false on host")
                .setView(box)
                .setPositiveButton("Create & Prepare", (d, w) -> {
                    String name = nameIn.getText().toString().trim();
                    String ver = verIn.getText().toString().trim();
                    int port = 25565;
                    try { port = Integer.parseInt(portIn.getText().toString().trim()); } catch (Exception ignored) {}
                    LocalServerEntry e = LocalServerStore.create(ctx, name, ver, port);
                    Toast.makeText(ctx, "Preparing " + e.name + "…", Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        try {
                            LocalServerRunner.prepare(ctx, e);
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(ctx, e.name + " ready — Start", Toast.LENGTH_LONG).show();
                                refreshLocalStatus();
                            });
                        } catch (Exception ex) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(ctx, "Prepare failed: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startFirstLocal(boolean soloPlay) {
        List<LocalServerEntry> locals = LocalServerStore.load(requireContext());
        if (locals.isEmpty()) {
            Toast.makeText(requireContext(), "Pehle Create karo", Toast.LENGTH_SHORT).show();
            return;
        }
        LocalServerEntry e = locals.get(0);
        Toast.makeText(requireContext(), "Starting " + e.name + "…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                LocalServerRunner.start(requireContext(), e);
                String soloAddr = "127.0.0.1:" + e.port;
                ServerEntry solo = new ServerEntry();
                solo.name = e.name + " (Solo)";
                solo.address = soloAddr;
                ServerStore.add(requireContext(), profileKey, solo);
                String lan = LanHostHelper.getLanIpv4(requireContext());
                if (lan != null && !lan.isEmpty()) {
                    ServerEntry friends = new ServerEntry();
                    friends.name = e.name + " (Wi‑Fi)";
                    friends.address = e.displayAddress(lan);
                    ServerStore.add(requireContext(), profileKey, friends);
                }
                ServerListSync.syncToGameDir(requireContext(), profileKey);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    refresh();
                    refreshLocalStatus();
                    if (soloPlay) {
                        queue(solo);
                        try {
                            if (profileKey != null)
                                LauncherPreferences.DEFAULT_PREF.edit()
                                        .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey).apply();
                        } catch (Exception ignored) {}
                        Toast.makeText(requireContext(), "Solo → " + soloAddr, Toast.LENGTH_LONG).show();
                        ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                    } else {
                        Toast.makeText(requireContext(), "Running · Solo 127.0.0.1", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception ex) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Start failed: " + ex.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void stopAllLocal() {
        List<LocalServerEntry> locals = LocalServerStore.load(requireContext());
        for (LocalServerEntry e : locals) {
            LocalServerRunner.stop(e.id);
            e.running = false;
            LocalServerStore.update(requireContext(), e);
        }
        Toast.makeText(requireContext(), "Stopped", Toast.LENGTH_SHORT).show();
        refreshLocalStatus();
    }

    private void refreshLocalStatus() {
        if (localListView == null || getContext() == null) return;
        List<LocalServerEntry> locals = LocalServerStore.load(requireContext());
        if (locals.isEmpty()) {
            localListView.setText("No local servers — Create");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (LocalServerEntry e : locals) {
            boolean run = LocalServerRunner.isRunning(e.id);
            e.running = run;
            sb.append(run ? "● " : "○ ")
                    .append(e.name).append(" · ").append(e.versionId)
                    .append(" · 127.0.0.1:").append(e.port)
                    .append(run ? " RUNNING" : " stopped").append('\n');
        }
        localListView.setText(sb.toString().trim());
    }

    private void refreshLanIp() {
        lastLanIp = LanHostHelper.getLanIpv4(requireContext());
        if (lanIpView != null) {
            if (lastLanIp == null || lastLanIp.isEmpty())
                lanIpView.setText("Solo: 127.0.0.1");
            else
                lanIpView.setText("Solo: 127.0.0.1 · Nearby: " + lastLanIp);
        }
    }

    private void queue(ServerEntry e){
        if (e == null || e.address == null) return;
        ServerStore.setPending(requireContext(), profileKey, e.address);
    }

    private void addFromInput(){
        if (input == null || getContext()==null) return;
        String raw = input.getText().toString().trim();
        if (raw.isEmpty()) {
            Toast.makeText(requireContext(), "Enter address", Toast.LENGTH_SHORT).show();
            return;
        }
        String displayName = raw;
        if (nameInput != null) {
            String n = nameInput.getText().toString().trim();
            if (!n.isEmpty()) displayName = n;
        }
        ServerEntry e = new ServerEntry();
        e.name = displayName;
        e.address = raw;
        ServerStore.add(requireContext(), profileKey, e);
        ServerListSync.syncToGameDir(requireContext(), profileKey);
        input.setText("");
        if (nameInput != null) nameInput.setText("");
        Toast.makeText(requireContext(), "Saved: " + displayName, Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void refresh(){
        if (getContext()==null) return;
        data.clear();
        data.addAll(ServerStore.load(requireContext(), profileKey));
        if (adapter!=null) adapter.notifyDataSetChanged();
        if (empty!=null) empty.setVisibility(data.isEmpty()? View.VISIBLE : View.GONE);
    }

    private void pingAll(){
        if (getContext()==null) return;
        for (ServerEntry e : new ArrayList<>(data)) {
            ServerPinger.pingAsync(e, new ServerPinger.Callback(){
                @Override public void onResult(ServerEntry entry){
                    if(!isAdded()) return;
                    requireActivity().runOnUiThread(()->{ ServerStore.update(requireContext(), profileKey, entry); refresh(); });
                }
                @Override public void onError(ServerEntry entry, Exception ex){
                    if(!isAdded()) return;
                    requireActivity().runOnUiThread(()->{ ServerStore.update(requireContext(), profileKey, entry); refresh(); });
                }
            });
        }
    }
}
