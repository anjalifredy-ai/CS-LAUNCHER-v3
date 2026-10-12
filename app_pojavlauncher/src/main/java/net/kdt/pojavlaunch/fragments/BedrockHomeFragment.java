package net.kdt.pojavlaunch.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.edition.BedrockProfile;
import net.kdt.pojavlaunch.edition.BedrockProfileStore;
import net.kdt.pojavlaunch.edition.BedrockVersionManager;
import net.kdt.pojavlaunch.edition.EditionMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/** Bedrock home — Day 3: validate APK after copy. */
public class BedrockHomeFragment extends Fragment {
    public static final String TAG = "bedrock_home";

    private final List<BedrockProfile> profiles = new ArrayList<>();
    private final List<String> versions = new ArrayList<>();
    private ProfileAdapter profileAdapter;
    private VersionAdapter versionAdapter;
    private TextView selectedName;
    private TextView ownedStatus;

    private ActivityResultLauncher<String> pickApkLauncher;

    public BedrockHomeFragment() {
        super(R.layout.fragment_bedrock_home);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickApkLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onApkPicked
        );
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        selectedName = root.findViewById(R.id.bedrock_selected_name);
        ownedStatus = root.findViewById(R.id.bedrock_owned_status);

        RecyclerView profileList = root.findViewById(R.id.bedrock_profile_list);
        RecyclerView versionList = root.findViewById(R.id.bedrock_version_list);
        profileList.setLayoutManager(new LinearLayoutManager(requireContext()));
        versionList.setLayoutManager(new LinearLayoutManager(requireContext()));

        profileAdapter = new ProfileAdapter();
        versionAdapter = new VersionAdapter();
        profileList.setAdapter(profileAdapter);
        versionList.setAdapter(versionAdapter);

        root.findViewById(R.id.bedrock_btn_add_profile).setOnClickListener(v -> showAddProfile());
        root.findViewById(R.id.bedrock_btn_import_apk).setOnClickListener(v -> {
            if (!EditionMode.isOfficialBedrockInstalled(requireContext())) {
                Toast.makeText(requireContext(),
                        "Pehle Play Store se official Minecraft install/own karo.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(requireContext(),
                    "Sirf apna official Minecraft APK choose karo.",
                    Toast.LENGTH_SHORT).show();
            pickApkLauncher.launch("application/vnd.android.package-archive");
        });
        root.findViewById(R.id.bedrock_btn_play).setOnClickListener(v -> {
            EditionMode.setMode(requireContext(), "bedrock");
            BedrockProfile sel = BedrockProfileStore.getSelected(requireContext());
            if (sel.versionId != null) {
                String id = sel.versionId.startsWith("Play Store") ? "playstore" : sel.versionId;
                BedrockVersionManager.registerVersion(requireContext(), id, sel.versionId);
            }
            if (getActivity() != null) {
                EditionMode.tryLaunchBedrock(getActivity());
            }
        });

        refresh();
    }

