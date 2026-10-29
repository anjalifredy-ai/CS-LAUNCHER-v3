package net.kdt.pojavlaunch.servers;

import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

/** Wires overview Start/Stop/Restart/Plugins. */
public final class CsServerDetailBinder {
    public interface Host {
        void startServer();
        void stopServer();
        void restartServer();
        void prepareServer();
        void copyBestAddress();
        LocalServerEntry entry();
    }

    private CsServerDetailBinder() {}

    public static void bindOverview(View panelOverview, Host host) {
        if (panelOverview == null || host == null) return;
        View start = panelOverview.findViewById(R.id.sd_btn_start);
        View stop = panelOverview.findViewById(R.id.sd_btn_stop);
        View restart = panelOverview.findViewById(R.id.sd_btn_restart);
        View plugins = panelOverview.findViewById(R.id.sd_btn_plugins);
        View prepare = panelOverview.findViewById(R.id.sd_btn_prepare);
        View copy = panelOverview.findViewById(R.id.sd_btn_copy_addr);
        if (start != null) start.setOnClickListener(v -> host.startServer());
        if (stop != null) stop.setOnClickListener(v -> host.stopServer());
        if (restart != null) restart.setOnClickListener(v -> host.restartServer());
        if (plugins != null) plugins.setOnClickListener(v ->
                CsServerPluginUi.showSearchInstall(panelOverview.getContext(), host.entry(), null));
        if (prepare != null) prepare.setOnClickListener(v -> host.prepareServer());
        if (copy != null) copy.setOnClickListener(v -> host.copyBestAddress());
    }

    public static void restart(Fragment fragment, String serverId, Runnable onUiRefresh) {
        Toast.makeText(fragment.requireContext(), "Restarting…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                LocalServerRunner.stop(serverId);
                Thread.sleep(1800);
                LocalServerEntry entry = null;
                for (LocalServerEntry e : LocalServerStore.load(fragment.requireContext())) {
                    if (serverId.equals(e.id)) { entry = e; break; }
                }
                if (entry == null) return;
                LocalServerRunner.start(fragment.requireContext(), entry);
                fragment.requireActivity().runOnUiThread(() -> {
                    Toast.makeText(fragment.requireContext(), "Restarted", Toast.LENGTH_LONG).show();
                    if (onUiRefresh != null) onUiRefresh.run();
                });
            } catch (Exception e) {
                fragment.requireActivity().runOnUiThread(() ->
                        Toast.makeText(fragment.requireContext(),
                                "Restart failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "sd-restart").start();
    }
}
