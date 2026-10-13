package net.kdt.pojavlaunch.edition.host;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.edition.EditionMode;

/** Step 3/5: real working path — launch official Minecraft. */
public final class OfficialPlayStoreHost implements BedrockHost {

    @NonNull
    @Override
    public Capability capability() {
        return Capability.OFFICIAL_APP;
    }

    @NonNull
    @Override
    public String statusLine() {
        return "Host: Official Play Store app";
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
                            "Official Minecraft (com.mojang.minecraftpe) must be installed.\n\n" +
                            "Remom does not ship Bedrock.")
                    .setPositiveButton("Open Play Store", (d, w) -> openPlayStore(activity))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        Intent launch = activity.getPackageManager()
                .getLaunchIntentForPackage(EditionMode.PKG_BEDROCK);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(launch);
            return true;
        }
        Toast.makeText(activity, "Reinstall Minecraft from Play Store.", Toast.LENGTH_LONG).show();
        openPlayStore(activity);
        return true;
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
