package net.kdt.pojavlaunch.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

/** Bedrock home: profiles + versions + isolation prepare + official Play. */
public class BedrockHomeFragment extends Fragment {
    public static final String TAG = "bedrock_home";

    private final List<BedrockProfile> profiles = new ArrayList<>();
    private final List<String> versions = new ArrayList<>();
    private ProfileAdapter profileAdapter;
    private VersionAdapter versionAdapter;
    private TextView selectedName;
    private TextView ownedStatus;

    public BedrockHomeFragment() {
        super(R.layout.fragment_bedrock_home);
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
            selectedName.setText(sel.name + " · " + v);
        }
        if (ownedStatus != null) {
            boolean ok = EditionMode.isOfficialBedrockInstalled(requireContext());
            ownedStatus.setText(ok
                    ? "✓ Official Minecraft installed — Play allowed"
                    : "✗ Minecraft not installed — buy/install from Play Store");
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
            h.title.setText((active ? "▶ " : "   ") + ver
                    + (position == 0 ? "  (Play target)" : "  (isolation ready)"));
            h.title.setOnClickListener(v -> {
                BedrockProfileStore.setVersionForSelected(requireContext(), ver);
                String id = ver.startsWith("Play Store") ? "playstore" : ver;
                BedrockVersionManager.registerVersion(requireContext(), id, ver);
                Toast.makeText(requireContext(),
                        "Isolation folder created for " + ver + ". Native multi-version host still Phase 3.",
                        Toast.LENGTH_LONG).show();
                refresh();
            });
        }

        @Override
        public int getItemCount() { return versions.size(); }
    }
}
