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

/**
 * Server Hub list adapter.
 *
 * <p>Two view types: the built-in partner card (banner artwork, Discord button, no delete) and
 * the regular server card.
 */
public class ServerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_FEATURED = 1;

    public interface Listener {
        void onJoin(ServerEntry e);
        void onRemove(ServerEntry e);
        void onEdit(ServerEntry e);
    }

    private final List<ServerEntry> data;
    private final Listener listener;
    public ServerAdapter(List<ServerEntry> data, Listener l){ this.data=data; this.listener=l; }

    @Override public int getItemViewType(int position) {
        ServerEntry e = data.get(position);
        return (e.pinned || PartnerServer.is(e.address)) ? TYPE_FEATURED : TYPE_NORMAL;
    }

    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int viewType){
        LayoutInflater inflater = LayoutInflater.from(p.getContext());
        if (viewType == TYPE_FEATURED) {
            return new FeaturedVH(inflater.inflate(R.layout.item_server_featured, p, false));
        }
        return new VH(inflater.inflate(R.layout.item_server_card, p, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos){
        ServerEntry e = data.get(pos);
        if (holder instanceof FeaturedVH) bindFeatured((FeaturedVH) holder, e, pos);
        else bindNormal((VH) holder, e, pos);
    }

    // ─────────────────────────── featured partner card ───────────────────────────

    private void bindFeatured(FeaturedVH h, ServerEntry e, int pos) {
        h.name.setText(e.name != null && !e.name.isEmpty() ? e.name : PartnerServer.NAME);
        h.address.setText(e.getJoinAddress());
        h.motd.setText(e.online || (e.motdRaw != null && !e.motdRaw.isEmpty())
                ? MotdRenderer.render(e)
                : PartnerServer.TAGLINE);

        if (e.online && e.playersOnline >= 0) {
            h.players.setVisibility(View.VISIBLE);
            h.players.setText(e.playersOnline + " online");
        } else h.players.setVisibility(View.GONE);

        if (e.online && e.pingMs >= 0) {
            h.ping.setText(e.pingMs + " ms");
            h.ping.setTextColor(0xFFB6F5C6);
        } else {
            h.ping.setText("connecting…");
            h.ping.setTextColor(0xFFFFD166);
        }

        if (e.versionName != null && !e.versionName.isEmpty()) {
            h.version.setVisibility(View.VISIBLE);
            h.version.setText(e.versionName);
        } else h.version.setVisibility(View.GONE);

        h.btnPlay.setOnClickListener(v -> {
            ServerPlayDialog.press(v);
            if (listener != null) listener.onJoin(e);
        });
        h.btnDiscord.setOnClickListener(v -> {
            ServerPlayDialog.press(v);
            ServerPlayDialog.openDiscord(v.getContext());
        });
        // Tapping the artwork itself also opens the play popup — nothing here is destructive.
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onJoin(e); });

        // Entrance + a slow breathing rim so the partner card stands out without being noisy.
        h.itemView.setAlpha(0f);
        h.itemView.setTranslationY(10f);
        h.itemView.animate().alpha(1f).translationY(0f).setDuration(280).start();
        if (h.glow != null) {
            h.glow.animate().cancel();
            h.glow.setAlpha(0.35f);
            pulse(h.glow);
        }
    }

    private void pulse(View glow) {
        if (!glow.isAttachedToWindow()) return;
        glow.animate().alpha(0.95f).setStartDelay(0).setDuration(1400)
                .withEndAction(() -> glow.animate().alpha(0.3f).setDuration(1400)
                        .withEndAction(() -> pulse(glow)).start())
                .start();
    }

    // ─────────────────────────── regular card ───────────────────────────

    private void bindNormal(@NonNull VH h, ServerEntry e, int pos){
        h.name.setText(e.name != null && !e.name.isEmpty() ? e.name : e.address);
        h.address.setText(e.address);

        // ── Coloured MOTD, rendered exactly like the vanilla multiplayer screen ──
        h.motd.setText(MotdRenderer.render(e));
        h.motd.setMaxLines(2);
        h.motd.setEllipsize(android.text.TextUtils.TruncateAt.END);

        if (e.online && e.playersOnline >= 0) {
            h.players.setVisibility(View.VISIBLE);
            h.players.setText(e.playersOnline + "/" + (e.playersMax>=0?String.valueOf(e.playersMax):"\u221e"));
        } else h.players.setVisibility(View.GONE);

        int[] barIds = {R.id.ping_bar_1, R.id.ping_bar_2, R.id.ping_bar_3, R.id.ping_bar_4, R.id.ping_bar_5};
        h.pingContainer.setVisibility(View.VISIBLE);

        if (e.online && e.pingMs >= 0) {
            // Views are recycled: every property set in the offline branch must be undone here.
            h.ping.setText(e.pingMs + " ms");
            h.ping.setTextColor(0xFF9AA3B2);
            h.offlineText.setVisibility(View.GONE);
            int bars = getBars(e.pingMs);
            int filledColor = 0xFF62D99B;
            int emptyColor = 0xFF3A404C;
            if (bars <= 2) filledColor = 0xFFFF6B6B;
            else if (bars <= 3) filledColor = 0xFFFFD166;
            for (int i=0;i<5;i++) {
                View bar = h.itemView.findViewById(barIds[i]);
                if (bar != null) bar.setBackgroundColor(i < bars ? filledColor : emptyColor);
            }
            h.btnJoin.setAlpha(1f); h.btnJoin.setEnabled(true);
            h.btnJoin.setText("PLAY");
        } else {
            h.ping.setText("Offline");
            h.ping.setTextColor(0xFFFF6B6B);
            for (int id : barIds) {
                View bar = h.itemView.findViewById(id);
                if (bar != null) bar.setBackgroundColor(0xFF3A404C);
            }
            h.offlineText.setVisibility(View.VISIBLE);
            h.offlineText.setText("Offline");
            h.motd.setText(e.motdRaw != null && !e.motdRaw.isEmpty()
                    ? MotdRenderer.render(e)
                    : "Server offline \u2014 tap refresh to retry");
            h.players.setVisibility(View.GONE);
            // Offline servers can still be queued: Minecraft may reach a host the phone cannot.
            h.btnJoin.setAlpha(0.75f); h.btnJoin.setEnabled(true);
            h.btnJoin.setText("JOIN");
        }

        if (e.versionName != null && !e.versionName.isEmpty()) {
            h.version.setVisibility(View.VISIBLE); h.version.setText(e.versionName);
        } else h.version.setVisibility(View.GONE);
        h.dot.setVisibility(e.online ? View.VISIBLE : View.GONE);
        h.dot.setBackgroundResource(R.drawable.bg_server_online_dot);
        Bitmap icon = e.decodeIcon();
        if (icon != null) { h.icon.setImageBitmap(icon); h.icon.setPadding(0,0,0,0); }
        else h.icon.setImageResource(R.drawable.ic_globe);
        h.btnJoin.setOnClickListener(v -> {
            ServerPlayDialog.press(v);
            if (!e.online) Toast.makeText(v.getContext(), "Server did not answer the ping \u2014 you can still join", Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onJoin(e);
        });
        h.btnRemove.setVisibility(View.VISIBLE);
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

    static class FeaturedVH extends RecyclerView.ViewHolder {
        TextView name, address, motd, ping, players, version, btnPlay, btnDiscord;
        View glow;
        FeaturedVH(View v){ super(v);
            name=v.findViewById(R.id.fs_name); address=v.findViewById(R.id.fs_address);
            motd=v.findViewById(R.id.fs_motd); ping=v.findViewById(R.id.fs_ping);
            players=v.findViewById(R.id.fs_players); version=v.findViewById(R.id.fs_version);
            btnPlay=v.findViewById(R.id.fs_btn_play); btnDiscord=v.findViewById(R.id.fs_btn_discord);
            glow=v.findViewById(R.id.fs_glow);
        }
    }
}
