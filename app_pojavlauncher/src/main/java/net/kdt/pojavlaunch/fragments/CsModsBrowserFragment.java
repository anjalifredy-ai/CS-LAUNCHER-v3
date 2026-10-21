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
 * CS Plus Mods Browser shell. UI is CS-themed; data still uses existing ModsSearchFragment.
 */
public class CsModsBrowserFragment extends Fragment {

    public static final String TAG = "CsModsBrowserFragment";

    public CsModsBrowserFragment() {
        super(R.layout.fragment_cs_mods_browser);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Immediately hand off to full working mod search so install still works
        view.post(() -> {
            try {
                Tools.swapFragment(requireActivity(), ModsSearchFragment.class,
                        ModsSearchFragment.TAG, null);
            } catch (Throwable t) {
                Toast.makeText(requireContext(), "Mods browser", Toast.LENGTH_SHORT).show();
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
}
