package net.kdt.pojavlaunch.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RightPaneHomeFragment extends Fragment {

    public static final String TAG = "RightPaneHomeFragment";
    public static final String CUSTOM_BG_PATH = Tools.DIR_DATA + "/custom_launcher_bg";

    private RecyclerView mRecyclerView;
    private HomeProfileAdapter mAdapter;
    private ItemTouchHelper mProfileTouchHelper;

    public RightPaneHomeFragment() {
        super(R.layout.fragment_right_pane_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        try {
            if (getActivity() != null)
                net.kdt.pojavlaunch.edition.EditionMode.bindHeader(getActivity());
        } catch (Throwable ignored) {}
        loadCustomWallpaper(view);

        mRecyclerView = view.findViewById(R.id.rv_home_profiles);
        if (mRecyclerView != null) {
            mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
            setupProfileDragAndDrop();
            setupProfileAdapter();
        }

        View fab = view.findViewById(R.id.fab_create_profile);
        if (fab != null) {
            fab.setScaleX(0.6f);
            fab.setScaleY(0.6f);
            fab.setAlpha(0f);
            fab.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200)
                    .setInterpolator(new DecelerateInterpolator()).start();
            fab.setOnClickListener(v -> {
                try {
                    Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class,
                            ProfileTypeSelectFragment.TAG, null);
                } catch (Throwable t) {
                    Toast.makeText(requireContext(), "Open create profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (getActivity() != null)
                net.kdt.pojavlaunch.edition.EditionMode.bindHeader(getActivity());
        } catch (Throwable ignored) {}
        setupProfileAdapter();
    }

    private void setupProfileDragAndDrop() {
        if (mProfileTouchHelper != null || mRecyclerView == null) return;
        mProfileTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        });
        mProfileTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    private void setupProfileAdapter() {
        if (getContext() == null || mRecyclerView == null) return;
        try {
            LauncherProfiles.load();
            List<String> keys = new ArrayList<>();
            List<MinecraftProfile> profiles = new ArrayList<>();
            if (LauncherProfiles.mainProfileJson != null && LauncherProfiles.mainProfileJson.profiles != null) {
                for (Map.Entry<String, MinecraftProfile> e : LauncherProfiles.mainProfileJson.profiles.entrySet()) {
                    keys.add(e.getKey());
                    profiles.add(e.getValue());
                }
            }
            mAdapter = new HomeProfileAdapter(keys, profiles,
                    new HomeProfileAdapter.OnProfileActionListener() {
                @Override
                public void onProfilePlay(String profileKey, MinecraftProfile profile) {
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey)
                            .apply();
                    ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                }
                @Override
                public void onProfileBrowse(String profileKey, MinecraftProfile profile) {
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey)
                            .apply();
                    try {
                        Tools.swapFragment(requireActivity(), ModsSearchFragment.class, ModsSearchFragment.TAG, null);
                    } catch (Throwable ignored) {}
                }
                @Override
                public void onProfileEdit(String profileKey, MinecraftProfile profile) {
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey)
                            .apply();
                    try {
                        Tools.swapFragment(requireActivity(), ProfileEditorFragment.class, ProfileEditorFragment.TAG, null);
                    } catch (Throwable ignored) {}
                }
                @Override public void onProfileAddShortcut(String profileKey, MinecraftProfile profile) {}
                @Override
                public void onProfileFavoriteChanged(String profileKey, MinecraftProfile profile, boolean favorite) {
                    try {
                        if (LauncherProfiles.setFavorite(profileKey, favorite)) setupProfileAdapter();
                    } catch (Throwable ignored) {}
                }
                @Override
                public void onProfileOrderChanged(List<String> orderedProfileKeys) {
                    try { LauncherProfiles.applyProfileOrder(orderedProfileKeys); } catch (Throwable ignored) {}
                }
            });
            try {
                mAdapter.setDragStartListener(holder -> {
                    if (mProfileTouchHelper != null) mProfileTouchHelper.startDrag(holder);
                });
            } catch (Throwable ignored) {}
            mRecyclerView.setAdapter(mAdapter);
        } catch (Throwable t) {
            Toast.makeText(getContext(), "Profiles load error", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCustomWallpaper(@NonNull View view) {
        ImageView wallpaper = view.findViewById(R.id.right_pane_wallpaper);
        if (wallpaper == null) return;
        File bgFile = new File(CUSTOM_BG_PATH);
        if (!bgFile.exists()) return;
        try {
            wallpaper.setImageDrawable(Drawable.createFromPath(bgFile.getAbsolutePath()));
            wallpaper.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {}
    }
}
