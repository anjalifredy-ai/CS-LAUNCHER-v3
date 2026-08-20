package net.kdt.pojavlaunch.servers;

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
 * V3.1 Server Hub — Zalith-inspired full-screen dedicated Servers page.
 * Opened from Profile → Servers (below Worlds). Dark white black, mast animation.
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
        // Header back
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

        if (list != null) {
            list.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new ServerAdapter(data, new ServerAdapter.Listener() {
                @Override public void onJoin(ServerEntry e){
                    if (getContext()==null||profileKey==null) return;
                    // Set pending for both edited profile and current profile (for 1-click)
                    ServerStore.setPending(requireContext(), profileKey, e.address);
                    try { String cur = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey); ServerStore.setPending(requireContext(), cur, e.address); } catch (Exception ignored){}
                    ServerListSync.syncToGameDir(requireContext(), profileKey);
                    Toast.makeText(requireContext(), "\u27a4 " + e.getJoinAddress() + " \u2014 queued, the next PLAY joins automatically", Toast.LENGTH_LONG).show();
                    View card = root.findViewById(R.id.server_hub_card);
                    if (card!=null) card.animate().scaleX(0.99f).scaleY(0.99f).setDuration(80).withEndAction(()->card.animate().scaleX(1f).scaleY(1f).setDuration(160).start()).start();
                    new android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Join " + e.address + "?")
                        .setMessage("Launch straight into this server?\n\nAddress: " + e.getJoinAddress()
                                + "\nThe launcher picks the right flag for the version it starts:"
                                + "\n  \u2022 1.20+  \u2192  --quickPlayMultiplayer"
                                + "\n  \u2022 older  \u2192  --server / --port"
                                + "\n\nIt is also added to the in-game Multiplayer list.")
                        .setPositiveButton("LAUNCH NOW", (d,w)->{
                            ServerStore.setPending(requireContext(), profileKey, e.address);
                            try{ LauncherPreferences.DEFAULT_PREF.edit().putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey).apply(); }catch(Exception ignored){}
                            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                        })
                        .setNegativeButton("JUST SAVE", null).show();
                }
                @Override public void onRemove(ServerEntry e){
                    if (getContext()==null) return;
                    ServerStore.remove(requireContext(), e.address);
                    ServerListSync.syncToGameDir(requireContext(), profileKey);
                    refresh();
                    String pending = ServerStore.getPending(requireContext(), profileKey);
                    if (pending!=null && pending.equalsIgnoreCase(e.address)) ServerStore.clearPending(requireContext(), profileKey);
                }
                @Override public void onEdit(ServerEntry e){
                    if (input!=null){ input.setText(e.address); input.requestFocus(); }
                    if (status!=null){ status.setVisibility(View.VISIBLE); status.setText("Re-pinging " + e.address + "…"); }
                    ServerPinger.pingAsync(e, new ServerPinger.Callback(){
                        @Override public void onResult(ServerEntry entry){ if(!isAdded()) return; requireActivity().runOnUiThread(()->{ ServerStore.update(requireContext(), entry); refresh(); if(status!=null) status.setText("✓ " + entry.motd + " • " + entry.playersOnline + "/" + entry.playersMax + " • " + entry.pingMs+"ms"); }); }
                        @Override public void onError(ServerEntry entry, Exception ex){ if(!isAdded()) return; requireActivity().runOnUiThread(()->{ ServerStore.update(requireContext(), entry); refresh(); if(status!=null) status.setText("✗ Offline — " + ex.getMessage()); }); }
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

        // Mast animation
        View header = root.findViewById(R.id.server_hub_header);
        View card = root.findViewById(R.id.server_hub_card);
        if (header!=null){ header.setAlpha(0f); header.setTranslationY(-12f); header.animate().alpha(1f).translationY(0f).setDuration(320).start(); }
        if (card!=null){ card.setAlpha(0f); card.setTranslationY(12f); card.animate().alpha(1f).translationY(0f).setStartDelay(120).setDuration(320).start(); }

        // Pull in servers the player added inside the game, then push ours back out,
        // so the hub and the Multiplayer screen always show the same list.
        try {
            ServerListSync.importFromGameDir(requireContext(), profileKey);
            ServerListSync.syncToGameDir(requireContext(), profileKey);
        } catch (Exception ignored) {}

        refresh();
        pingAll();
    }

    private void addFromInput(){
        if (getContext()==null||input==null) return;
        String raw = input.getText().toString().trim();
        if (raw.isEmpty()){ Toast.makeText(requireContext(),"Enter IP like mc.hypixel.net", Toast.LENGTH_SHORT).show(); return; }
        if (raw.contains(" ")) raw = raw.split("\\s+")[0];
        if (raw.startsWith("http://")) raw=raw.substring(7);
        if (raw.startsWith("https://")) raw=raw.substring(8);
        if (raw.endsWith("/")) raw=raw.substring(0,raw.length()-1);
        ServerEntry e = new ServerEntry(raw);
        final String finalRaw = raw;
        if (status!=null){ status.setVisibility(View.VISIBLE); status.setText("Pinging " + raw + "…"); status.setAlpha(0f); status.animate().alpha(1f).setDuration(180).start(); }
        for(ServerEntry s: data) if(s.address.equalsIgnoreCase(e.address)){ if(status!=null) status.setText("Already added"); return; }
        ServerPinger.pingAsync(e, new ServerPinger.Callback(){
            @Override public void onResult(ServerEntry entry){ if(!isAdded()) return; requireActivity().runOnUiThread(()->{ ServerStore.add(requireContext(), entry); ServerListSync.syncToGameDir(requireContext(), profileKey); if(input!=null) input.setText(""); refresh(); if(status!=null) status.setText("✓ Added: " + (entry.motd!=null?entry.motd:"") + " • " + entry.playersOnline + "/" + entry.playersMax + " • " + entry.pingMs+"ms"); if(list!=null) list.smoothScrollToPosition(0); });}
            @Override public void onError(ServerEntry entry, Exception ex){ if(!isAdded()) return; requireActivity().runOnUiThread(()->{ ServerStore.add(requireContext(), entry); ServerListSync.syncToGameDir(requireContext(), profileKey); refresh(); if(status!=null) status.setText("Added offline: " + finalRaw + " ("+ex.getMessage()+") — will retry"); });}
        });
    }
    private void refresh(){
        if (getContext()==null||list==null) return;
        List<ServerEntry> stored = ServerStore.load(requireContext());
        data.clear(); data.addAll(stored);
        if(adapter!=null) adapter.notifyDataSetChanged();
        if(empty!=null) empty.setVisibility(data.isEmpty()?View.VISIBLE:View.GONE);
        if(profileKey!=null && status!=null){
            String pending = ServerStore.getPending(requireContext(), profileKey);
            if(pending!=null){ status.setVisibility(View.VISIBLE); status.setText("Queued join: " + pending + " \u2014 the next PLAY auto-joins it"); }
        }
    }
    private void pingAll(){
        if(getContext()==null||data.isEmpty()) return;
        if(status!=null){ status.setVisibility(View.VISIBLE); status.setText("Pinging " + data.size() + " servers…"); }
        for(ServerEntry e: new ArrayList<>(data)){
            ServerPinger.pingAsync(e, new ServerPinger.Callback(){
                @Override public void onResult(ServerEntry entry){ if(!isAdded()) return; requireActivity().runOnUiThread(()->{ ServerStore.update(requireContext(), entry); ServerListSync.syncToGameDir(requireContext(), profileKey); refresh(); });}
                @Override public void onError(ServerEntry entry, Exception ex){ if(!isAdded()) return; requireActivity().runOnUiThread(()->{ ServerStore.update(requireContext(), entry); refresh(); });}
            });
        }
    }
}
