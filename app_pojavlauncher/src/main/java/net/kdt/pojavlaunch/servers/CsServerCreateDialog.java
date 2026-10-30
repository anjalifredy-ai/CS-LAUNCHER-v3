package net.kdt.pojavlaunch.servers;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.List;

/** Create-local-server dialog + local list text (card-style). */
public final class CsServerCreateDialog {
    public interface OpenDetail { void open(String id); }

    private CsServerCreateDialog() {}

    public static void show(Fragment fragment, Runnable onPrepared, OpenDetail openDetail) {
        Context ctx = fragment.requireContext();
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * fragment.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, 0);

        final EditText nameIn = new EditText(ctx);
        nameIn.setHint("Server name");
        nameIn.setText("CS Server");
        box.addView(nameIn);

        final EditText descIn = new EditText(ctx);
        descIn.setHint("MOTD / description");
        descIn.setText("A CS Launcher Minecraft Server");
        box.addView(descIn);

        final Spinner softSpin = new Spinner(ctx);
        softSpin.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"vanilla", "paper", "purpur", "fabric"}));
        box.addView(softSpin);

        final Spinner verSpin = new Spinner(ctx);
        java.util.List<String> versions;
        try {
            versions = CsServerVersionCatalog.fetchReleaseIds(40);
        } catch (Throwable t) {
            versions = null;
        }
        if (versions == null || versions.isEmpty()) {
            versions = java.util.Arrays.asList(
                    "1.21.1", "1.21", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.19.2", "1.18.2", "1.16.5", "1.12.2");
        }
        verSpin.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, versions));
        for (int i = 0; i < versions.size(); i++) {
            if ("1.20.4".equals(versions.get(i))) { verSpin.setSelection(i); break; }
            if ("1.20.1".equals(versions.get(i))) { verSpin.setSelection(i); }
        }
        box.addView(verSpin);

        final EditText portIn = new EditText(ctx);
        portIn.setHint("Port (auto free port)");
        portIn.setInputType(InputType.TYPE_CLASS_NUMBER);
        int autoPort = 25565;
        try { autoPort = CsServerPortAllocator.allocate(ctx); } catch (Throwable ignored) {}
        portIn.setText(String.valueOf(autoPort));
        box.addView(portIn);

        final EditText memIn = new EditText(ctx);
        memIn.setHint("RAM MB (512-2048)");
        memIn.setInputType(InputType.TYPE_CLASS_NUMBER);
        memIn.setText("1024");
        box.addView(memIn);

        final android.widget.CheckBox offlineBox = new android.widget.CheckBox(ctx);
        offlineBox.setText("Allow offline players (online-mode OFF)");
        offlineBox.setChecked(true);
        offlineBox.setTextColor(0xFFF0F6FF);
        box.addView(offlineBox);

        new AlertDialog.Builder(ctx)
                .setTitle("Create local server")
                .setMessage("Pick software + version from list.\nIcon auto · Port auto · Paper/Fabric for plugins/mods.")
                .setView(box)
                .setPositiveButton("Create \u0026 Prepare", (d, w) -> {
                    String name = nameIn.getText().toString().trim();
                    String desc = descIn.getText().toString().trim();
                    String ver = verSpin.getSelectedItem() != null ? verSpin.getSelectedItem().toString().trim() : "1.20.4";
                    String soft = (String) softSpin.getSelectedItem();
                    int port = 0;
                    int mem = 1024;
                    try { port = Integer.parseInt(portIn.getText().toString().trim()); } catch (Exception ignored) {}
                    try { mem = Integer.parseInt(memIn.getText().toString().trim()); } catch (Exception ignored) {}
                    if (port <= 0) {
                        try { port = CsServerPortAllocator.allocate(ctx); } catch (Throwable t) { port = 25565; }
                    }
                    LocalServerEntry e = LocalServerStore.createFull(ctx, name, ver, port, soft, desc, mem, null);
                    e.onlineMode = !offlineBox.isChecked();
                    try { LocalServerStore.update(ctx, e); } catch (Throwable ignored) {}
                    Toast.makeText(ctx, "Preparing " + e.name + "…", Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        try {
                            LocalServerRunner.prepare(ctx, e);
                            if (!fragment.isAdded()) return;
                            fragment.requireActivity().runOnUiThread(() -> {
                                Toast.makeText(ctx, e.name + " ready · port " + e.port, Toast.LENGTH_LONG).show();
                                if (onPrepared != null) onPrepared.run();
                                if (openDetail != null) openDetail.open(e.id);
                            });
                        } catch (Exception ex) {
                            if (!fragment.isAdded()) return;
                            fragment.requireActivity().runOnUiThread(() ->
                                    Toast.makeText(ctx, "Prepare failed: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void refreshLocalList(TextView localListView, Context ctx) {
        if (localListView == null || ctx == null) return;
        List<LocalServerEntry> locals = LocalServerStore.load(ctx);
        if (locals.isEmpty()) {
            localListView.setText("No local servers yet — tap Create");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < locals.size(); i++) {
            LocalServerEntry e = locals.get(i);
            boolean on = LocalServerRunner.isRunning(e.id);
            sb.append(on ? "● ONLINE  " : "○ offline  ");
            sb.append(e.name != null ? e.name : "Server");
            sb.append("\n   ").append(e.softwareLabel()).append(" · MC ").append(e.versionId);
            sb.append(" · :").append(e.port);
            sb.append(" · RAM ").append(e.memoryMb).append("MB");
            sb.append(" · online-mode ").append(e.onlineMode ? "ON" : "OFF");
            if (e.publicAddress != null && !e.publicAddress.isEmpty()) {
                sb.append("\n   Public: ").append(e.publicAddress);
            }
            if (i < locals.size() - 1) sb.append("\n\n");
        }
        sb.append("\n\nTap → dashboard (Start/Stop/Restart/Console/Plugins)");
        localListView.setText(sb.toString());
    }
}
