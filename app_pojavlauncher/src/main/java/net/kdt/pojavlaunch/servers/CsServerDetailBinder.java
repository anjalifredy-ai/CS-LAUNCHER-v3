package net.kdt.pojavlaunch.servers;

import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

/** Wires overview Start/Stop/Restart/Plugins + start with tunnel wait. */
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

    /** Start server then poll until bore.pub address appears (or timeout ~6.4s). */
    public static void startAndWaitPublic(Fragment fragment, String serverId, Runnable onUiDone) {
        Toast.makeText(fragment.requireContext(), "Starting…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                LocalServerEntry entry = null;
                for (LocalServerEntry e : LocalServerStore.load(fragment.requireContext())) {
                    if (serverId.equals(e.id)) { entry = e; break; }
                }
                if (entry == null) throw new Exception("Server not found");
                LocalServerRunner.start(fragment.requireContext(), entry);
                try {
                    PlayitAccountStore.applyDefaultToServer(fragment.requireContext(), entry);
                } catch (Throwable ignored) {}
                String pub = null;
                for (int i = 0; i < 16; i++) {
                    Thread.sleep(400);
                    for (LocalServerEntry e : LocalServerStore.load(fragment.requireContext())) {
                        if (serverId.equals(e.id)) {
                            entry = e;
                            if (e.publicAddress != null && !e.publicAddress.isEmpty()) {
                                pub = e.publicAddress;
                            }
                            break;
                        }
                    }
                    if (pub != null) break;
                }
                final String msg = pub != null
                        ? ("Started · Public: " + pub)
                        : "Started local · Public tunnel pending — check Overview";
                fragment.requireActivity().runOnUiThread(() -> {
                    Toast.makeText(fragment.requireContext(), msg, Toast.LENGTH_LONG).show();
                    if (onUiDone != null) onUiDone.run();
                });
            } catch (Exception e) {
                fragment.requireActivity().runOnUiThread(() ->
                        Toast.makeText(fragment.requireContext(),
                                "Start failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "sd-start-wait").start();
    }
}
