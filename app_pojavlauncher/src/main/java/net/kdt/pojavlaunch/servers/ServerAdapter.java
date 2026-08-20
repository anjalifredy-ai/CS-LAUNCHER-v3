package net.kdt.pojavlaunch.servers;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
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
        h.motd.setText(e.motd != null && !e.motd.isEmpty() ? e.motd : (e.online ? "Online" : "Offline — tap refresh"));
        if (e.online && e.playersOnline >= 0) {
            h.players.setVisibility(View.VISIBLE);
            h.players.setText(e.playersOnline + "/" + (e.playersMax>=0?String.valueOf(e.playersMax):"∞"));
        } else h.players.setVisibility(View.GONE);
        if (e.online && e.pingMs >= 0) {
            h.ping.setVisibility(View.VISIBLE);
            h.ping.setText(e.pingMs + "ms");
            if (e.pingMs < 80) h.ping.setBackgroundResource(R.drawable.bg_badge_compat_ok);
            else if (e.pingMs < 180) h.ping.setBackgroundResource(R.drawable.bg_stat_chip);
            else h.ping.setBackgroundResource(R.drawable.bg_badge_compat_bad);
        } else h.ping.setVisibility(View.GONE);
        if (e.versionName != null && !e.versionName.isEmpty()) {
            h.version.setVisibility(View.VISIBLE); h.version.setText(e.versionName);
        } else h.version.setVisibility(View.GONE);
        h.dot.setVisibility(e.online ? View.VISIBLE : View.GONE);
        if (!e.online) h.dot.setBackgroundResource(R.drawable.bg_server_online_dot);
        Bitmap icon = e.decodeIcon();
        if (icon != null) { h.icon.setImageBitmap(icon); h.icon.setPadding(0,0,0,0); }
        else h.icon.setImageResource(R.drawable.ic_globe);
        h.btnJoin.setOnClickListener(v -> {
            v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70).withEndAction(()->v.animate().scaleX(1f).scaleY(1f).setDuration(160).start()).start();
            if (listener != null) listener.onJoin(e);
        });
        h.btnRemove.setOnClickListener(v -> { if(listener!=null) listener.onRemove(e); });
        h.itemView.setOnClickListener(v -> { if(listener!=null) listener.onEdit(e); });
        // entrance
        h.itemView.setAlpha(0f); h.itemView.setTranslationY(8f);
        h.itemView.animate().alpha(1f).translationY(0f).setStartDelay(pos*35L).setDuration(220).start();
    }
    @Override public int getItemCount(){ return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon; View dot; TextView name, address, motd, ping, players, version; View btnJoin, btnRemove;
        VH(View v){ super(v);
            icon=v.findViewById(R.id.server_icon); dot=v.findViewById(R.id.server_online_dot);
            name=v.findViewById(R.id.server_name); address=v.findViewById(R.id.server_address);
            motd=v.findViewById(R.id.server_motd); ping=v.findViewById(R.id.server_ping);
            players=v.findViewById(R.id.server_players); version=v.findViewById(R.id.server_version);
            btnJoin=v.findViewById(R.id.server_btn_join); btnRemove=v.findViewById(R.id.server_btn_remove);
        }
    }
}
