package net.kdt.pojavlaunch.fragments;

import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

/** Opens Packs/Shaders browser or Mods browser from the in-game drawer. */
public final class InGameContentEntry {
    private InGameContentEntry() {}

    public static void open(@NonNull FragmentActivity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Install content")
                .setItems(new CharSequence[]{
                        "Resource packs & shaders (Modrinth)",
                        "Mods (Modrinth) — needs game restart"
                }, (d, which) -> {
                    try {
                        if (which == 0) {
                            ResourceBrowserDialog.show(activity);
                        } else {
                            InGameModBrowserDialog.show(activity);
                        }
                    } catch (Throwable t) {
                        net.kdt.pojavlaunch.Tools.showError(activity, t);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
