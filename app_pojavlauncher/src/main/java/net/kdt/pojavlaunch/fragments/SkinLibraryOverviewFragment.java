package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.PojavProfile;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.skins.SkinSlotPreviewView;
import net.kdt.pojavlaunch.skins.SkinSlotStore;
import net.kdt.pojavlaunch.value.MinecraftAccount;

/** Read-first skin library: three slot summaries, then drill into Skin Studio. */
public class SkinLibraryOverviewFragment extends Fragment {
    public static final String TAG = "SKIN_LIBRARY_OVERVIEW";
    private LinearLayout slotContainer;
    private TextView accountText, activeText;
    private MinecraftAccount account;
    private SkinSlotStore store;

    public SkinLibraryOverviewFragment() { super(R.layout.fragment_skin_library_overview); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        slotContainer = view.findViewById(R.id.skin_overview_slots);
        accountText = view.findViewById(R.id.skin_overview_account);
        activeText = view.findViewById(R.id.skin_overview_active);
        view.findViewById(R.id.skin_overview_back).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
        loadLibrary();
        view.findViewById(R.id.skin_overview_header).setAlpha(0f);
        view.findViewById(R.id.skin_overview_header).animate().alpha(1f).setDuration(180).start();
    }

    @Override public void onResume() { super.onResume(); if (slotContainer != null) loadLibrary(); }

    private void loadLibrary() {
        account = PojavProfile.getCurrentProfileContent(requireContext(), null);
        if (account == null) {
            Tools.dialog(requireContext(), "Authentication Required", "Please select an account first.");
            return;
        }
        store = new SkinSlotStore(requireContext(), account);
        accountText.setText(account.username + "  •  Three saved looks, one active skin");
        int active = store.getActiveSlot();
        activeText.setText(active >= 0 ? "ACTIVE • SLOT " + (active + 1) : "NO ACTIVE SKIN");
        slotContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < SkinSlotStore.SLOT_COUNT; i++) {
            final int slot = i;
            View card = inflater.inflate(R.layout.item_skin_overview_slot, slotContainer, false);
            boolean filled = store.isFilled(i), isActive = active == i;
            card.setBackgroundResource(isActive ? R.drawable.bg_skin_slot_active : R.drawable.bg_skin_slot_idle);
            ((TextView) card.findViewById(R.id.overview_slot_number)).setText("SKIN SLOT " + (i + 1));
            TextView status = card.findViewById(R.id.overview_slot_status);
            status.setText(isActive ? "ACTIVE" : filled ? "SAVED" : "EMPTY");
            status.setTextColor(isActive ? 0xFFC5F0D0 : 0xFF8CAF96);
            ((TextView) card.findViewById(R.id.overview_slot_name)).setText(
                    filled ? store.get(i).name : "Empty Slot");
            ((TextView) card.findViewById(R.id.overview_slot_model)).setText(
                    filled ? store.get(i).model.toUpperCase() + " MODEL" : "NO SKIN SAVED");
            SkinSlotPreviewView preview = card.findViewById(R.id.overview_slot_preview);
            preview.setSkin(filled ? store.fileFor(i) : null);
            card.findViewById(R.id.overview_slot_empty).setVisibility(filled ? View.GONE : View.VISIBLE);
            card.setOnClickListener(v -> openStudio(slot));
            card.setAlpha(0f); card.setTranslationY(14f);
            card.animate().alpha(1f).translationY(0f).setStartDelay(i * 55L)
                    .setDuration(240).start();
            slotContainer.addView(card);
            if (i < SkinSlotStore.SLOT_COUNT - 1) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
                lp.setMarginEnd((int)(10 * getResources().getDisplayMetrics().density));
                card.setLayoutParams(lp);
            }
        }
    }

    private void openStudio(int slot) {
        Bundle args = new Bundle();
        args.putInt(SkinManagerFragment.ARG_SELECTED_SLOT, slot);
        Fragment parent = getParentFragment();
        if (parent instanceof MainMenuFragment) {
            ((MainMenuFragment) parent).openChildPane(SkinManagerFragment.class, SkinManagerFragment.TAG, args);
        } else {
            Tools.swapFragment(requireActivity(), SkinManagerFragment.class,
                    SkinManagerFragment.TAG, args);
        }
    }
}
