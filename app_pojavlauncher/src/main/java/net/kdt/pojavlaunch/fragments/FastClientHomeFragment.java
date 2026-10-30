package net.kdt.pojavlaunch.fragments;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.PojavProfile;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.profiles.ProfileIconCache;
import net.kdt.pojavlaunch.servers.ServerHubFragment;
import net.kdt.pojavlaunch.ui.PremiumPlayButtonView;
import net.kdt.pojavlaunch.value.MinecraftAccount;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;

public class FastClientHomeFragment extends Fragment {

    public static final String TAG = "FastClientHomeFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_fastclient, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            if (getActivity() != null)
                net.kdt.pojavlaunch.edition.EditionMode.bindHeader(getActivity());
        } catch (Throwable ignored) {}

        bindAccountData(view);
        bindProfileData(view);

        PremiumPlayButtonView btnPlay = view.findViewById(R.id.btn_play_main);
        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> {
                if (btnPlay.isStopMode()) {
                    btnPlay.cancelPendingLaunch();
                    return;
                }
                btnPlay.beginLaunch();
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
            });
        }

        View btnSelectProfile = view.findViewById(R.id.btn_select_profile);
        if (btnSelectProfile != null) {
            btnSelectProfile.setOnClickListener(v ->
                    Tools.swapFragment(requireActivity(), InstancePickerFragment.class, InstancePickerFragment.TAG, null));
        }

        View btnProfileSetting = view.findViewById(R.id.btn_profile_setting);
        if (btnProfileSetting != null) {
            btnProfileSetting.setOnClickListener(v ->
                    Tools.swapFragment(requireActivity(), ProfileEditorFragment.class, ProfileEditorFragment.TAG, null));
        }

        setupServerCards(view);

        View btnHostServers = view.findViewById(R.id.btn_add_server);
        if (btnHostServers != null) {
            if (btnHostServers instanceof TextView) {
                ((TextView) btnHostServers).setText("  HOST · CREATE SERVER");
            }
            btnHostServers.setOnClickListener(v -> openServerHub());
        }
        View btnHostMain = view.findViewById(R.id.btn_host_create_main);
        if (btnHostMain != null) {
            btnHostMain.setOnClickListener(v -> openServerHub());
        }
    }

    private void bindAccountData(View view) {
        TextView tvName = view.findViewById(R.id.tv_player_name);
        TextView tvStatus = view.findViewById(R.id.tv_player_status);
        try {
            MinecraftAccount acc = PojavProfile.getCurrentProfileContent(requireContext(), null);
            if (acc != null && acc.username != null) {
                if (tvName != null) tvName.setText(acc.username);
                if (tvStatus != null) tvStatus.setText(acc.isOffline() ? "Offline" : "Online");
            } else {
                if (tvName != null) tvName.setText("Player");
                if (tvStatus != null) tvStatus.setText("Offline");
            }
        } catch (Throwable t) {
            if (tvName != null) tvName.setText("Player");
        }
    }

    private void bindProfileData(View view) {
        TextView tvProfileName = view.findViewById(R.id.tv_profile_name);
        TextView chipVersion = view.findViewById(R.id.chip_version);
        TextView chipLoader = view.findViewById(R.id.chip_loader);
        TextView chipJava = view.findViewById(R.id.chip_java);
        TextView chipRam = view.findViewById(R.id.chip_ram);
        ImageView ivProfileIcon = view.findViewById(R.id.iv_profile_icon);
        try {
            LauncherProfiles.load();
            String profileKey = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            MinecraftProfile profile = profileKey != null ? LauncherProfiles.mainProfileJson.profiles.get(profileKey) : null;
            if (profile == null) return;
            if (tvProfileName != null) tvProfileName.setText(profile.name != null ? profile.name : "Default");
            if (chipVersion != null) chipVersion.setText(profile.lastVersionId != null ? profile.lastVersionId : "Unknown");
            String loader = "Vanilla";
            if (profile.lastVersionId != null) {
                String vid = profile.lastVersionId.toLowerCase();
                if (vid.contains("fabric")) loader = "Fabric";
                else if (vid.contains("forge")) loader = "Forge";
                else if (vid.contains("quilt")) loader = "Quilt";
                else if (vid.contains("neoforge")) loader = "NeoForge";
            }
            if (chipLoader != null) chipLoader.setText(loader);
            if (chipJava != null) chipJava.setText("Java");
            int ramMb = LauncherPreferences.PREF_RAM_ALLOCATION;
            if (chipRam != null) chipRam.setText(String.format("%.1fGB", ramMb / 1024.0));
            if (profile.icon != null && !profile.icon.equals("default") && ivProfileIcon != null) {
                try {
                    Drawable icon = ProfileIconCache.fetchIcon(getResources(), profileKey, profile.icon);
                    ivProfileIcon.setImageDrawable(icon);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private void openServerHub() {
        try {
            Bundle args = new Bundle();
            String key = LauncherPreferences.DEFAULT_PREF.getString(
                    LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            args.putString("profileKey", key);
            Tools.swapFragment(requireActivity(), ServerHubFragment.class,
                    ServerHubFragment.TAG, args);
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "Open Servers: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupServerCards(View view) {
        String[] serverAddresses = {
            "bananasmp.net", "fast.ascendiamc.com", "play.happymc.fun", "insanesmp.net", "fast.eternalnetwork.club"
        };
        String pkg = requireContext().getPackageName();
        for (int i = 0; i < serverAddresses.length; i++) {
            final String address = serverAddresses[i];
            int cid = getResources().getIdentifier("card_server_" + (i + 1), "id", pkg);
            View card = cid != 0 ? view.findViewById(cid) : null;
            if (card != null) {
                card.setOnClickListener(v ->
                        Toast.makeText(getContext(), "Joining " + address + "...", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private File getCurrentProfileDirectory() {
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        if (!Tools.isValidString(currentProfile)) return new File(Tools.DIR_GAME_NEW);
        LauncherProfiles.load();
        MinecraftProfile profileObject = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if (profileObject == null) return new File(Tools.DIR_GAME_NEW);
        return Tools.getGameDirPath(profileObject);
    }
}
