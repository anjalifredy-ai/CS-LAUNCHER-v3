package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

/**
 * CS Launcher Plus — Settings hub (Screen 9).
 * Cards open existing settings / about / account flows.
 */
public class CsSettingsCardsFragment extends Fragment {

    public static final String TAG = "CsSettingsCardsFragment";

    public CsSettingsCardsFragment() {
        super(R.layout.fragment_cs_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bind(view, R.id.card_java_tweaks, () -> openPrefs("java"));
        bind(view, R.id.card_misc, () -> openPrefs("misc"));
        bind(view, R.id.card_video, () -> openPrefs("video"));
        bind(view, R.id.card_experimental, () -> openPrefs("experimental"));

        bind(view, R.id.card_accounts, () -> {
            try {
                Tools.swapFragment(requireActivity(), CsAccountHubFragment.class,
                        CsAccountHubFragment.TAG, null);
            } catch (Throwable t) {
                Toast.makeText(requireContext(), "Accounts", Toast.LENGTH_SHORT).show();
            }
        });

        bind(view, R.id.card_profiles, () -> {
            try {
                Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class,
                        ProfileTypeSelectFragment.TAG, null);
            } catch (Throwable t) {
                Toast.makeText(requireContext(), "Profiles", Toast.LENGTH_SHORT).show();
            }
        });

        bind(view, R.id.card_launcher, () -> openPrefs("launcher"));

        bind(view, R.id.card_about, () -> {
            try {
                Tools.swapFragment(requireActivity(), CsAboutFragment.class,
                        CsAboutFragment.TAG, null);
            } catch (Throwable t) {
                try {
                    Tools.swapFragment(requireActivity(), AboutFragment.class,
                            AboutFragment.TAG, null);
                } catch (Throwable t2) {
                    Toast.makeText(requireContext(), "About", Toast.LENGTH_SHORT).show();
                }
            }
        });

        View home = view.findViewById(R.id.btn_home);
        if (home != null) {
            home.setOnClickListener(v -> {
                try {
                    Tools.swapFragment(requireActivity(), RightPaneHomeFragment.class,
                            RightPaneHomeFragment.TAG, null);
                } catch (Throwable ignored) {}
            });
        }
    }

    private void bind(View root, int id, Runnable action) {
        View v = root.findViewById(id);
        if (v != null) v.setOnClickListener(x -> action.run());
    }

    private void openPrefs(String section) {
        try {
            // Prefer existing launcher preference fragment if present
            Class<?> cls = Class.forName("net.kdt.pojavlaunch.prefs.LauncherPreferenceFragment");
            Tools.swapFragment(requireActivity(), (Class<? extends Fragment>) cls,
                    "LauncherPreferenceFragment", null);
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "Settings: " + section, Toast.LENGTH_SHORT).show();
        }
    }
}
