package net.kdt.pojavlaunch.edition.host;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.edition.EditionMode;

/**
 * Legal Bedrock path: official owned Minecraft PE.
 * Launches in the same task so Back returns to Remom (not a detached external app stack).
 * Full native multi-version inside Remom process is separate (StubNativeHost).
 */
public final class OfficialPlayStoreHost implements BedrockHost {

    @NonNull
    @Override
    public Capability capability() {
        return Capability.OFFICIAL_APP;
    }

    @NonNull
    @Override
    public String statusLine() {
        return "Host: Official Minecraft (same task · Back → Remom)";
    }

    @Override
    public boolean canLaunch(@NonNull Activity activity, @Nullable String versionId) {
        return EditionMode.isOfficialBedrockInstalled(activity);
    }

    @Override
    public boolean launch(@NonNull Activity activity, @Nullable String versionId) {
        if (!EditionMode.isOfficialBedrockInstalled(activity)) {
            new AlertDialog.Builder(activity)
                    .setTitle("Bedrock — Minecraft required")
                    .setMessage(
                            "Official Minecraft (Play Store) must be installed and owned.\n\n" +
                            "Remom does not ship Bedrock game files.")
                    .setPositiveButton("Open Play Store", (d, w) -> openPlayStore(activity))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }

        Intent launch = activity.getPackageManager()
                .getLaunchIntentForPackage(EditionMode.PKG_BEDROCK);
        if (launch == null) {
            Toast.makeText(activity, "Reinstall Minecraft from Play Store.", Toast.LENGTH_LONG).show();
            openPlayStore(activity);
            return true;
        }

        // Same task as Remom — Back returns here (no FLAG_ACTIVITY_NEW_TASK)
        launch.setFlags(0);
        launch.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        try {
            ComponentName cn = launch.getComponent();
            if (cn != null) {
                Intent explicit = new Intent(Intent.ACTION_MAIN);
                explicit.addCategory(Intent.CATEGORY_LAUNCHER);
                explicit.setComponent(cn);
                explicit.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                activity.startActivity(explicit);
            } else {
                activity.startActivity(launch);
            }
            Toast.makeText(activity,
                    "Bedrock opened · Back returns to Remom",
                    Toast.LENGTH_SHORT).show();
            return true;
        } catch (Throwable t) {
            try {
                activity.startActivity(launch);
                return true;
            } catch (Throwable t2) {
                Toast.makeText(activity, "Launch failed: " + t2.getMessage(), Toast.LENGTH_LONG).show();
                return true;
            }
        }
    }

    private static void openPlayStore(Activity activity) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + EditionMode.PKG_BEDROCK)));
        } catch (Exception e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id="
                            + EditionMode.PKG_BEDROCK)));
        }
    }
}
