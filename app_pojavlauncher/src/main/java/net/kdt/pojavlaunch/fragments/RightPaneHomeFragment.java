package net.kdt.pojavlaunch.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
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
    private boolean mProfileOrderDirty;

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
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        setupProfileDragAndDrop();
        setupProfileAdapter();

        View fab = view.findViewById(R.id.fab_create_profile);
        if (fab != null) {
            fab.setScaleX(0.6f);
            fab.setScaleY(0.6f);
            fab.setAlpha(0f);
            fab.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            fab.animate()
                    .scaleX(1f).scaleY(1f)
                    .alpha(1f)
                    .setDuration(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> fab.setLayerType(View.LAYER_TYPE_NONE, null))
                    .start();
            fab.setOnClickListener(v -> {
                v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(60)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                        .start();
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
        try {
            if (getActivity() != null)
                net.kdt.pojavlaunch.edition.EditionMode.bindHeader(getActivity());
        } catch (Throwable ignored) {}
        super.onResume();
        setupProfileAdapter();
    }

    private void loadCustomWallpaper(View view) {
        ImageView wallpaper = view.findViewById(R.id.right_pane_wallpaper);
        if (wallpaper == null) return;
        try {
            File f = new File(CUSTOM_BG_PATH);
            if (f.exists()) {
                wallpaper.setImageDrawable(Drawable.createFromPath(f.getAbsolutePath()));
                wallpaper.setVisibility(View.VISIBLE);
            }
        } catch (Throwable ignored) {}
    }

    private void setupProfileDragAndDrop() {
        if (mProfileTouchHelper != null) return;
        mProfileTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                if (mAdapter == null) return false;
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false;
                mAdapter.moveItem(from, to);
                mProfileOrderDirty = true;
                return true;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
            @Override public void clearView(@NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (mProfileOrderDirty && mAdapter != null) {
                    mProfileOrderDirty = false;
                    mAdapter.persistOrder();
                }
            }
        });
        mProfileTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    private void setupProfileAdapter() {
        if (getContext() == null || mRecyclerView == null) return;
        try {
            LauncherProfiles.load();
            List<HomeProfileAdapter.Item> items = new ArrayList<>();
            Map<String, MinecraftProfile> map = LauncherProfiles.mainProfileJson != null
                    ? LauncherProfiles.mainProfileJson.profiles : null;
            if (map != null) {
                for (Map.Entry<String, MinecraftProfile> e : map.entrySet()) {
                    items.add(new HomeProfileAdapter.Item(e.getKey(), e.getValue()));
                }
            }
            mAdapter = new HomeProfileAdapter(items, new HomeProfileAdapter.Listener() {
                @Override public void onPlay(String key) {
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key).apply();
                    ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                }
                @Override public void onEdit(String key) {
                    Bundle b = new Bundle();
                    b.putString(ProfileEditorFragment.BUNDLE_SELECT_PROFILE_KEY, key);
                    Tools.swapFragment(requireActivity(), ProfileEditorFragment.class,
                            ProfileEditorFragment.TAG, b);
                }
                @Override public void onSelect(String key) {
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key).apply();
                    ExtraCore.setValue(ExtraConstants.REFRESH_VERSION_SPINNER, key);
                }
            });
            mRecyclerView.setAdapter(mAdapter);
            try {
                mRecyclerView.setLayoutAnimation(
                        AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fall_down));
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            Toast.makeText(getContext(), "Profiles load error", Toast.LENGTH_SHORT).show();
        }
    }
}
