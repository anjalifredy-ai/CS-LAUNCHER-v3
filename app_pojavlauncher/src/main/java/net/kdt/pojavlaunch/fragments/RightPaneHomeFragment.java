package net.kdt.pojavlaunch.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.servers.ServerHubFragment;
import net.kdt.pojavlaunch.servers.ServerListSync;
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
            play.setOnClickListener(v -> {
                try {
                    String key = LauncherPreferences.DEFAULT_PREF.getString(
                            LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
                    if (key != null && getContext() != null)
                        ServerListSync.syncToGameDir(requireContext(), key);
                } catch (Throwable ignored) {}
                ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
            });
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

    private void showProfilePicker() {
        try {
            LauncherProfiles.load();
            final List<String> keys = new ArrayList<>();
            final List<MinecraftProfile> profiles = new ArrayList<>();
            if (LauncherProfiles.mainProfileJson != null
                    && LauncherProfiles.mainProfileJson.profiles != null) {
                for (Map.Entry<String, MinecraftProfile> e : LauncherProfiles.mainProfileJson.profiles.entrySet()) {
                    keys.add(e.getKey());
                    profiles.add(e.getValue());
                }
            }

            final Dialog dialog = new Dialog(requireContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            View root = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_profile_picker, null, false);
            dialog.setContentView(root);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            TextView count = root.findViewById(R.id.pp_instance_count);
            TextView currentName = root.findViewById(R.id.pp_current_name);
            TextView create = root.findViewById(R.id.pp_create_new);
            TextView close = root.findViewById(R.id.pp_close);
            RecyclerView list = root.findViewById(R.id.pp_list);

            if (count != null) count.setText(keys.size() + " instance" + (keys.size() == 1 ? "" : "s"));

            final String activeKey = LauncherPreferences.DEFAULT_PREF.getString(
                    LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if (currentName != null) {
                String cn = "—";
                if (activeKey != null && LauncherProfiles.mainProfileJson != null
                        && LauncherProfiles.mainProfileJson.profiles != null) {
                    MinecraftProfile ap = LauncherProfiles.mainProfileJson.profiles.get(activeKey);
                    if (ap != null && ap.name != null && !ap.name.isEmpty()) cn = ap.name;
                    else if (activeKey != null) cn = activeKey;
                }
                currentName.setText(cn);
            }

            if (list != null) {
                list.setLayoutManager(new LinearLayoutManager(requireContext()));
                list.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View row = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_profile_picker_row, parent, false);
                        return new RecyclerView.ViewHolder(row) {};
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        String key = keys.get(position);
                        MinecraftProfile p = profiles.get(position);
                        String name = (p != null && p.name != null && !p.name.isEmpty()) ? p.name : key;
                        String ver = (p != null && p.lastVersionId != null) ? p.lastVersionId : "";
                        TextView n = holder.itemView.findViewById(R.id.pp_row_name);
                        TextView m = holder.itemView.findViewById(R.id.pp_row_meta);
                        TextView active = holder.itemView.findViewById(R.id.pp_row_active);
                        TextView editBtn = holder.itemView.findViewById(R.id.pp_row_edit);
                        if (n != null) n.setText(name);
                        if (m != null) m.setText(ver);
                        boolean isActive = key.equals(activeKey);
                        if (active != null) active.setVisibility(isActive ? View.VISIBLE : View.GONE);
                        holder.itemView.setOnClickListener(v -> {
                            LauncherPreferences.DEFAULT_PREF.edit()
                                    .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key)
                                    .apply();
                            refreshSelectedProfileLabel();
                            setupProfileAdapter();
                            dialog.dismiss();
                        });
                        if (editBtn != null) {
                            editBtn.setOnClickListener(v -> {
                                LauncherPreferences.DEFAULT_PREF.edit()
                                        .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key)
                                        .apply();
                                dialog.dismiss();
                                try {
                                    Tools.swapFragment(requireActivity(), ProfileEditorFragment.class,
                                            ProfileEditorFragment.TAG, null);
                                } catch (Throwable ignored) {}
                            });
                        }
                    }

                    @Override
                    public int getItemCount() {
                        return keys.size();
                    }
                });
            }

            if (create != null) create.setOnClickListener(v -> {
                dialog.dismiss();
                openCreateProfile();
            });
            if (close != null) close.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
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
        try {
            String key = LauncherPreferences.DEFAULT_PREF.getString(
                    LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if (key != null && getContext() != null)
                ServerListSync.importFromGameDir(requireContext(), key);
        } catch (Throwable ignored) {}
        refreshSelectedProfileLabel();
        setupProfileAdapter();
    }

    public void reloadBackground() {
        View root = getView() != null ? getView() : mRootView;
        if (root != null) loadCustomWallpaper(root);
    }

    private void refreshSelectedProfileLabel() {
        if (mSelectedName == null && getView() == null) return;
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
            String ver = (p != null && p.lastVersionId != null) ? p.lastVersionId : "";
            if (mSelectedName != null) mSelectedName.setText(name);
            if (mSelectedMeta != null) mSelectedMeta.setText(ver);
            View root = getView() != null ? getView() : mRootView;
            if (root != null) {
                TextView playVer = root.findViewById(R.id.tv_play_version);
                if (playVer != null) playVer.setText(ver);
                TextView chip = root.findViewById(R.id.tv_hero_version_chip);
                if (chip != null) chip.setText(ver.isEmpty() ? "Java" : ver);
            }
        } catch (Throwable ignored) {
            if (mSelectedName != null) mSelectedName.setText("Default");
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
                    try {
                        ServerListSync.syncToGameDir(requireContext(), profileKey);
                    } catch (Throwable ignored) {}
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
