package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
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
        View header = view.findViewById(R.id.skin_overview_header);
        header.setAlpha(0f);
        header.setTranslationY(-12f * getResources().getDisplayMetrics().density);
        header.animate().alpha(1f).translationY(0f).setDuration(240)
                .setInterpolator(new FastOutSlowInInterpolator()).start();
        applyPressAnimation(view.findViewById(R.id.skin_overview_back));
    }

    @Override public void onResume() { super.onResume(); if (slotContainer != null) loadLibrary(); }

    private void loadLibrary() {
        account = PojavProfile.getCurrentProfileContent(requireContext(), null);
        if (account == null) {
            Tools.dialog(requireContext(), "Authentication Required", "Please select an account first.");
            return;
        }
        store = new SkinSlotStore(requireContext(), account);
        accountText.setText(account.username);
        int active = store.getActiveSlot();
        activeText.setText(active >= 0 ? "SLOT " + (active + 1) + " ACTIVE" : "NO ACTIVE SKIN");
        activeText.setTextColor(active >= 0 ? 0xFF82F3F4 : 0xFF22C9CB);
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
            status.setTextColor(isActive ? 0xFF82F3F4 : 0xFF9A9CA6);
            ((TextView) card.findViewById(R.id.overview_slot_name)).setText(
                    filled ? store.get(i).name : "Empty Slot");
            ((TextView) card.findViewById(R.id.overview_slot_model)).setText(
                    filled ? store.get(i).model.toUpperCase() + " MODEL" : "NO SKIN SAVED");
            SkinSlotPreviewView preview = card.findViewById(R.id.overview_slot_preview);
            preview.setSkin(filled ? store.fileFor(i) : null);
            card.findViewById(R.id.overview_slot_empty).setVisibility(filled ? View.GONE : View.VISIBLE);
            card.setOnClickListener(v -> openStudio(slot));
            applyPressAnimation(card);
            card.setAlpha(0f);
            card.setTranslationY(12f * getResources().getDisplayMetrics().density);
            card.animate().alpha(1f).translationY(0f).setStartDelay(i * 35L)
                    .setDuration(240).setInterpolator(new FastOutSlowInInterpolator()).start();
            slotContainer.addView(card);
            if (i < SkinSlotStore.SLOT_COUNT - 1) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
                lp.setMarginEnd((int)(10 * getResources().getDisplayMetrics().density));
                card.setLayoutParams(lp);
            }
        }
    }

    private void applyPressAnimation(@Nullable View target) {
        if (target == null) return;
        target.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(90).start();
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120)
                        .setInterpolator(new FastOutSlowInInterpolator()).start();
            }
            return false;
        });
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
