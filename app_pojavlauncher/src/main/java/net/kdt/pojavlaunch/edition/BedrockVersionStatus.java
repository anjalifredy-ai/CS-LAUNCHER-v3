package net.kdt.pojavlaunch.edition;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Day 4: UI rows for Bedrock version list. */
public final class BedrockVersionStatus {

    public enum Kind {
        PLAY_STORE,
        IMPORTED,
        MISSING
    }

    public final String displayLabel;
    public final String versionId;
    public final Kind kind;
    public final String detectedVersion;

    public BedrockVersionStatus(String displayLabel, String versionId, Kind kind, String detectedVersion) {
        this.displayLabel = displayLabel;
        this.versionId = versionId;
        this.kind = kind;
        this.detectedVersion = detectedVersion;
    }

    public String statusText() {
        switch (kind) {
            case PLAY_STORE:
                return "Play Store";
            case IMPORTED:
                return detectedVersion != null ? "Imported (v" + detectedVersion + ")" : "Imported";
            case MISSING:
            default:
                return "Missing";
        }
    }

    public int statusColor() {
        switch (kind) {
            case PLAY_STORE:
                return 0xFF64B5F6; // blue
            case IMPORTED:
                return 0xFF81C784; // green
            case MISSING:
            default:
                return 0xFF90A4AE; // grey
        }
    }

    /** Build list: Play Store + catalog + any extra imported ids. */
    public static List<BedrockVersionStatus> buildList(Context c) {
        Map<String, BedrockVersionStatus> map = new LinkedHashMap<>();

        // Play Store row
        boolean playInstalled = EditionMode.isOfficialBedrockInstalled(c);
        boolean playApk = BedrockVersionManager.hasFullApk(c, "playstore");
        String playDetected = BedrockVersionManager.getDetectedVersionName(c, "playstore");
        Kind playKind = playApk ? Kind.IMPORTED : (playInstalled ? Kind.PLAY_STORE : Kind.MISSING);
        map.put("playstore", new BedrockVersionStatus(
                "Play Store (current)", "playstore", playKind, playDetected));

        for (String label : BedrockProfileStore.catalogVersions()) {
            if (label.startsWith("Play Store")) continue;
            String id = label;
            boolean has = BedrockVersionManager.hasFullApk(c, id);
            String det = BedrockVersionManager.getDetectedVersionName(c, id);
            Kind k = has ? Kind.IMPORTED : Kind.MISSING;
            map.put(id, new BedrockVersionStatus(label, id, k, det));
        }

        // Extra imported from registry not in catalog
        for (BedrockVersionManager.VersionEntry e : BedrockVersionManager.list(c)) {
            if (map.containsKey(e.id)) continue;
            if ("playstore".equals(e.id)) continue;
            boolean has = BedrockVersionManager.hasFullApk(c, e.id);
            String det = BedrockVersionManager.getDetectedVersionName(c, e.id);
            if (!has && det == null) continue;
            map.put(e.id, new BedrockVersionStatus(
                    e.label != null ? e.label : e.id,
                    e.id,
                    has ? Kind.IMPORTED : Kind.MISSING,
                    det
            ));
        }

        return new ArrayList<>(map.values());
    }
}
