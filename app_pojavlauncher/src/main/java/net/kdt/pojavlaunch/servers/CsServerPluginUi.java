package net.kdt.pojavlaunch.servers;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/** Quick Modrinth plugin/mod install UI (Paper plugins / Fabric mods). */
public final class CsServerPluginUi {
    private CsServerPluginUi() {}

    public static void showSearchInstall(Context ctx, LocalServerEntry entry, Runnable onUi) {
        if (ctx == null || entry == null) return;
        final EditText q = new EditText(ctx);
        q.setHint("Search plugins/mods");
        q.setText("ViaVersion");
        new AlertDialog.Builder(ctx)
                .setTitle("Install plugin / mod")
                .setMessage("Paper/Purpur → plugins/ · Fabric → mods/. Restart after install.")
                .setView(q)
                .setPositiveButton("Search", (d, w) -> {
                    String query = q.getText().toString().trim();
                    Toast.makeText(ctx, "Searching…", Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        try {
                            List<CsServerPluginInstaller.Hit> hits =
                                    CsServerPluginInstaller.search(query, entry.software, 12);
                            if (hits == null || hits.isEmpty()) {
                                post(ctx, () -> Toast.makeText(ctx, "No results", Toast.LENGTH_SHORT).show());
                                return;
                            }
                            String[] names = new String[hits.size()];
                            for (int i = 0; i < hits.size(); i++) names[i] = hits.get(i).title;
                            List<CsServerPluginInstaller.Hit> finalHits = hits;
                            post(ctx, () -> new AlertDialog.Builder(ctx)
                                    .setTitle("Install")
                                    .setItems(names, (dd, which) -> {
                                        CsServerPluginInstaller.Hit hit = finalHits.get(which);
                                        Toast.makeText(ctx, "Downloading " + hit.title + "…", Toast.LENGTH_SHORT).show();
                                        new Thread(() -> {
                                            try {
                                                File f = CsServerPluginInstaller.install(ctx, entry, hit.projectId);
                                                post(ctx, () -> Toast.makeText(ctx,
                                                        "Installed " + f.getName() + " — restart server", Toast.LENGTH_LONG).show());
                                            } catch (Exception ex) {
                                                post(ctx, () -> Toast.makeText(ctx,
                                                        "Install failed: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                                            }
                                        }).start();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show());
                        } catch (Exception ex) {
                            post(ctx, () -> Toast.makeText(ctx, "Search failed: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void post(Context ctx, Runnable r) {
        if (ctx instanceof android.app.Activity) {
            ((android.app.Activity) ctx).runOnUiThread(r);
        } else {
            r.run();
        }
    }
}
