package net.kdt.pojavlaunch.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.servers.ServerHubFragment;
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
    private View mRootView;
    private TextView mSelectedName;
    private TextView mSelectedMeta;

    public RightPaneHomeFragment() {
        super(R.layout.fragment_right_pane_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mRootView = view;
        try {
            if (getActivity() != null)
                net.kdt.pojavlaunch.edition.EditionMode.bindHeader(getActivity());
        } catch (Throwable ignored) {}
        loadCustomWallpaper(view);

        mSelectedName = view.findViewById(R.id.tv_selected_profile_name);
        mSelectedMeta = view.findViewById(R.id.tv_selected_profile_meta);
        refreshSelectedProfileLabel();

        mRecyclerView = view.findViewById(R.id.rv_home_profiles);
        if (mRecyclerView != null) {
            mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
            setupProfileDragAndDrop();
            setupProfileAdapter();
        }

        View browse = view.findViewById(R.id.btn_browse_resources);
        if (browse != null) {
            browse.setOnClickListener(v -> {
                try {
                    ResourceBrowserDialog.show(requireActivity());
                } catch (Throwable t) {
                    try {
                        Tools.swapFragment(requireActivity(), ModsSearchFragment.class, ModsSearchFragment.TAG, null);
                    } catch (Throwable t2) {
                        Toast.makeText(requireContext(), "Browse resources", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        View manager = view.findViewById(R.id.btn_manager);
        if (manager != null) {
            manager.setOnClickListener(v -> {
                try {
                    Tools.swapFragment(requireActivity(), ModsSearchFragment.class, ModsSearchFragment.TAG, null);
                } catch (Throwable t) {
                    Toast.makeText(requireContext(), "Manager", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View host = view.findViewById(R.id.btn_host_servers);
        if (host != null) {
            host.setOnClickListener(v -> {
                try {
                    Bundle args = new Bundle();
                    String key = LauncherPreferences.DEFAULT_PREF.getString(
                            LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
                    if (key != null) args.putString("profileKey", key);
                    Tools.swapFragment(requireActivity(), ServerHubFragment.class, ServerHubFragment.TAG, args);
                } catch (Throwable t) {
                    Toast.makeText(requireContext(), "Servers / Host", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View play = view.findViewById(R.id.btn_play_main);
        if (play != null) {
            play.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));
        }

        View edit = view.findViewById(R.id.btn_edit_selected);
        if (edit != null) {
            edit.setOnClickListener(v -> {
                try {
                    Tools.swapFragment(requireActivity(), ProfileEditorFragment.class, ProfileEditorFragment.TAG, null);
                } catch (Throwable t) {
                    Toast.makeText(requireContext(), "Edit profile", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View selected = view.findViewById(R.id.btn_selected_profile);
        if (selected != null) {
            selected.setOnClickListener(v -> showProfilePicker());
        }

        View fab = view.findViewById(R.id.fab_create_profile);
        if (fab != null) {
            fab.setScaleX(0.6f);
            fab.setScaleY(0.6f);
            fab.setAlpha(0f);
            fab.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200)
                    .setInterpolator(new DecelerateInterpolator()).start();
            fab.setOnClickListener(v -> openCreateProfile());
        }
    }

    private void openCreateProfile() {
        try {
            Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class,
                    ProfileTypeSelectFragment.TAG, null);
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "Open create profile", Toast.LENGTH_SHORT).show();
        }
    }

    /** CS Plus style: list + Create New Instance */
    private void showProfilePicker() {
        try {
            LauncherProfiles.load();
            List<String> keys = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            if (LauncherProfiles.mainProfileJson != null
                    && LauncherProfiles.mainProfileJson.profiles != null) {
                for (Map.Entry<String, MinecraftProfile> e : LauncherProfiles.mainProfileJson.profiles.entrySet()) {
                    keys.add(e.getKey());
                    MinecraftProfile p = e.getValue();
                    String name = (p != null && p.name != null && !p.name.isEmpty()) ? p.name : e.getKey();
                    String ver = (p != null && p.lastVersionId != null) ? p.lastVersionId : "";
                    labels.add(ver.isEmpty() ? name : (name + "  ·  " + ver));
                }
            }
            AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
            b.setTitle("Select Profile");
            if (labels.isEmpty()) {
                b.setMessage("No instances yet");
            } else {
                b.setItems(labels.toArray(new String[0]), (d, which) -> {
                    String key = keys.get(which);
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key)
                            .apply();
                    refreshSelectedProfileLabel();
                    setupProfileAdapter();
                });
            }
            b.setPositiveButton("+ Create New Instance", (d, w) -> openCreateProfile());
            b.setNegativeButton("Close", null);
            b.show();
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "Profile picker error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (getActivity() != null)
                net.kdt.pojavlaunch.edition.EditionMode.bindHeader(getActivity());
        } catch (Throwable ignored) {}
        refreshSelectedProfileLabel();
        setupProfileAdapter();
    }

    public void reloadBackground() {
        View root = getView() != null ? getView() : mRootView;
        if (root != null) loadCustomWallpaper(root);
    }

    private void refreshSelectedProfileLabel() {
        if (mSelectedName == null) return;
        try {
            LauncherProfiles.load();
            String key = LauncherPreferences.DEFAULT_PREF.getString(
                    LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            MinecraftProfile p = null;
            if (key != null && LauncherProfiles.mainProfileJson != null
                    && LauncherProfiles.mainProfileJson.profiles != null) {
                p = LauncherProfiles.mainProfileJson.profiles.get(key);
            }
            if (p == null && LauncherProfiles.mainProfileJson != null
                    && LauncherProfiles.mainProfileJson.profiles != null
                    && !LauncherProfiles.mainProfileJson.profiles.isEmpty()) {
                Map.Entry<String, MinecraftProfile> e =
                        LauncherProfiles.mainProfileJson.profiles.entrySet().iterator().next();
                key = e.getKey();
                p = e.getValue();
            }
            String name = (p != null && p.name != null && !p.name.isEmpty()) ? p.name
                    : (key != null ? key : "Default");
            mSelectedName.setText(name);
            if (mSelectedMeta != null) {
                String ver = (p != null && p.lastVersionId != null) ? p.lastVersionId : "";
                mSelectedMeta.setText(ver);
            }
        } catch (Throwable ignored) {
            mSelectedName.setText("Default");
        }
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
                    refreshSelectedProfileLabel();
                    ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                }
                @Override
                public void onProfileBrowse(String profileKey, MinecraftProfile profile) {
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey)
                            .apply();
                    refreshSelectedProfileLabel();
                    try {
                        ResourceBrowserDialog.show(requireActivity());
                    } catch (Throwable t) {
                        try {
                            Tools.swapFragment(requireActivity(), ModsSearchFragment.class, ModsSearchFragment.TAG, null);
                        } catch (Throwable ignored) {}
                    }
                }
                @Override
                public void onProfileEdit(String profileKey, MinecraftProfile profile) {
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey)
                            .apply();
                    refreshSelectedProfileLabel();
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
        if (!bgFile.exists()) {
            wallpaper.setVisibility(View.GONE);
            return;
        }
        try {
            wallpaper.setImageDrawable(Drawable.createFromPath(bgFile.getAbsolutePath()));
            wallpaper.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
            wallpaper.setVisibility(View.GONE);
        }
    }
}
