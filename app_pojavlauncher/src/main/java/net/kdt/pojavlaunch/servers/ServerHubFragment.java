package net.kdt.pojavlaunch.servers;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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
    private EditText tunnelInput;
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
        tunnelInput = root.findViewById(R.id.tunnel_public_input);
        status = root.findViewById(R.id.server_hub_status);
        empty = root.findViewById(R.id.server_hub_empty);
        addBtn = root.findViewById(R.id.server_hub_add);
        refreshBtn = root.findViewById(R.id.server_hub_refresh);
        localListView = root.findViewById(R.id.local_servers_status);
        if (localListView != null) {
            localListView.setOnClickListener(v -> pickLocalServer(false));
            localListView.setOnLongClickListener(v -> { pickLocalServer(false); return true; });
        }

        View aternos = root.findViewById(R.id.free_host_aternos);
        View exaroton = root.findViewById(R.id.free_host_exaroton);
        View minehut = root.findViewById(R.id.free_host_minehut);
        View help = root.findViewById(R.id.free_host_help);
        if (exaroton != null) exaroton.setOnClickListener(v -> openHostInApp("exaroton"));
        if (aternos != null) aternos.setOnClickListener(v -> openHostInApp("aternos"));
        if (minehut != null) minehut.setOnClickListener(v -> openHostInApp("minehut"));
        if (help != null) help.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Cloud host")
                .setMessage("Exaroton/Aternos/Minehut → create → start → IP ADD.")
                .setPositiveButton("OK", null).show());

        View playit = root.findViewById(R.id.btn_open_playit);
        View saveTunnel = root.findViewById(R.id.btn_save_tunnel);
        View tunnelHelp = root.findViewById(R.id.btn_tunnel_help);
        if (playit != null) playit.setOnClickListener(v -> showDistantHelp());
        if (saveTunnel != null) saveTunnel.setOnClickListener(v -> saveTunnelAddress());
        if (tunnelHelp != null) tunnelHelp.setOnClickListener(v -> showDistantHelp());

        View createLocal = root.findViewById(R.id.btn_create_local_server);
        View startLocal = root.findViewById(R.id.btn_start_local_server);
        View stopLocal = root.findViewById(R.id.btn_stop_local_server);
        if (createLocal != null) createLocal.setOnClickListener(v -> showCreateLocalDialog());
        if (startLocal != null) {
            startLocal.setOnClickListener(v -> pickLocalServer(true));
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
        if (refreshLan != null) refreshLan.setOnClickListener(v -> {
            refreshLanIp();
            refreshLocalStatus();
        });

        if (list != null) {
            list.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new ServerAdapter(data, new ServerAdapter.Listener() {
                @Override public void onJoin(ServerEntry e){
                    if (getContext()==null) return;
                    queue(e);
                    try{ if (profileKey != null) LauncherPreferences.DEFAULT_PREF.edit().putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey).apply(); }catch(Exception ignored){}
                    ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                }
                @Override public void onRemove(ServerEntry e){
                    if (getContext()==null) return;
                    ServerStore.remove(requireContext(), profileKey, e.address);
                    ServerListSync.syncToGameDir(requireContext(), profileKey);
                    refresh();
                }
                @Override public void onEdit(ServerEntry e){
                    if (nameInput != null && e.name != null) nameInput.setText(e.name);
                    if (input != null) input.setText(e.address);
                }
            });
            list.setAdapter(adapter);
        }
        if (addBtn != null) addBtn.setOnClickListener(v -> addFromInput());
        if (refreshBtn != null) refreshBtn.setOnClickListener(v -> { refresh(); refreshLocalStatus(); });

        try { ServerListSync.importFromGameDir(requireContext(), profileKey); } catch (Throwable ignored) {}
        refresh();
        refreshLocalStatus();
    }

    private void showDistantHelp() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remote friends")
                .setMessage("Create + Start local server. Public address (bore.pub:PORT) appears on dashboard. Friends paste in Multiplayer.")
                .setPositiveButton("OK", null).show();
    }

    private void saveTunnelAddress() {
        if (tunnelInput == null || getContext() == null) return;
        String addr = tunnelInput.getText().toString().trim();
        if (addr.isEmpty()) {
            Toast.makeText(requireContext(), "Address paste karo", Toast.LENGTH_SHORT).show();
            return;
        }
        ServerEntry e = new ServerEntry();
        e.name = "Distant (tunnel)";
        e.address = addr;
        ServerStore.add(requireContext(), profileKey, e);
        ServerListSync.syncToGameDir(requireContext(), profileKey);
        List<LocalServerEntry> locals = LocalServerStore.load(requireContext());
        if (!locals.isEmpty()) {
            LocalServerEntry local = locals.get(0);
            local.publicAddress = addr;
            LocalServerStore.update(requireContext(), local);
        }
        Toast.makeText(requireContext(), "Saved", Toast.LENGTH_LONG).show();
        refresh();
    }

    private void openHostInApp(String which) {
        try {
            HostWebFragment f;
            if ("exaroton".equals(which)) f = HostWebFragment.exaroton();
            else if ("minehut".equals(which)) f = HostWebFragment.minehut();
            else if ("playit".equals(which)) f = HostWebFragment.playit();
            else f = HostWebFragment.aternos();
            Tools.swapFragment(requireActivity(), HostWebFragment.class, HostWebFragment.TAG, f.getArguments());
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "Open failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showCreateLocalDialog() {
        CsServerCreateDialog.show(this, this::refreshLocalStatus, this::openServerDetail);
    }

    private void startFirstLocal(boolean soloPlay) {
        List<LocalServerEntry> locals = LocalServerStore.load(requireContext());
        if (locals.isEmpty()) {
            Toast.makeText(requireContext(), "Pehle Create karo", Toast.LENGTH_SHORT).show();
            return;
        }
        openServerDetail(locals.get(0).id);
    }

    private void stopAllLocal() {
        List<LocalServerEntry> locals = LocalServerStore.load(requireContext());
        for (LocalServerEntry e : locals) {
            LocalServerRunner.stop(e.id);
            e.running = false;
            LocalServerStore.update(requireContext(), e);
        }
        try { LocalServerService.stop(requireContext()); } catch (Throwable ignored) {}
        Toast.makeText(requireContext(), "Stopped", Toast.LENGTH_SHORT).show();
        refreshLocalStatus();
    }

    private void refreshLocalStatus() {
        CsServerCreateDialog.refreshLocalList(localListView, getContext());
    }

    private void openServerDetail(String id) {
        try {
            Tools.swapFragment(requireActivity(), ServerDetailFragment.class,
                    ServerDetailFragment.TAG, ServerDetailFragment.args(id));
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "Open detail: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void pickLocalServer(boolean forStart) {
        List<LocalServerEntry> locals = LocalServerStore.load(requireContext());
        if (locals.isEmpty()) {
            Toast.makeText(requireContext(), "No local servers", Toast.LENGTH_SHORT).show();
            return;
        }
        if (locals.size() == 1) {
            openServerDetail(locals.get(0).id);
            return;
        }
        String[] names = new String[locals.size()];
        for (int i = 0; i < locals.size(); i++) {
            LocalServerEntry e = locals.get(i);
            names[i] = (LocalServerRunner.isRunning(e.id) ? "● " : "○ ") + e.name + " (" + e.softwareLabel() + ")";
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Select local server")
                .setItems(names, (d, which) -> openServerDetail(locals.get(which).id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshLanIp() {
        lastLanIp = LanHostHelper.getLanIpv4(requireContext());
        if (lanIpView != null) lanIpView.setText(lastLanIp != null ? lastLanIp : "—");
    }

    private void addFromInput() {
        if (input == null || getContext() == null) return;
        String addr = input.getText().toString().trim();
        if (addr.isEmpty()) return;
        ServerEntry e = new ServerEntry();
        e.address = addr;
        e.name = nameInput != null ? nameInput.getText().toString().trim() : addr;
        if (e.name.isEmpty()) e.name = addr;
        ServerStore.add(requireContext(), profileKey, e);
        ServerListSync.syncToGameDir(requireContext(), profileKey);
        input.setText("");
        refresh();
    }

    private void queue(ServerEntry e) {
        if (e == null || e.address == null) return;
        ServerStore.add(requireContext(), profileKey, e);
        ServerListSync.syncToGameDir(requireContext(), profileKey);
    }

    private void refresh() {
        if (getContext() == null) return;
        data.clear();
        try {
            List<ServerEntry> loaded = ServerStore.load(requireContext(), profileKey);
            if (loaded != null) data.addAll(loaded);
        } catch (Throwable ignored) {}
        if (adapter != null) adapter.notifyDataSetChanged();
        if (empty != null) empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        if (status != null) status.setText(data.size() + " saved");
    }
}
