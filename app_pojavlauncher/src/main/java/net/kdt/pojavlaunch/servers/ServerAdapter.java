package net.kdt.pojavlaunch.servers;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import net.kdt.pojavlaunch.R;
import java.util.List;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.VH> {
    public interface Listener {
        void onJoin(ServerEntry e);
        void onRemove(ServerEntry e);
        void onEdit(ServerEntry e);
    }
    private final List<ServerEntry> data;
    private final Listener listener;
    public ServerAdapter(List<ServerEntry> data, Listener l){ this.data=data; this.listener=l; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){
        View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_server_card, p, false);
        return new VH(view);
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos){
        ServerEntry e = data.get(pos);
        h.name.setText(e.name != null && !e.name.isEmpty() ? e.name : e.address);
        h.address.setText(e.address);
        // MOTD - clean, no JSON, max 2 lines
        String motd = e.motd;
        if (motd == null || motd.isEmpty() || motd.contains("{\"extra\"") || motd.contains("\"text\"")) {
            motd = "No server description available";
        }
        // Ensure MOTD is not raw JSON
        if (motd.trim().startsWith("{") && motd.contains("extra")) motd = "No server description available";
        h.motd.setText(motd);
        h.motd.setMaxLines(2);
        h.motd.setEllipsize(android.text.TextUtils.TruncateAt.END);

        if (e.online && e.playersOnline >= 0) {
            h.players.setVisibility(View.VISIBLE);
            h.players.setText(e.playersOnline + "/" + (e.playersMax>=0?String.valueOf(e.playersMax):"∞"));
        } else h.players.setVisibility(View.GONE);

        // Ping bars - Minecraft style 5 bars
        if (e.online && e.pingMs >= 0) {
            h.pingContainer.setVisibility(View.VISIBLE);
            h.ping.setText(e.pingMs + " ms");
            h.offlineText.setVisibility(View.GONE);
            int bars = getBars(e.pingMs);
            int filledColor = 0xFF62D99B;
            int emptyColor = 0xFF3A404C;
            // For poor ping, use yellow/red
            if (bars <= 2) filledColor = 0xFFFF6B6B;
            else if (bars <= 3) filledColor = 0xFFFFD166;
            int[] barIds = {R.id.ping_bar_1, R.id.ping_bar_2, R.id.ping_bar_3, R.id.ping_bar_4, R.id.ping_bar_5};
            for (int i=0;i<5;i++) {
                View bar = h.itemView.findViewById(barIds[i]);
                if (bar != null) bar.setBackgroundColor(i < bars ? filledColor : emptyColor);
            }
            h.btnJoin.setAlpha(1f); h.btnJoin.setEnabled(true);
            h.btnJoin.setText("PLAY");
        } else {
            // Offline
            h.pingContainer.setVisibility(View.VISIBLE);
            h.ping.setText("Offline");
            h.ping.setTextColor(0xFFFF6B6B);
            for (int id : new int[]{R.id.ping_bar_1,R.id.ping_bar_2,R.id.ping_bar_3,R.id.ping_bar_4,R.id.ping_bar_5}) {
                View bar = h.itemView.findViewById(id);
                if (bar != null) bar.setBackgroundColor(0xFF3A404C);
            }
            h.offlineText.setVisibility(View.VISIBLE);
            h.offlineText.setText("Offline");
            // Keep ping container visible but bars empty, disable PLAY
            h.btnJoin.setAlpha(0.45f); h.btnJoin.setEnabled(false);
            h.btnJoin.setText("OFFLINE");
            if (!e.online) {
                h.motd.setText("Server Offline");
                h.players.setVisibility(View.GONE);
            }
        }
        if (e.versionName != null && !e.versionName.isEmpty()) {
            h.version.setVisibility(View.VISIBLE); h.version.setText(e.versionName);
        } else h.version.setVisibility(View.GONE);
        h.dot.setVisibility(e.online ? View.VISIBLE : View.GONE);
        if (e.online) h.dot.setBackgroundResource(R.drawable.bg_server_online_dot);
        else h.dot.setBackgroundResource(R.drawable.bg_server_online_dot); // will be hidden anyway
        Bitmap icon = e.decodeIcon();
        if (icon != null) { h.icon.setImageBitmap(icon); h.icon.setPadding(0,0,0,0); }
        else h.icon.setImageResource(R.drawable.ic_globe);
        h.btnJoin.setOnClickListener(v -> {
            if (!e.online) { Toast.makeText(v.getContext(), "Server is offline", Toast.LENGTH_SHORT).show(); return; }
            v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70).withEndAction(()->v.animate().scaleX(1f).scaleY(1f).setDuration(160).start()).start();
            if (listener != null) listener.onJoin(e);
        });
        h.btnRemove.setOnClickListener(v -> { if(listener!=null) listener.onRemove(e); });
        h.itemView.setOnClickListener(v -> { if(listener!=null) listener.onEdit(e); });
        // entrance
        h.itemView.setAlpha(0f); h.itemView.setTranslationY(8f);
        h.itemView.animate().alpha(1f).translationY(0f).setStartDelay(pos*35L).setDuration(220).start();
    }
    private int getBars(long ping) {
        if (ping < 0) return 0;
        if (ping < 80) return 5;
        if (ping < 150) return 4;
        if (ping < 300) return 3;
        if (ping < 500) return 2;
        return 1;
    }
    @Override public int getItemCount(){ return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon; View dot; TextView name, address, motd, ping, players, version, offlineText, btnJoin; View btnRemove; View pingContainer;
        VH(View v){ super(v);
            icon=v.findViewById(R.id.server_icon); dot=v.findViewById(R.id.server_online_dot);
            name=v.findViewById(R.id.server_name); address=v.findViewById(R.id.server_address);
            motd=v.findViewById(R.id.server_motd); ping=v.findViewById(R.id.server_ping);
            players=v.findViewById(R.id.server_players); version=v.findViewById(R.id.server_version);
            offlineText=v.findViewById(R.id.server_offline_text);
            pingContainer=v.findViewById(R.id.server_ping_container);
            btnJoin=v.findViewById(R.id.server_btn_join); btnRemove=v.findViewById(R.id.server_btn_remove);
        }
    }
}
