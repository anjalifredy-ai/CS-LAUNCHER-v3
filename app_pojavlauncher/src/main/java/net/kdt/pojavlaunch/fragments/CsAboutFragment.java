package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

/** CS Launcher Plus — About (Screen 10). */
public class CsAboutFragment extends Fragment {

    public static final String TAG = "CsAboutFragment";

    public CsAboutFragment() {
        super(R.layout.fragment_cs_about);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
