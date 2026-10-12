package net.kdt.pojavlaunch.servers;

import android.content.Intent;
import android.net.Uri;
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

/**
 * Server Hub — join list + Remom free hosting shortcuts (Aternos / Minehut).
 */
public class ServerHubFragment extends Fragment {
    public static final String TAG = "ServerHubFragment";
    private String profileKey;
    private RecyclerView list;
    private EditText input;
    private TextView status, empty;
    private View addBtn, refreshBtn;
    private ServerAdapter adapter;
    private final List<ServerEntry> data = new ArrayList<>();

    public ServerHubFragment(){ super(R.layout.fragment_server_hub); }

    @Override public void onCreate(@Nullable Bundle s){
        super.onCreate(s);
        Bundle a = getArguments();
        profileKey = a != null ? a.getString("profileKey") : null;
        if (profileKey == null) profileKey = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
    }

    @Override public void onViewCreated(@NonNull View root, @Nullable Bundle s){
        View back = root.findViewById(R.id.server_hub_back);
        if (back != null) back.setOnClickListener(v -> {
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(()->v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()).start();
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        TextView title = root.findViewById(R.id.server_hub_title);
        if (title != null && profileKey != null) {
            try { LauncherProfiles.load(); String name = LauncherProfiles.mainProfileJson.profiles.get(profileKey) != null ? LauncherProfiles.mainProfileJson.profiles.get(profileKey).name : profileKey; title.setText(name + " — Servers"); } catch (Exception ignored){}
        }

        list = root.findViewById(R.id.server_hub_list);
        input = root.findViewById(R.id.server_hub_input);
        status = root.findViewById(R.id.server_hub_status);
        empty = root.findViewById(R.id.server_hub_empty);
        addBtn = root.findViewById(R.id.server_hub_add);
        refreshBtn = root.findViewById(R.id.server_hub_refresh);

        View freeAternos = root.findViewById(R.id.free_host_aternos);
        if (freeAternos != null) freeAternos.setOnClickListener(v -> openUrl("https://aternos.org/"));
        View freeMinehut = root.findViewById(R.id.free_host_minehut);
        if (freeMinehut != null) freeMinehut.setOnClickListener(v -> openUrl("https://minehut.com/"));
        View freeHelp = root.findViewById(R.id.free_host_help);
        if (freeHelp != null) freeHelp.setOnClickListener(v -> Toast.makeText(requireContext(),
                "Free hosts sleep when empty. Start server on website, copy IP, Add above, then Play.", Toast.LENGTH_LONG).show());

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
                            refresh();
                        }
                    });
                }
                @Override public void onDelete(ServerEntry e){
                    if (getContext()==null) return;
                    if (e.pinned || PartnerServer.is(e.address)) {
                        Toast.makeText(requireContext(), PartnerServer.NAME + " is built into the launcher and stays in every profile", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ServerStore.remove(requireContext(), profileKey, e.address);
                    ServerListSync.syncToGameDir(requireContext(), profileKey);
                    refresh();
                    String pending = ServerStore.getPending(requireContext(), profileKey);
                    if (pending!=null && pending.equalsIgnoreCase(e.address)) ServerStore.clearPending(requireContext(), profileKey);
                }
                @Override public void onEdit(ServerEntry e){
                    if (e.pinned || PartnerServer.is(e.address)) { onJoin(e); return; }
                    if (input!=null){ input.setText(e.address); input.requestFocus(); }
                    if (status!=null){ status.setVisibility(View.VISIBLE); status.setText("Re-pinging " + e.address + "…"); }
                    ServerPinger.pingAsync(e, new ServerPinger.Callback(){
                        @Override public void onResult(ServerEntry entry){ if(!isAdded()) return; requireActivity().runOnUiThread(()->{ ServerStore.update(requireContext(), profileKey, entry); refresh(); if(status!=null) status.setText("✓ " + entry.motd + " • " + entry.playersOnline + "/" + entry.playersMax + " • " + entry.pingMs+"ms"); }); }
                        @Override public void onError(ServerEntry entry, Exception ex){ if(!isAdded()) return; requireActivity().runOnUiThread(()->{ ServerStore.update(requireContext(), profileKey, entry); refresh(); if(status!=null) status.setText("✗ Offline — " + ex.getMessage()); }); }
                    });
                }
            });
            list.setAdapter(adapter);
        }

        if (addBtn != null) addBtn.setOnClickListener(v->{
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).withEndAction(()->v.animate().scaleX(1f).scaleY(1f).setDuration(160).start()).start();
            addFromInput();
        });
        if (refreshBtn != null) refreshBtn.setOnClickListener(v->{
            v.animate().rotationBy(360).setDuration(600).start();
            pingAll();
        });
        if (input != null) input.setOnEditorActionListener((v,a,e)->{ addFromInput(); return true; });

        refresh();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cannot open browser", Toast.LENGTH_SHORT).show();
        }
    }

    private void queue(ServerEntry e){
        if (getContext()==null || e==null) return;
        ServerStore.setPending(requireContext(), profileKey, e.address);
        ServerListSync.syncToGameDir(requireContext(), profileKey);
    }

    private void addFromInput(){
        if (input==null || getContext()==null) return;
        String raw = input.getText()!=null ? input.getText().toString().trim() : "";
        if (raw.isEmpty()){ Toast.makeText(requireContext(), "Enter an address", Toast.LENGTH_SHORT).show(); return; }
        if (status!=null){ status.setVisibility(View.VISIBLE); status.setText("Pinging "+raw+"…"); }
        ServerEntry e = new ServerEntry(raw);
        ServerPinger.pingAsync(e, new ServerPinger.Callback(){
            @Override public void onResult(ServerEntry entry){
                if(!isAdded()) return;
                requireActivity().runOnUiThread(()->{
                    ServerStore.add(requireContext(), profileKey, entry);
                    ServerListSync.syncToGameDir(requireContext(), profileKey);
                    refresh();
                    if (input!=null) input.setText("");
                    if (status!=null) status.setText("✓ Added — "+entry.motd);
                });
            }
            @Override public void onError(ServerEntry entry, Exception ex){
                if(!isAdded()) return;
                requireActivity().runOnUiThread(()->{
                    ServerStore.add(requireContext(), profileKey, entry);
                    ServerListSync.syncToGameDir(requireContext(), profileKey);
                    refresh();
                    if (status!=null) status.setText("Added (offline for now)");
                });
            }
        });
    }

    private void refresh(){
        if (getContext()==null) return;
        data.clear();
        data.addAll(ServerStore.list(requireContext(), profileKey));
        if (adapter!=null) adapter.notifyDataSetChanged();
        if (empty!=null) empty.setVisibility(data.isEmpty()? View.VISIBLE : View.GONE);
    }

    private void pingAll(){
        if (getContext()==null) return;
        for (ServerEntry e : new ArrayList<>(data)) {
            ServerPinger.pingAsync(e, new ServerPinger.Callback(){
                @Override public void onResult(ServerEntry entry){ if(!isAdded()) return; requireActivity().runOnUiThread(()->{ ServerStore.update(requireContext(), profileKey, entry); refresh(); }); }
                @Override public void onError(ServerEntry entry, Exception ex){ if(!isAdded()) return; requireActivity().runOnUiThread(()->{ ServerStore.update(requireContext(), profileKey, entry); refresh(); }); }
            });
        }
    }
}
