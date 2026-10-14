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

        View sponsorPlay = view.findViewById(R.id.infrawire_card_play);
        if (sponsorPlay != null) sponsorPlay.setVisibility(View.GONE);
        int feedId = getResources().getIdentifier("infrawire_card_feed", "id", requireContext().getPackageName());
        View sponsorFeed = feedId != 0 ? view.findViewById(feedId) : null;
        if (sponsorFeed != null) sponsorFeed.setVisibility(View.GONE);

        updateNotificationBanner(view);

        view.post(() -> {
            if (isAdded() && !isRemoving()) {
                try { net.kdt.pojavlaunch.UiMotion.revealScreen(view); } catch (Throwable ignored) {}
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) {
            bindAccountData(view);
            bindProfileData(view);
            updateNotificationBanner(view);
        }
        try {
            if (getActivity() != null)
                net.kdt.pojavlaunch.edition.EditionMode.bindHeader(getActivity());
        } catch (Throwable ignored) {}
        try {
            net.kdt.pojavlaunch.remote.FirebaseSyncManager.setHomeBannerListener(() -> updateNotificationBanner(getView()));
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try { net.kdt.pojavlaunch.remote.FirebaseSyncManager.setHomeBannerListener(null); } catch (Throwable ignored) {}
    }

    private void updateNotificationBanner(View root) {
        if (root == null || getActivity() == null) return;
        View card = root.findViewById(R.id.card_notification);
        if (card == null) return;
        try {
            net.kdt.pojavlaunch.remote.FirebaseSyncManager.HomeBannerItem banner =
                    net.kdt.pojavlaunch.remote.FirebaseSyncManager.getLatestHomeBanner();
            if (banner == null) { card.setVisibility(View.GONE); return; }
            TextView tvTitle = root.findViewById(R.id.tv_notification_title);
            TextView tvBody = root.findViewById(R.id.tv_notification_body);
            View btnClose = root.findViewById(R.id.btn_dismiss_notification);
            if (tvTitle != null) tvTitle.setText(banner.title);
            if (tvBody != null) {
                String snippet = banner.body.replaceAll("[#*`>\\[\\]()-_]", "").trim();
                if (snippet.isEmpty()) tvBody.setVisibility(View.GONE);
                else { tvBody.setVisibility(View.VISIBLE); tvBody.setText(snippet); }
            }
            card.setVisibility(View.VISIBLE);
            card.setOnClickListener(v -> {
                try {
                    net.kdt.pojavlaunch.remote.FirebaseSyncManager.showMarkdownDialog(requireActivity(), banner.title, banner.body);
                } catch (Throwable t) {
                    Toast.makeText(requireContext(), banner.title, Toast.LENGTH_SHORT).show();
                }
            });
            if (btnClose != null) {
                btnClose.setOnClickListener(v -> {
                    try {
                        net.kdt.pojavlaunch.remote.FirebaseSyncManager.dismissBanner(banner.id, requireContext());
                    } catch (Throwable ignored) {}
                    card.setVisibility(View.GONE);
                });
            }
        } catch (Throwable t) {
            card.setVisibility(View.GONE);
        }
    }

    private void bindAccountData(View view) {
        MinecraftAccount account = PojavProfile.getCurrentProfileContent(requireContext(), null);
        TextView tvPlayerName = view.findViewById(R.id.tv_player_name);
        ImageView ivPlayerHead = view.findViewById(R.id.iv_player_head);
        TextView tvPlayerStatus = view.findViewById(R.id.tv_player_status);
        if (account != null) {
            if (tvPlayerName != null) tvPlayerName.setText(account.username);
            if (ivPlayerHead != null) {
                try {
                    Bitmap head = net.kdt.pojavlaunch.ui.SkinHead3DRenderer.resolve(getResources(), account);
                    if (head != null) ivPlayerHead.setImageBitmap(head);
                    else ivPlayerHead.setImageResource(R.drawable.ic_cs_logo_placeholder);
                } catch (Throwable t) {
                    ivPlayerHead.setImageResource(R.drawable.ic_cs_logo_placeholder);
                }
            }
            if (tvPlayerStatus != null) {
                boolean isOnline = account.accessToken != null && !account.accessToken.equals("0");
                tvPlayerStatus.setText(isOnline ? "Online" : "Offline");
            }
        }
    }

    private void bindProfileData(View view) {
        String profileKey = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "");
        LauncherProfiles.load();
        MinecraftProfile profile = LauncherProfiles.mainProfileJson.profiles.get(profileKey);
        TextView tvProfileName = view.findViewById(R.id.tv_profile_name);
        TextView tvFolderName = view.findViewById(R.id.tv_folder_name);
        TextView chipVersion = view.findViewById(R.id.chip_version);
        TextView chipLoader = view.findViewById(R.id.chip_loader);
        TextView chipJava = view.findViewById(R.id.chip_java);
        TextView chipRam = view.findViewById(R.id.chip_ram);
        ImageView ivProfileIcon = view.findViewById(R.id.iv_player_head);
        if (profile != null) {
            if (tvProfileName != null) tvProfileName.setText(profile.name != null ? profile.name : "Default");
            if (tvFolderName != null) tvFolderName.setText(profile.name != null ? profile.name : "Default");
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
