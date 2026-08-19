package net.kdt.pojavlaunch.multirt;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.BaseActivity;
import net.kdt.pojavlaunch.NewJREUtil;
import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.UiMotion;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.ProgressListener;

import com.kdt.mcgui.ProgressLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dedicated full-screen first-launch runtime installer (Items 5+6).
 *
 * State 1 — SELECT: Java 8/17/21/25 cards (recommended picks pre-selected,
 * installed ones marked, unsupported ones hidden), one white CTA or Skip.
 * State 2 — INSTALL: a distinct system-install deck with an aggregate amber
 * ring, per-runtime progress bars with MB/speed/ETA, sequential chaining,
 * individual retry, and a finish CTA. Completely separate visuals+animations
 * from the normal download deck.
 */
public class RuntimeSetupActivity extends BaseActivity {

    private static final String PREF_SHOWN = "runtimeWizardShown";

    public static boolean wasShown() {
        return LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_SHOWN, false);
    }

    private static final int[] MAJORS = {8, 17, 21, 25};
    private static final int[] ROLE_RES = {
            R.string.rw_java8_role, R.string.rw_java17_role,
            R.string.rw_java21_role, R.string.rw_java25_role};
    private static final int[] SIZE_ESTIMATE_MB = {45, 55, 60, 65};

    // selection state
    private LinearLayout mSelectContainer, mCardContainer;
    private TextView mSkipButton;
    private TextView mInstallButton;
    private final List<CardState> mCards = new ArrayList<>();

    // install state
    private LinearLayout mInstallContainer, mInstallRows;
    private RuntimeRingView mRing;
    private TextView mOverallPercent, mOverallStep, mStatusLine;
    private TextView mDoneButton;
    private final List<InstallRow> mRows = new ArrayList<>();
    private boolean mInstalling = false;
    private int mDoneCount = 0;
    private int mCurrentPct = 0;

    private final java.util.Map<String,ProgressListener> mProgressHooks=new java.util.HashMap<>();

    private static class CardState {
        int major; View root; TextView chip; boolean installed; boolean selectable;
        boolean selected; NewJREUtil.ExternalRuntime runtime;
    }

    private static class InstallRow {
        int major; View root; TextView chip; TextView stats; TextView stepText;
        TextView bigPercent;
        ProgressBar bar; View spinner; View check;
        NewJREUtil.ExternalRuntime runtime; String progressRecord; java.io.File archive; volatile int progress;
        int state; // 0 queued, 1 downloading, 2 downloaded, 3 ready, 4 failed
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runtime_setup);

        mSelectContainer = findViewById(R.id.rs_select_container);
        mCardContainer = findViewById(R.id.rs_card_container);
        mSkipButton = findViewById(R.id.rs_btn_skip);
        mInstallButton = findViewById(R.id.rs_btn_install);
        mInstallContainer = findViewById(R.id.rs_install_container);
        mInstallRows = findViewById(R.id.rs_install_rows);
        mRing = findViewById(R.id.rs_ring);
        mOverallPercent = findViewById(R.id.rs_overall_percent);
        mOverallStep = findViewById(R.id.rs_overall_step);
        mStatusLine = findViewById(R.id.rs_status_line);
        mDoneButton = findViewById(R.id.rs_btn_done);

        buildCards();

        // Auto-skip when there is genuinely nothing to offer
        boolean anySelectable = false;
        for (CardState c : mCards) if (c.selectable) anySelectable = true;
        if (!anySelectable) {
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean(PREF_SHOWN, true).apply();
            finish();
            return;
        }

        // Flag as shown only after the UI composed successfully — a crash during
        // an earlier attempt must not permanently suppress the onboarding.
        LauncherPreferences.DEFAULT_PREF.edit().putBoolean(PREF_SHOWN, true).apply();

        if (!Tools.isOnline(this)) {
            findViewById(R.id.rs_offline_note).setVisibility(View.VISIBLE);
        }

        mSkipButton.setOnClickListener(v -> finish());
        mInstallButton.setOnClickListener(v -> beginInstall());
        mDoneButton.setOnClickListener(v -> finish());
        UiMotion.pressFeedback(mSkipButton, mInstallButton, mDoneButton);
        updateCta();

        // Runtime Forge entrance: compositor-only alpha/translation so first
        // launch remains smooth even while runtime metadata is being resolved.
        float d = getResources().getDisplayMetrics().density;
        mSelectContainer.setAlpha(0f);
        mSelectContainer.setTranslationX(22 * d);
        mSelectContainer.animate().alpha(1f).translationX(0f)
                .setDuration(320).setInterpolator(new DecelerateInterpolator(1.7f))
                .withLayer().start();
        View orbit = findViewById(R.id.rs_brand_orbit);
        if (orbit != null) {
            orbit.setAlpha(0f); orbit.setScaleX(0.72f); orbit.setScaleY(0.72f);
            orbit.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(90)
                    .setDuration(360).setInterpolator(new android.view.animation.OvershootInterpolator(0.9f))
                    .withLayer().start();
        }
    }

    // ─────────────────────────── selection ───────────────────────────

    private void buildCards() {
        LayoutInflater inflater = LayoutInflater.from(this);
        List<NewJREUtil.ExternalRuntime> downloadable = MultiRTUtils.getRuntimesToDownload();
        mCardContainer.removeAllViews();
        mCards.clear();
        for (int i = 0; i < MAJORS.length; i++) {
            int major = MAJORS[i];
            CardState st = new CardState();
            st.major = major;
            st.root = inflater.inflate(R.layout.item_runtime_choice, mCardContainer, false);
            st.chip = st.root.findViewById(R.id.runtime_chip);

            ((TextView) st.root.findViewById(R.id.runtime_major_text)).setText(String.valueOf(major));
            ((TextView) st.root.findViewById(R.id.runtime_name_text)).setText("Java " + major);
            ((TextView) st.root.findViewById(R.id.runtime_role_text)).setText(ROLE_RES[i]);

            st.installed = MultiRTUtils.getExactJreName(major) != null;
            for (NewJREUtil.ExternalRuntime rt : downloadable) {
                if (rt.majorVersion == major) { st.runtime = rt; break; }
            }
            st.selectable = st.runtime != null && !st.installed;

            if (st.installed) {
                showChip(st, getString(R.string.rw_installed), true);
                st.root.setAlpha(0.62f);
                View check = st.root.findViewById(R.id.runtime_check);
                check.setVisibility(View.VISIBLE);
            } else if (st.selectable) {
                boolean recommended = major == 17 || major == 21;
                st.selected = recommended;
                if (recommended) showChip(st, getString(R.string.rw_recommended), false);
                else showChip(st, getString(R.string.rw_size_estimate, SIZE_ESTIMATE_MB[i]), false);
                applySelectionVisual(st);
                st.root.setOnClickListener(v -> {
                    st.selected = !st.selected;
                    applySelectionVisual(st);
                    updateCta();
                });
            } else {
                st.root.setVisibility(View.GONE);
            }
            mCards.add(st);
            mCardContainer.addView(st.root);
            UiMotion.pressFeedback(st.root);
            if (st.root.getVisibility() == View.VISIBLE) {
                float dd = getResources().getDisplayMetrics().density;
                st.root.setAlpha(0f);
                st.root.setTranslationX(36f * dd);
                st.root.animate().alpha(1f).translationX(0f)
                        .setStartDelay(60L + i * 45L).setDuration(240)
                        .setInterpolator(new DecelerateInterpolator(1.8f))
                        .withLayer().start();
            }
        }
    }

    private void showChip(CardState st, String text, boolean installed) {
        st.chip.setVisibility(View.VISIBLE);
        st.chip.setText(text.toUpperCase(Locale.ROOT));
        st.chip.setBackgroundResource(installed
                ? R.drawable.bg_runtime_chip_installed : R.drawable.bg_runtime_chip_recommended);
        st.chip.setTextColor(installed ? 0xFF9FD6AC : 0xFFC9CBD3);
    }

    private void applySelectionVisual(CardState st) {
        st.root.setBackgroundResource(st.selected
                ? R.drawable.bg_runtime_card_selected : R.drawable.bg_runtime_card);
        View check = st.root.findViewById(R.id.runtime_check);
        if (check != null && !st.installed) {
            check.animate().cancel();
            if (st.selected) {
                check.setVisibility(View.VISIBLE);
                check.setAlpha(0f); check.setScaleX(0.55f); check.setScaleY(0.55f);
                check.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(190)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f))
                        .withLayer().start();
            } else {
                check.setVisibility(View.GONE);
            }
        }
        st.root.animate().scaleX(st.selected ? 1f : 0.99f).scaleY(st.selected ? 1f : 0.99f)
                .setDuration(150).setInterpolator(new DecelerateInterpolator(1.8f))
                .withLayer().start();
    }

    private void updateCta() {
        int sel = 0;
        for (CardState c : mCards) if (c.selectable && c.selected) sel++;
        if (sel == 0) mInstallButton.setText(R.string.rs_continue_cta);
        else mInstallButton.setText(getString(R.string.rs_install_cta, sel));
    }

    // ─────────────────────────── install deck ───────────────────────────

    private void beginInstall() {
        List<CardState> queue = new ArrayList<>();
        for (CardState c : mCards) if (c.selectable && c.selected && !c.installed) queue.add(c);
        if (queue.isEmpty()) { finish(); return; }

        mInstalling = true;
        buildInstallRows(queue);

        // State transition: selection slides out, install deck rises in
        mSelectContainer.animate().alpha(0f).translationX(-56f *
                getResources().getDisplayMetrics().density).setDuration(220)
                .withEndAction(() -> {
            mSelectContainer.setVisibility(View.GONE);
            mInstallContainer.setVisibility(View.VISIBLE);
            mInstallContainer.setAlpha(0f);
            mInstallContainer.setTranslationX(64f *
                    getResources().getDisplayMetrics().density);
            mInstallContainer.animate().alpha(1f).translationX(0f)
                    .setDuration(360).setInterpolator(new DecelerateInterpolator(1.5f)).start();
            staggerRows();
        }).start();

        hookProgress();
        mOverallStep.setText(getString(R.string.rs_step_installing, queue.get(0).major));
        mStatusLine.setText(R.string.rs_stats_preparing);
        mRing.setProgress(0f);
        mOverallPercent.setText("0%");

        PojavApplication.sExecutorService.execute(() -> runQueue(queue));
    }

    private void buildInstallRows(List<CardState> queue) {
        LayoutInflater inflater = LayoutInflater.from(this);
        mInstallRows.removeAllViews();
        mRows.clear();
        for (CardState c : queue) {
            InstallRow row = new InstallRow();
            row.major = c.major;
            row.runtime = c.runtime;
            row.progressRecord=ProgressLayout.UNPACK_RUNTIME+":"+row.major;row.progress=0;
            row.state = 0;
            row.root = inflater.inflate(R.layout.item_runtime_install_row, mInstallRows, false);
            row.chip = row.root.findViewById(R.id.install_state_chip);
            row.stats = row.root.findViewById(R.id.install_stats_text);
            row.bar = row.root.findViewById(R.id.install_progress_bar);
            row.spinner = row.root.findViewById(R.id.install_spinner);
            row.check = row.root.findViewById(R.id.install_check);
            row.bigPercent = row.root.findViewById(R.id.install_big_percent);
            if (row.bigPercent != null) row.bigPercent.setText("0%");
            ((TextView) row.root.findViewById(R.id.install_major_text)).setText(String.valueOf(c.major));
            ((TextView) row.root.findViewById(R.id.install_name_text)).setText("Java " + c.major);
            setRowChip(row, getString(R.string.rw_queued), 0xFF9C9CA8, R.drawable.bg_runtime_chip_neutral);
            row.stats.setText(R.string.rs_stats_queued);
            mRows.add(row);
            mInstallRows.addView(row.root);
        }
    }

    /** Rows cascade in one after another. */
    private void staggerRows() {
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < mRows.size(); i++) {
            View r = mRows.get(i).root;
            r.setAlpha(0f);
            r.setTranslationX(42 * d);
            r.animate().alpha(1f).translationX(0f).setStartDelay(120L + i * 110L)
                    .setDuration(320).setInterpolator(new DecelerateInterpolator(1.5f)).start();
        }
    }

    private void setRowChip(InstallRow row, String text, int color, int chipBg) {
        row.chip.setVisibility(View.VISIBLE);
        row.chip.setText(text.toUpperCase(Locale.ROOT));
        row.chip.setTextColor(color);
        row.chip.setBackgroundResource(chipBg);
    }

    private void setBarProgress(ProgressBar bar, int pct) {
        if (android.os.Build.VERSION.SDK_INT >= 24) bar.setProgress(pct, true);
        else bar.setProgress(pct);
    }

    private void hookProgress(){for(InstallRow row:mRows){ProgressListener hook=new ProgressListener(){@Override public void onProgressStarted(){}@Override public void onProgressEnded(){}@Override public void onProgressUpdated(int progress,int resid,Object...va){runOnUiThread(()->{if(isFinishingOrDestroyedSoft())return;row.progress=Math.max(0,Math.min(100,progress));setBarProgress(row.bar,row.progress);if(row.bigPercent!=null)row.bigPercent.setText(row.progress+"%");double cur=va!=null&&va.length>1&&va[1]instanceof Number?((Number)va[1]).doubleValue():-1,tot=va!=null&&va.length>2&&va[2]instanceof Number?((Number)va[2]).doubleValue():-1,speed=va!=null&&va.length>3&&va[3]instanceof Number?((Number)va[3]).doubleValue():-1,remain=va!=null&&va.length>4&&va[4]instanceof Number?((Number)va[4]).doubleValue():-1;if(tot>0){String text=String.format(Locale.US,"%.1f / %.0f MB",cur,tot);if(speed>0)text+=String.format(Locale.US," • %.1f MB/s",speed);if(remain>0)text+=String.format(Locale.US," • ~%ds",(int)remain);row.stats.setText(text);}refreshAggregate();});}};mProgressHooks.put(row.progressRecord,hook);ProgressKeeper.addListener(row.progressRecord,hook);}}
    private void refreshAggregate(){if(mRows.isEmpty())return;float sum=0;for(InstallRow r:mRows)sum+=r.state==3?100:r.progress;float frac=sum/(100f*mRows.size());mRing.setProgress(frac);mOverallPercent.setText((int)(frac*100)+"%");}

    private void runQueue(List<CardState> queue){java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(Math.min(2,Math.max(1,mRows.size())));java.util.concurrent.CountDownLatch downloads=new java.util.concurrent.CountDownLatch(mRows.size());for(InstallRow row:mRows){pool.execute(()->{runOnUiThreadSafe(()->{row.state=1;row.root.setBackgroundResource(R.drawable.bg_rs_stage_card_active);row.spinner.setVisibility(View.VISIBLE);setRowChip(row,"DOWNLOADING",0xFFD8C79A,R.drawable.bg_runtime_chip_recommended);row.stats.setText(R.string.rs_stats_preparing);mOverallStep.setText("Downloading selected runtimes in parallel");});try{row.archive=NewJREUtil.downloadRuntimeArchive(getApplicationContext(),row.major,row.progressRecord);row.progress=100;row.state=2;runOnUiThreadSafe(()->{row.spinner.setVisibility(View.GONE);setRowChip(row,"VERIFIED",0xFF9FD6AC,R.drawable.bg_runtime_chip_installed);row.stats.setText("Archive ready • waiting for safe install");refreshAggregate();});}catch(RuntimeException e){row.state=4;runOnUiThreadSafe(()->{row.spinner.setVisibility(View.GONE);row.root.setBackgroundResource(R.drawable.bg_rs_stage_card_failed);setRowChip(row,getString(R.string.rw_failed),0xFFE5A0A6,R.drawable.bg_runtime_chip_installed);row.stats.setText(R.string.rs_stats_failed);row.root.setOnClickListener(v->retryRow(row));});}finally{ProgressLayout.clearProgress(row.progressRecord);downloads.countDown();}});}pool.shutdown();try{downloads.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}for(InstallRow row:mRows){if(row.state!=2||row.archive==null)continue;runOnUiThreadSafe(()->{row.state=2;row.spinner.setVisibility(View.VISIBLE);setRowChip(row,"INSTALLING",0xFFD8C79A,R.drawable.bg_runtime_chip_recommended);mOverallStep.setText("Installing Java "+row.major);});try{NewJREUtil.installRuntimeArchive(row.archive,row.major);row.state=3;row.progress=100;mDoneCount++;runOnUiThreadSafe(()->{row.spinner.setVisibility(View.GONE);setBarProgress(row.bar,100);if(row.bigPercent!=null)row.bigPercent.setText("100%");row.root.setBackgroundResource(R.drawable.bg_rs_stage_card_success);setRowChip(row,getString(R.string.rw_ready),0xFF9FD6AC,R.drawable.bg_runtime_chip_installed);row.stats.setText(R.string.rs_stats_ready);row.check.setVisibility(View.VISIBLE);UiMotion.popIn(row.check);refreshAggregate();});}catch(RuntimeException e){row.state=4;runOnUiThreadSafe(()->{row.spinner.setVisibility(View.GONE);row.root.setBackgroundResource(R.drawable.bg_rs_stage_card_failed);setRowChip(row,getString(R.string.rw_failed),0xFFE5A0A6,R.drawable.bg_runtime_chip_installed);row.stats.setText(R.string.rs_stats_failed);row.root.setOnClickListener(v->retryRow(row));});}}runOnUiThreadSafe(this::finishDeck);}

    private void retryRow(InstallRow row){if(row.state!=4)return;row.state=1;row.progress=0;row.root.setOnClickListener(null);PojavApplication.sExecutorService.execute(()->{runOnUiThreadSafe(()->{row.root.setBackgroundResource(R.drawable.bg_rs_stage_card_active);row.spinner.setVisibility(View.VISIBLE);setRowChip(row,"DOWNLOADING",0xFFD8C79A,R.drawable.bg_runtime_chip_recommended);});try{row.archive=NewJREUtil.downloadRuntimeArchive(getApplicationContext(),row.major,row.progressRecord);ProgressLayout.clearProgress(row.progressRecord);runOnUiThreadSafe(()->{setRowChip(row,"INSTALLING",0xFFD8C79A,R.drawable.bg_runtime_chip_recommended);});NewJREUtil.installRuntimeArchive(row.archive,row.major);row.state=3;row.progress=100;runOnUiThreadSafe(()->{row.spinner.setVisibility(View.GONE);setBarProgress(row.bar,100);if(row.bigPercent!=null)row.bigPercent.setText("100%");row.root.setBackgroundResource(R.drawable.bg_rs_stage_card_success);setRowChip(row,getString(R.string.rw_ready),0xFF9FD6AC,R.drawable.bg_runtime_chip_installed);row.stats.setText(R.string.rs_stats_ready);row.check.setVisibility(View.VISIBLE);refreshAggregate();finishDeck();});}catch(RuntimeException e){row.state=4;ProgressLayout.clearProgress(row.progressRecord);runOnUiThreadSafe(()->{row.spinner.setVisibility(View.GONE);row.root.setBackgroundResource(R.drawable.bg_rs_stage_card_failed);setRowChip(row,getString(R.string.rw_failed),0xFFE5A0A6,R.drawable.bg_runtime_chip_installed);row.stats.setText(R.string.rs_stats_failed);row.root.setOnClickListener(v->retryRow(row));});}});}

    private void finishDeck() {
        if (isFinishingOrDestroyedSoft()) return;
        boolean anyFailed = false;
        boolean allDone = true;
        for (InstallRow r : mRows) {
            if (r.state == 4) anyFailed = true;
            if (r.state != 3 && r.state != 4) allDone = false;
        }
        if (!allDone) return;

        mRing.setProgress(anyFailed ? mRing.getProgress() : 1f);
        if (!anyFailed) {
            mOverallPercent.setText("100%");
            mOverallStep.setText(R.string.rs_step_done);
            mStatusLine.setText(R.string.rs_stats_all_ready);
        } else {
            mOverallStep.setText(R.string.rs_step_partial);
            mStatusLine.setText(R.string.rs_stats_partial);
        }
        mDoneButton.setVisibility(View.VISIBLE);
        mDoneButton.setAlpha(0f);
        mDoneButton.setTranslationY(14f * getResources().getDisplayMetrics().density);
        mDoneButton.animate().alpha(1f).translationY(0f).setDuration(300)
                .setInterpolator(new DecelerateInterpolator(1.5f)).start();
    }

    // ─────────────────────────── lifecycle ───────────────────────────

    private boolean isFinishingOrDestroyedSoft() {
        return isFinishing() || isDestroyed();
    }

    private void runOnUiThreadSafe(Runnable r) {
        runOnUiThread(() -> { if (!isFinishingOrDestroyedSoft()) r.run(); });
    }

    @Override
    public void onBackPressed() {
        if (mInstalling) {
            Toast.makeText(this, R.string.rs_back_locked, Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        for(java.util.Map.Entry<String,ProgressListener> e:mProgressHooks.entrySet())try{ProgressKeeper.removeListener(e.getKey(),e.getValue());}catch(Throwable ignored){}mProgressHooks.clear();
        super.onDestroy();
    }
}
