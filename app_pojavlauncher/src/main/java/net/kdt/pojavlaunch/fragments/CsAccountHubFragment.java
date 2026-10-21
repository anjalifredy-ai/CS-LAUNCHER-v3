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
 * CS Launcher Plus — Account Hub (Screen 2).
 * Wires Microsoft / Ely.by / Offline cards to existing login fragments.
 */
public class CsAccountHubFragment extends Fragment {

    public static final String TAG = "CsAccountHubFragment";

    public CsAccountHubFragment() {
        super(R.layout.fragment_cs_account_hub);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        View ms = view.findViewById(R.id.card_microsoft);
        if (ms != null) {
            ms.setOnClickListener(v -> {
                try {
                    Tools.swapFragment(requireActivity(), MicrosoftLoginFragment.class,
                            MicrosoftLoginFragment.TAG, null);
                } catch (Throwable t) {
                    Toast.makeText(requireContext(), "Microsoft login", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View ely = view.findViewById(R.id.card_elyby);
        if (ely != null) {
            ely.setOnClickListener(v -> {
                try {
                    Tools.swapFragment(requireActivity(), ElybyLoginFragment.class,
                            ElybyLoginFragment.TAG, null);
                } catch (Throwable t) {
                    try {
                        Tools.swapFragment(requireActivity(), ElybyBrowserFragment.class,
                                ElybyBrowserFragment.TAG, null);
                    } catch (Throwable t2) {
                        Toast.makeText(requireContext(), "Ely.by login", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        View off = view.findViewById(R.id.card_offline);
        if (off != null) {
            off.setOnClickListener(v -> {
                try {
                    Tools.swapFragment(requireActivity(), LocalLoginFragment.class,
                            LocalLoginFragment.TAG, null);
                } catch (Throwable t) {
                    Toast.makeText(requireContext(), "Offline account", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Top bar Home
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