    private void onApkPicked(@Nullable Uri uri) {
        if (uri == null) {
            Toast.makeText(requireContext(), "No APK selected", Toast.LENGTH_SHORT).show();
            return;
        }
        BedrockProfile sel = BedrockProfileStore.getSelected(requireContext());
        String ver = sel.versionId != null ? sel.versionId : "Play Store (current)";
        String id = ver.startsWith("Play Store") ? "playstore" : ver;

        BedrockVersionManager.registerVersion(requireContext(), id, ver);

        ProgressDialog pd = new ProgressDialog(requireContext());
        pd.setTitle("Importing official APK");
        pd.setMessage("Copying + validating…");
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();

        Executors.newSingleThreadExecutor().execute(() -> {
            BedrockVersionManager.copyOfficialApk(requireContext().getApplicationContext(), id, uri,
                    new BedrockVersionManager.CopyProgress() {
                        @Override
                        public void onProgress(long copied, long totalOrMinus1) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                if (totalOrMinus1 > 0) {
                                    pd.setIndeterminate(false);
                                    pd.setMax(100);
                                    int pct = (int) Math.min(100, (copied * 100) / totalOrMinus1);
                                    pd.setProgress(pct);
                                    pd.setMessage((copied / (1024 * 1024)) + " MB…");
                                } else {
                                    pd.setMessage((copied / (1024 * 1024)) + " MB…");
                                }
                            });
                        }

                        @Override
                        public void onDone(boolean success, File apkFile, String error) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                try { pd.dismiss(); } catch (Exception ignored) {}
                                if (success && apkFile != null) {
                                    String detected = BedrockVersionManager.getDetectedVersionName(
                                            requireContext(), id);
                                    long mb = apkFile.length() / (1024 * 1024);
                                    Toast.makeText(requireContext(),
                                            "Day 3 OK: official Minecraft\n" +
                                                    "v" + (detected != null ? detected : "?") +
                                                    " · " + mb + " MB",
                                            Toast.LENGTH_LONG).show();
                                    if (detected != null) {
                                        BedrockProfileStore.setVersionForSelected(requireContext(), detected);
                                    }
                                } else {
                                    Toast.makeText(requireContext(),
                                            "Reject / fail: " + (error != null ? error : "unknown"),
                                            Toast.LENGTH_LONG).show();
                                }
                                refresh();
                            });
                        }
                    });
        });
    }

    private void refresh() {
        profiles.clear();
        profiles.addAll(BedrockProfileStore.list(requireContext()));
        profileAdapter.notifyDataSetChanged();

        versions.clear();
        for (String s : BedrockProfileStore.catalogVersions()) versions.add(s);
        versionAdapter.notifyDataSetChanged();

        BedrockProfile sel = BedrockProfileStore.getSelected(requireContext());
        if (selectedName != null) {
            String v = sel.versionId != null ? sel.versionId : "Play Store current";
            String id = v.startsWith("Play Store") ? "playstore" : v;
            boolean has = BedrockVersionManager.hasFullApk(requireContext(), id);
            String detected = BedrockVersionManager.getDetectedVersionName(requireContext(), id);
            String extra = has
                    ? (detected != null ? " · OK v" + detected : " · APK saved")
                    : "";
            selectedName.setText(sel.name + " · " + v + extra);
        }
        if (ownedStatus != null) {
            boolean ok = EditionMode.isOfficialBedrockInstalled(requireContext());
            ownedStatus.setText(ok
                    ? "✓ Official Minecraft installed — import/play allowed"
                    : "✗ Install official Minecraft from Play Store first");
            ownedStatus.setTextColor(ok
                    ? getResources().getColor(R.color.accent_light)
                    : 0xFFFF8A80);
        }
    }

    private void showAddProfile() {
        EditText input = new EditText(requireContext());
        input.setHint("Profile name");
        input.setText("Bedrock " + (profiles.size() + 1));
        new AlertDialog.Builder(requireContext())
                .setTitle("New Bedrock profile")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    BedrockProfileStore.add(requireContext(), input.getText().toString(), "Play Store (current)");
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final TextView title;
            VH(View v) { super(v); title = (TextView) v; }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setPadding(16, 18, 16, 18);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(13f);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            BedrockProfile bp = profiles.get(position);
            boolean sel = bp.id.equals(BedrockProfileStore.getSelectedId(requireContext()));
            String ver = bp.versionId != null ? bp.versionId : "—";
            h.title.setText((sel ? "● " : "○ ") + bp.name + "\n    " + ver);
            h.title.setBackgroundColor(sel ? 0x332196F3 : 0x00000000);
            h.title.setOnClickListener(v -> {
                BedrockProfileStore.setSelectedId(requireContext(), bp.id);
                refresh();
            });
        }

        @Override
        public int getItemCount() { return profiles.size(); }
    }

    private class VersionAdapter extends RecyclerView.Adapter<VersionAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final TextView title;
            VH(View v) { super(v); title = (TextView) v; }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setPadding(14, 14, 14, 14);
            tv.setTextColor(0xFFDDDDDD);
            tv.setTextSize(12f);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            String ver = versions.get(position);
            BedrockProfile sel = BedrockProfileStore.getSelected(requireContext());
            boolean active = ver.equals(sel.versionId)
                    || (sel.versionId == null && position == 0);
            String id = ver.startsWith("Play Store") ? "playstore" : ver;
            boolean has = BedrockVersionManager.hasFullApk(requireContext(), id);
            String detected = BedrockVersionManager.getDetectedVersionName(requireContext(), id);
            String mark = has
                    ? (detected != null ? "  ✓ v" + detected : "  ✓ APK")
                    : "  (no APK)";
            h.title.setText((active ? "▶ " : "   ") + ver + mark);
            h.title.setOnClickListener(v -> {
                BedrockProfileStore.setVersionForSelected(requireContext(), ver);
                BedrockVersionManager.registerVersion(requireContext(), id, ver);
                refresh();
            });
        }

        @Override
        public int getItemCount() { return versions.size(); }
    }
}
