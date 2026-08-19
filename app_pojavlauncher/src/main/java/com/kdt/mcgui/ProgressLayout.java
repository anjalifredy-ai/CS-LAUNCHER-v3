package com.kdt.mcgui;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.ProgressBar;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache;
import net.kdt.pojavlaunch.utils.DownloadControl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.collection.ArrayMap;
import androidx.constraintlayout.widget.ConstraintLayout;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.ProgressListener;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;
import net.kdt.pojavlaunch.services.ProgressService;

import java.util.ArrayList;


/** Class staring at specific values and automatically show something if the progress is present
 * Since progress is posted in a specific way, The packing/unpacking is handheld by the class
 *
 * This class relies on ExtraCore for its behavior.
 *
 * v4 — Premium redesign: luminous 8dp pill beam (DownloadAssemblyBar), big tabular %,
 * accent rail, segmented controls, collapsed pill, and a lightweight animation system:
 *  - Start: spring rise from bottom (PathInterpolator)
 *  - Progress: smooth beam + percentage PathInterpolator 0.22/1/0.36/1
 *  - Completion: success mint tint + scale bounce + check morph
 *  - Error: shake + coral tint
 *  - Paused: amber tint + badge
 * All animations are compositor-only (alpha/translation/scale) — no layout thrash.
 */
public class ProgressLayout extends ConstraintLayout implements View.OnClickListener, TaskCountListener{
    public static final String UNPACK_RUNTIME = "unpack_runtime";
    public static final String DOWNLOAD_MINECRAFT = "download_minecraft";
    public static final String DOWNLOAD_VERSION_LIST = "download_verlist";
    public static final String AUTHENTICATE_MICROSOFT = "authenticate_microsoft";
    public static final String INSTALL_MODPACK = "install_modpack";
    public static final String EXTRACT_COMPONENTS = "extract_components";
    public static final String EXTRACT_SINGLE_FILES = "extract_single_files";

    private static final int COLOR_TEXT_PRIMARY = 0xFFF0F0F3;
    private static final int COLOR_TEXT_SECONDARY = 0xFFFFFFFF;
    private static final int COLOR_SUCCESS = 0xFF9FD6AC;
    private static final int COLOR_AMBER = 0xFFD8C79A;
    private static final int COLOR_ERROR = 0xFFFF6B6B;

    private static final PathInterpolator SMOOTH = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
    private static final PathInterpolator SPRING = new PathInterpolator(0.22f, 1f, 0.36f, 1f);

    public ProgressLayout(@NonNull Context context) {
        super(context);
        init();
    }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private final ArrayList<LayoutProgressListener> mMap = new ArrayList<>();
    private LinearLayout mLinearLayout;
    private TextView mTaskNumberDisplayer;
    private ImageView mFlipArrow;
    private PulseOrbitView mPulseOrbit;
    private TextView mStatusText;
    private TextView mDetailText;
    private TextView mSpeedText;
    private String mLastProgressingKey;
    private int mLastProgress = 0;
    private String mLastDetailText = "";

    private ProgressBar mProgressBar;
    private TextView mPercentageText;
    private TextView mEtaText;
    private View mDownloadCard;
    private ImageView mProgressIcon;
    private boolean mIsFinishing = false;
    private boolean mExpectingQuietEnd = false;

    private View mControlsRow;
    private View mBtnPause;
    private ImageView mBtnPauseIcon;
    private TextView mBtnPauseText;
    private View mBtnStop;
    private View mBtnHide;
    private View mPausedBadge;
    private View mConsolePill;
    private TextView mPillPercent;
    private boolean mIsCollapsed = false;

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private final Runnable mFadeOutRunnable = new Runnable() {
        @Override
        public void run() {
            ProgressLayout.this.animate()
                .alpha(0f)
                .translationY(ProgressLayout.this.getHeight() + dp(24f))
                .setDuration(420)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    hideChromeImmediately();
                })
                .start();
        }
    };

    private void hideChromeImmediately() {
        removeCallbacks(mFadeOutRunnable);
        ProgressLayout.this.animate().cancel();
        if (mDownloadCard != null) {
            mDownloadCard.animate().cancel();
        }
        if (mConsolePill != null) {
            mConsolePill.animate().cancel();
            mConsolePill.setVisibility(GONE);
            mConsolePill.setAlpha(1f);
            mConsolePill.setTranslationY(0f);
        }
        mIsCollapsed = false;
        if (mDownloadCard != null) {
            mDownloadCard.setVisibility(VISIBLE);
            mDownloadCard.setAlpha(1f);
            mDownloadCard.setTranslationY(0f);
            mDownloadCard.setScaleX(1f);
            mDownloadCard.setScaleY(1f);
        }
        ProgressLayout.this.setVisibility(GONE);
        ProgressLayout.this.setAlpha(1f);
        ProgressLayout.this.setTranslationY(0f);
        mIsFinishing = false;
    }


    public void observe(String progressKey){
        mMap.add(new LayoutProgressListener(progressKey));
    }

    public void cleanUpObservers() {
        for(LayoutProgressListener progressListener : mMap) {
            ProgressKeeper.removeListener(progressListener.progressKey, progressListener);
        }
    }

    public boolean hasProcesses(){
        return ProgressKeeper.getTaskCount() > 0;
    }

    private static boolean isCancellableKey(String key) {
        return INSTALL_MODPACK.equals(key)
                || DOWNLOAD_MINECRAFT.equals(key)
                || UNPACK_RUNTIME.equals(key);
    }


    private void init(){
        inflate(getContext(), R.layout.view_progress, this);
        mLinearLayout = findViewById(R.id.progress_linear_layout);
        mTaskNumberDisplayer = findViewById(R.id.progress_textview);
        mFlipArrow = findViewById(R.id.progress_flip_arrow);
        mPulseOrbit = findViewById(R.id.pulse_orbit);
        mStatusText = findViewById(R.id.progress_status_text);
        mDetailText = findViewById(R.id.progress_detail_text);
        mSpeedText = findViewById(R.id.progress_speed_text);

        mProgressBar = findViewById(R.id.progress_horizontal_bar);
        mPercentageText = findViewById(R.id.progress_percentage_text);
        mEtaText = findViewById(R.id.progress_eta_text);
        mDownloadCard = findViewById(R.id.download_card);
        mProgressIcon = findViewById(R.id.progress_icon);

        mControlsRow = findViewById(R.id.progress_controls_row);
        mBtnPause = findViewById(R.id.progress_btn_pause);
        mBtnPauseIcon = findViewById(R.id.progress_btn_pause_icon);
        mBtnPauseText = findViewById(R.id.progress_btn_pause_text);
        mBtnStop = findViewById(R.id.progress_btn_stop);
        mBtnHide = findViewById(R.id.progress_btn_hide);
        mPausedBadge = findViewById(R.id.progress_paused_badge);
        mConsolePill = findViewById(R.id.console_pill);
        mPillPercent = findViewById(R.id.console_pill_percent);

        if (mDownloadCard != null) {
            mDownloadCard.setOnClickListener(v -> {
                if (mIsFinishing || ProgressKeeper.getTaskCount() == 0) {
                    mExpectingQuietEnd = false;
                    hideChromeImmediately();
                } else {
                    ProgressLayout.this.onClick(ProgressLayout.this);
                }
            });
            // subtle press feedback
            mDownloadCard.setOnTouchListener((v, e) -> {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(90).start();
                } else if (e.getAction() == android.view.MotionEvent.ACTION_UP || e.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(SMOOTH).start();
                }
                return false;
            });
        }

        if (mBtnPause != null) {
            mBtnPause.setOnClickListener(v -> {
                String key = mLastProgressingKey;
                if (key == null) return;
                boolean nowPaused = !DownloadControl.isPaused(key);
                DownloadControl.requestPause(key, nowPaused);
                updatePauseUi(nowPaused);
                // micro press
                v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(SMOOTH).start()).start();
            });
        }

        if (mBtnStop != null) {
            mBtnStop.setOnClickListener(v -> {
                String key = mLastProgressingKey;
                if (key == null) return;
                DownloadControl.requestPause(key, false);
                DownloadControl.requestCancel(key);
                updatePauseUi(false);
                mExpectingQuietEnd = true;
                Toast.makeText(getContext(), R.string.download_console_stopped, Toast.LENGTH_SHORT).show();
                // stop has a quick shrink-then-leave
                if (mDownloadCard != null) {
                    mDownloadCard.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.7f).setDuration(120).withEndAction(() -> hideChromeImmediately()).start();
                } else hideChromeImmediately();
            });
        }

        if (mBtnHide != null) {
            mBtnHide.setOnClickListener(v -> {
                v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()).start();
                collapseToPill();
            });
        }

        if (mConsolePill != null) {
            mConsolePill.setOnClickListener(v -> expandFromPill());
            mConsolePill.setOnTouchListener((v, e) -> {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
                else if (e.getAction() == android.view.MotionEvent.ACTION_UP || e.getAction() == android.view.MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(SMOOTH).start();
                return false;
            });
        }

        setBackgroundColor(Color.TRANSPARENT);
        setOnClickListener(this);
    }

    private void collapseToPill() {
        if (mIsCollapsed || mIsFinishing || getVisibility() != VISIBLE) return;
        mIsCollapsed = true;

        if (mPillPercent != null) mPillPercent.setText(mLastProgress + "%");
        if (mConsolePill != null) {
            mConsolePill.setVisibility(VISIBLE);
            mConsolePill.setAlpha(0f);
            mConsolePill.setTranslationY(dp(12f));
            mConsolePill.setScaleX(0.92f);
            mConsolePill.setScaleY(0.92f);
            mConsolePill.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(320)
                    .setInterpolator(SMOOTH)
                    .start();
        }
        if (mDownloadCard != null) {
            mDownloadCard.animate().cancel();
            mDownloadCard.animate()
                    .alpha(0f)
                    .translationY(dp(26f))
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(240)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        mDownloadCard.setVisibility(GONE);
                        mDownloadCard.setTranslationY(0f);
                        mDownloadCard.setScaleX(1f);
                        mDownloadCard.setScaleY(1f);
                    })
                    .start();
        }
    }

    private void expandFromPill() {
        if (!mIsCollapsed) return;
        mIsCollapsed = false;

        if (mDownloadCard != null) {
            mDownloadCard.setVisibility(VISIBLE);
            mDownloadCard.setAlpha(0f);
            mDownloadCard.setTranslationY(dp(18f));
            mDownloadCard.setScaleX(0.96f);
            mDownloadCard.setScaleY(0.96f);
            mDownloadCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(360)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(0.7f))
                    .start();
        }
        if (mConsolePill != null) {
            mConsolePill.animate().cancel();
            mConsolePill.animate()
                    .alpha(0f)
                    .translationY(dp(8f))
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(190)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        mConsolePill.setVisibility(GONE);
                        mConsolePill.setAlpha(1f);
                        mConsolePill.setTranslationY(0f);
                        mConsolePill.setScaleX(1f);
                        mConsolePill.setScaleY(1f);
                    })
                    .start();
        }
    }

    private void updatePauseUi(boolean paused) {
        if (mPausedBadge != null) {
            if (paused) {
                mPausedBadge.setVisibility(VISIBLE);
                mPausedBadge.setAlpha(0f);
                mPausedBadge.setScaleX(0.85f);
                mPausedBadge.setScaleY(0.85f);
                mPausedBadge.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).setInterpolator(SMOOTH).start();
            } else {
                mPausedBadge.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(140).withEndAction(() -> mPausedBadge.setVisibility(GONE)).start();
            }
        }
        if (mBtnPauseIcon != null) mBtnPauseIcon.setImageResource(paused ? R.drawable.ic_play_arrow : R.drawable.ic_pause);
        if (mBtnPauseText != null) mBtnPauseText.setText(paused ? R.string.download_console_resume : R.string.download_console_pause);
        if (mProgressBar instanceof DownloadAssemblyBar) {
            ((DownloadAssemblyBar) mProgressBar).setBarState(paused ? 3 : 0);
        } else if (mProgressBar != null && !mIsFinishing) {
            mProgressBar.setProgressTintList(paused ? ColorStateList.valueOf(COLOR_AMBER) : null);
        }
        if (paused) {
            if (mSpeedText != null) mSpeedText.animate().alpha(0f).setDuration(140).withEndAction(() -> mSpeedText.setVisibility(GONE)).start();
            if (mEtaText != null) mEtaText.animate().alpha(0f).setDuration(140).withEndAction(() -> mEtaText.setVisibility(GONE)).start();
        }
    }

    /** Shake for error state — lightweight translate oscillation */
    private void triggerErrorShake() {
        if (mDownloadCard == null) return;
        mDownloadCard.animate().cancel();
        android.animation.ValueAnimator shake = android.animation.ValueAnimator.ofFloat(0, 1);
        shake.setDuration(380);
        shake.setInterpolator(new android.view.animation.DecelerateInterpolator());
        shake.addUpdateListener(v -> {
            float f = (float) v.getAnimatedValue();
            float off = (float) (Math.sin(f * Math.PI * 6) * (1 - f) * dp(5));
            mDownloadCard.setTranslationX(off);
        });
        shake.start();
        if (mProgressBar instanceof DownloadAssemblyBar) {
            ((DownloadAssemblyBar) mProgressBar).setBarState(2);
        }
        if (mStatusText != null) {
            mStatusText.setTextColor(COLOR_ERROR);
            mStatusText.animate().scaleX(1.02f).scaleY(1.02f).setDuration(120).withEndAction(() -> mStatusText.animate().scaleX(1f).scaleY(1f).setDuration(140).start()).start();
        }
    }

    public static void setProgress(String progressKey, int progress){
        ProgressKeeper.submitProgress(progressKey, progress, -1, (Object)null);
    }

    public static void setProgress(String progressKey, int progress, @StringRes int resource, Object... message){
        ProgressKeeper.submitProgress(progressKey, progress, resource, message);
    }

    public static void setProgress(String progressKey, int progress, String message){
        setProgress(progressKey,progress, -1, message);
    }

    public static void clearProgress(String progressKey){
        setProgress(progressKey, -1, -1);
    }

    @Override
    public void onClick(View v) {
        if (mIsFinishing || ProgressKeeper.getTaskCount() == 0) {
            mExpectingQuietEnd = false;
            hideChromeImmediately();
        } else {
            if (mLinearLayout != null) {
                boolean show = mLinearLayout.getVisibility() == GONE;
                mLinearLayout.setVisibility(show ? VISIBLE : GONE);
                if (show) {
                    mLinearLayout.setAlpha(0f);
                    mLinearLayout.setTranslationY(-dp(6));
                    mLinearLayout.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(SMOOTH).start();
                }
                if (mFlipArrow != null) mFlipArrow.animate().rotation(show ? 180 : 0).setDuration(200).start();
            }
        }
    }

    @Override
    public void onUpdateTaskCount(int tc) {
        post(()->{
            int effectiveTc = tc;
            if (effectiveTc > 0 && net.kdt.pojavlaunch.launch.LaunchTracker.suppressesDownloadConsole()) {
                effectiveTc = 0;
            }
            if(effectiveTc > 0) {
                removeCallbacks(mFadeOutRunnable);
                boolean becameVisible = getVisibility() != VISIBLE;
                mIsFinishing = false;
                mExpectingQuietEnd = false;
                setAlpha(1f);
                setTranslationY(0f);
                mIsCollapsed = false;
                if (mConsolePill != null) {
                    mConsolePill.animate().cancel();
                    mConsolePill.setVisibility(GONE);
                    mConsolePill.setAlpha(1f);
                    mConsolePill.setTranslationY(0f);
                    mConsolePill.setScaleX(1f);
                    mConsolePill.setScaleY(1f);
                }
                if (mDownloadCard != null) {
                    mDownloadCard.animate().cancel();
                    mDownloadCard.setVisibility(VISIBLE);
                    mDownloadCard.setAlpha(1f);
                    mDownloadCard.setTranslationY(0f);
                    mDownloadCard.setScaleX(1f);
                    mDownloadCard.setScaleY(1f);
                    if (mProgressBar instanceof DownloadAssemblyBar) ((DownloadAssemblyBar) mProgressBar).setBarState(0);
                }
                if (mProgressBar != null && !(mProgressBar instanceof DownloadAssemblyBar)) {
                    mProgressBar.setProgressTintList(null);
                }
                if (mStatusText != null) {
                    mStatusText.setTextColor(COLOR_TEXT_PRIMARY);
                    mStatusText.setScaleX(1f);
                    mStatusText.setScaleY(1f);
                }
                if (mPercentageText != null) {
                    mPercentageText.setTextColor(COLOR_TEXT_SECONDARY);
                    mPercentageText.setScaleX(1f);
                    mPercentageText.setScaleY(1f);
                }
                if (mProgressIcon != null) {
                    mProgressIcon.setAlpha(1f);
                    mProgressIcon.setVisibility(VISIBLE);
                    mProgressIcon.setScaleX(1f);
                    mProgressIcon.setScaleY(1f);
                }
                updatePauseUi(false);
                if (mControlsRow != null) {
                    mControlsRow.setVisibility(isCancellableKey(mLastProgressingKey) ? VISIBLE : GONE);
                }
                if (mTaskNumberDisplayer != null) mTaskNumberDisplayer.setText(getContext().getString(R.string.progresslayout_tasks_in_progress, tc));
                setVisibility(VISIBLE);
                if (becameVisible) {
                    // Premium spring rise — deck lands softly
                    animate().cancel();
                    setAlpha(0f);
                    setTranslationY(dp(110f));
                    setScaleX(0.98f);
                    setScaleY(0.98f);
                    animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                            .setDuration(520)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(0.72f))
                            .withLayer()
                            .start();
                    if (mDownloadCard != null) {
                        mDownloadCard.setAlpha(0f);
                        mDownloadCard.setTranslationY(dp(18f));
                        mDownloadCard.animate().alpha(1f).translationY(0f).setDuration(380).setStartDelay(80).setInterpolator(SMOOTH).start();
                    }
                }
            } else {
                if (getVisibility() == VISIBLE && !mIsFinishing) {
                    if (mExpectingQuietEnd) {
                        mExpectingQuietEnd = false;
                        hideChromeImmediately();
                        return;
                    }
                    mIsFinishing = true;
                    if (mPulseOrbit != null) {
                        mPulseOrbit.showCompleted();
                    }
                    // Success choreography
                    if (mProgressBar instanceof DownloadAssemblyBar) {
                        ((DownloadAssemblyBar) mProgressBar).setBarState(1);
                    }
                    if (mProgressIcon != null) {
                        mProgressIcon.animate().scaleX(0.88f).scaleY(0.88f).setDuration(120).withEndAction(() -> {
                            mProgressIcon.setImageResource(R.drawable.ic_check);
                            mProgressIcon.setImageTintList(ColorStateList.valueOf(COLOR_SUCCESS));
                            mProgressIcon.setScaleX(0.6f); mProgressIcon.setScaleY(0.6f);
                            mProgressIcon.animate().scaleX(1f).scaleY(1f).setDuration(320).setInterpolator(new android.view.animation.OvershootInterpolator(1.8f)).start();
                            mProgressIcon.animate().alpha(1f).setDuration(280).start();
                        }).start();
                    }
                    if (mDownloadCard != null) {
                        mDownloadCard.animate().scaleX(1.015f).scaleY(1.015f).setDuration(180).setInterpolator(SMOOTH)
                                .withEndAction(() -> mDownloadCard.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(SMOOTH).start()).start();
                    }
                    if (mStatusText != null) {
                        mStatusText.setText("✓ Download Complete");
                        mStatusText.setTextColor(COLOR_SUCCESS);
                        mStatusText.setAlpha(0f); mStatusText.setTranslationY(-dp(4));
                        mStatusText.animate().alpha(1f).translationY(0f).setDuration(260).setInterpolator(SMOOTH).start();
                        mStatusText.animate().scaleX(1.03f).scaleY(1.03f).setDuration(140).withEndAction(() -> mStatusText.animate().scaleX(1f).scaleY(1f).setDuration(180).start()).start();
                    }
                    if (mPercentageText != null) {
                        mPercentageText.setText("100%");
                        mPercentageText.setTextColor(COLOR_SUCCESS);
                        mPercentageText.setScaleX(0.9f); mPercentageText.setScaleY(0.9f);
                        mPercentageText.animate().scaleX(1f).scaleY(1f).setDuration(340).setInterpolator(new android.view.animation.OvershootInterpolator(1.6f)).start();
                    }
                    if (mProgressBar != null) {
                        mProgressBar.setProgress(100);
                        if (!(mProgressBar instanceof DownloadAssemblyBar)) mProgressBar.setProgressTintList(ColorStateList.valueOf(COLOR_SUCCESS));
                        mProgressBar.animate().cancel();
                        mProgressBar.setAlpha(0.4f);
                        mProgressBar.animate().alpha(1f).setDuration(460).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                        // subtle bar pop
                        mProgressBar.setScaleY(1.25f);
                        mProgressBar.animate().scaleY(1f).setDuration(300).setInterpolator(SMOOTH).start();
                    }
                    if (mPausedBadge != null) {
                        mPausedBadge.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(140).withEndAction(() -> mPausedBadge.setVisibility(GONE)).start();
                    }
                    if (mControlsRow != null) {
                        mControlsRow.animate().alpha(0f).translationY(dp(6)).setDuration(180).withEndAction(() -> mControlsRow.setVisibility(GONE)).start();
                    }
                    if (mDetailText != null) {
                        mDetailText.animate().alpha(0f).setDuration(140).withEndAction(() -> mDetailText.setVisibility(GONE)).start();
                    }
                    if (mSpeedText != null) {
                        mSpeedText.animate().alpha(0f).setDuration(140).withEndAction(() -> mSpeedText.setVisibility(GONE)).start();
                    }
                    if (mEtaText != null) {
                        mEtaText.animate().alpha(0f).setDuration(140).withEndAction(() -> mEtaText.setVisibility(GONE)).start();
                    }
                    if (mIsCollapsed && mPillPercent != null) {
                        mPillPercent.setText("✓");
                        mPillPercent.setTextColor(COLOR_SUCCESS);
                    }
                    removeCallbacks(mFadeOutRunnable);
                    postDelayed(mFadeOutRunnable, 2600);
                } else if (getVisibility() != VISIBLE) {
                    mExpectingQuietEnd = false;
                    hideChromeImmediately();
                }
            }
        });
    }

    private static String formatRemainingTime(double seconds) {
        if (seconds < 0) return "";
        int totalSecs = (int) seconds;
        int mins = totalSecs / 60;
        int secs = totalSecs % 60;
        if (mins > 0) {
            return mins + "m " + secs + "s remaining";
        } else {
            return secs + "s remaining";
        }
    }

    class LayoutProgressListener implements ProgressListener {
        final String progressKey;
        final TextProgressBar textView;
        final LinearLayout.LayoutParams params;
        private ObjectAnimator mProgressAnimator;
        private ValueAnimator mPercentageAnimator;
        private int mTargetProgress = -1;

        public LayoutProgressListener(String progressKey) {
            this.progressKey = progressKey;
            textView = new TextProgressBar(getContext());
            textView.setTextPadding(getContext().getResources().getDimensionPixelOffset(R.dimen._6sdp));
            params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, getResources().getDimensionPixelOffset(R.dimen._20sdp));
            params.bottomMargin = getResources().getDimensionPixelOffset(R.dimen._6sdp);
            ProgressKeeper.addListener(progressKey, this);
        }
        @Override
        public void onProgressStarted() {
            DownloadControl.reset(progressKey);
            post(()-> {
                Log.i("ProgressLayout", "onProgressStarted");
                mLinearLayout.addView(textView, params);
                // selection pulse on icon
                if (mProgressIcon != null) {
                    mProgressIcon.animate().cancel();
                    mProgressIcon.setScaleX(0.82f); mProgressIcon.setScaleY(0.82f);
                    mProgressIcon.animate().scaleX(1f).scaleY(1f).setDuration(320).setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).start();
                }
                // card quick highlight
                if (mDownloadCard != null) {
                    mDownloadCard.animate().scaleX(1.01f).scaleY(1.01f).setDuration(110).withEndAction(() -> mDownloadCard.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(SMOOTH).start()).start();
                }
            });
        }

        @Override
        public void onProgressUpdated(int progress, int resid, Object... va) {
            post(()-> {
                int current = textView.getProgress();
                if (progress != current && progress >= 0) {
                    ObjectAnimator anim = ObjectAnimator.ofInt(textView, "progress", current, progress);
                    anim.setDuration(280);
                    anim.setInterpolator(SMOOTH);
                    anim.start();
                } else {
                    textView.setProgress(progress);
                }
                if(resid != -1) textView.setText(getContext().getString(resid, va));
                else if(va != null && va.length > 0 && va[0] != null)textView.setText((String)va[0]);
                else textView.setText("");

                if (progress >= 0 && mPulseOrbit != null) {
                    mPulseOrbit.setProgress(progress);
                    mLastProgress = progress;
                }
                boolean keyChanged = !this.progressKey.equals(mLastProgressingKey);
                mLastProgressingKey = this.progressKey;
                if (mIsCollapsed && mPillPercent != null && progress >= 0 && !mIsFinishing) {
                    mPillPercent.setText(progress + "%");
                    mPillPercent.setAlpha(0.6f);
                    mPillPercent.animate().alpha(1f).setDuration(180).start();
                }
                if (mControlsRow != null && !mIsFinishing) {
                    mControlsRow.setVisibility(isCancellableKey(this.progressKey) ? VISIBLE : GONE);
                }
                if (keyChanged && !mIsFinishing) {
                    updatePauseUi(DownloadControl.isPaused(this.progressKey));
                }

                if (mProgressBar != null && progress >= 0) {
                    int currProgress = mProgressBar.getProgress();
                    if (progress != mTargetProgress) {
                        mTargetProgress = progress;

                        if (mProgressAnimator != null && mProgressAnimator.isRunning()) {
                            mProgressAnimator.cancel();
                        }
                        mProgressAnimator = ObjectAnimator.ofInt(mProgressBar, "progress", currProgress, progress);
                        mProgressAnimator.setDuration(420);
                        mProgressAnimator.setInterpolator(SMOOTH);
                        mProgressAnimator.start();

                        if (mPercentageAnimator != null && mPercentageAnimator.isRunning()) {
                            mPercentageAnimator.cancel();
                        }
                        mPercentageAnimator = ValueAnimator.ofInt(currProgress, progress);
                        mPercentageAnimator.setDuration(420);
                        mPercentageAnimator.setInterpolator(SMOOTH);
                        mPercentageAnimator.addUpdateListener(animation -> {
                            if (mPercentageText != null) {
                                mPercentageText.setText(animation.getAnimatedValue() + "%");
                            }
                        });
                        mPercentageAnimator.start();

                        // percentage pop on significant jumps
                        if (Math.abs(progress - currProgress) >= 4 && mPercentageText != null) {
                            mPercentageText.animate().cancel();
                            mPercentageText.setScaleX(1.06f); mPercentageText.setScaleY(1.06f);
                            mPercentageText.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(SMOOTH).start();
                        }
                    }

                }

                double speed = -1;
                String detailStr = "";
                String etaStr = "";
                String statusTitle = "";

                String modName = null;
                String modVersion = null;
                String modIconUrl = null;
                String contentType = null;

                if (va != null && va.length >= 9) {
                    try {
                        modName = (String) va[5];
                        modVersion = (String) va[6];
                        modIconUrl = (String) va[7];
                        contentType = (String) va[8];

                        double currentMB = ((Number) va[1]).doubleValue();
                        double totalMB = ((Number) va[2]).doubleValue();
                        speed = ((Number) va[3]).doubleValue();
                        double remainingSec = ((Number) va[4]).doubleValue();

                        if (totalMB > 0) {
                            detailStr = String.format(java.util.Locale.US, "%.1f MB / %.1f MB", currentMB, totalMB);
                        }
                        if (remainingSec >= 0) {
                            etaStr = formatRemainingTime(remainingSec);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (mProgressIcon != null) {
                    if (modIconUrl != null && !modIconUrl.isEmpty()) {
                        String cacheTag = modName != null ? modName : modIconUrl.substring(modIconUrl.lastIndexOf('/') + 1);
                        ModIconCache.getInstance().getImage(bitmap -> {
                            post(() -> {
                                if (mProgressIcon != null && bitmap != null) {
                                    int pad = (int) (getResources().getDisplayMetrics().density * 6);
                                    mProgressIcon.setPadding(pad, pad, pad, pad);
                                    mProgressIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    mProgressIcon.setImageBitmap(bitmap);
                                    mProgressIcon.setAlpha(1f);
                                    mProgressIcon.setScaleX(0.9f); mProgressIcon.setScaleY(0.9f);
                                    mProgressIcon.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(SMOOTH).start();
                                }
                            });
                        }, cacheTag, modIconUrl);
                    } else {
                        int pad = (int) (getResources().getDisplayMetrics().density * 11);
                        mProgressIcon.setPadding(pad, pad, pad, pad);
                        mProgressIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        mProgressIcon.setImageResource(R.drawable.ic_download);
                    }
                }

                if (this.progressKey != null) {
                    if (resid == R.string.fabric_dl_progress) {
                        statusTitle = "Downloading Fabric" + (va != null && va.length > 0 && va[0] != null ? " " + va[0] : "");
                    } else if (resid == R.string.forge_dl_progress) {
                        String loaderName = "Forge";
                        String verSuffix = "";
                        if (va != null && va.length > 0 && va[0] != null) {
                            String verStr = String.valueOf(va[0]);
                            if (verStr.toLowerCase().contains("neoforge")) {
                                loaderName = "";
                            }
                            verSuffix = " " + verStr;
                        }
                        statusTitle = "Downloading " + loaderName + verSuffix;
                    } else if (resid == R.string.of_dl_progress) {
                        statusTitle = "Downloading OptiFine" + (va != null && va.length > 0 && va[0] != null ? " " + va[0] : "");
                    } else if (resid == R.string.neoforge_dl_searching) {
                        statusTitle = "Searching NeoForge...";
                    } else if (resid == R.string.forge_dl_searching) {
                        statusTitle = "Searching Forge...";
                    } else {
                        switch (this.progressKey) {
                            case DOWNLOAD_MINECRAFT:
                                statusTitle = "Downloading Minecraft";
                                break;
                            case UNPACK_RUNTIME:
                                statusTitle = "Unpacking Runtime";
                                break;
                            case INSTALL_MODPACK:
                                if (contentType != null) {
                                    String typeStr = contentType.substring(0, 1).toUpperCase() + contentType.substring(1);
                                    if ("resourcepack".equals(contentType)) typeStr = "Resource Pack";
                                    statusTitle = "Downloading " + typeStr + ": " + modName;
                                } else {
                                    statusTitle = "Installing Modpack";
                                }
                                break;
                            case EXTRACT_COMPONENTS:
                                statusTitle = "Extracting Components";
                                break;
                            case EXTRACT_SINGLE_FILES:
                                statusTitle = "Extracting Files";
                                break;
                            default:
                                if (resid != -1) {
                                    statusTitle = getContext().getString(resid);
                                } else if (va != null && va.length > 0 && va[0] instanceof String) {
                                    statusTitle = (String) va[0];
                                } else {
                                    statusTitle = "Downloading...";
                                }
                                break;
                        }
                    }
                }

                if (detailStr.isEmpty() && va != null) {
                    if (resid == R.string.newdl_downloading_game_files_size && va.length >= 3) {
                        try {
                            double currentMB = ((Number) va[0]).doubleValue();
                            double totalMB = ((Number) va[1]).doubleValue();
                            speed = ((Number) va[2]).doubleValue();
                            detailStr = String.format(java.util.Locale.US, "%.1f MB / %.1f MB", currentMB, totalMB);
                            if (speed > 0) {
                                double remainingMB = totalMB - currentMB;
                                double etaSeconds = remainingMB / speed;
                                etaStr = formatRemainingTime(etaSeconds);
                            } else {
                                etaStr = "";
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (resid == R.string.newdl_downloading_game_files && va.length >= 3) {
                        try {
                            long currentFiles = ((Number) va[0]).longValue();
                            long totalFiles = ((Number) va[1]).longValue();
                            speed = ((Number) va[2]).doubleValue();
                            detailStr = currentFiles + " / " + totalFiles + " files";
                            etaStr = "";
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (va.length >= 2 && va[0] instanceof Number && va[1] instanceof Number) {
                        try {
                            double currentMB = ((Number) va[0]).doubleValue();
                            double totalMB = ((Number) va[1]).doubleValue();
                            detailStr = String.format(java.util.Locale.US, "%.1f MB / %.1f MB", currentMB, totalMB);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (va.length >= 3 && va[1] instanceof Number && va[2] instanceof Number) {
                        try {
                            double currentMB = ((Number) va[1]).doubleValue();
                            double totalMB = ((Number) va[2]).doubleValue();
                            detailStr = String.format(java.util.Locale.US, "%.1f MB / %.1f MB", currentMB, totalMB);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (va.length > 1 && va[1] != null) {
                            detailStr = String.valueOf(va[1]);
                        } else if (va.length > 0 && va[0] != null) {
                            detailStr = String.valueOf(va[0]);
                        }
                    }
                }

                if (mStatusText != null) {
                    mStatusText.setTextColor(COLOR_TEXT_PRIMARY);
                    CharSequence prev = mStatusText.getText();
                    boolean titleChanged = prev == null || !prev.toString().contentEquals(statusTitle);
                    mStatusText.setText(statusTitle);
                    if (titleChanged && !statusTitle.isEmpty()) {
                        mStatusText.animate().cancel();
                        mStatusText.setAlpha(0f);
                        mStatusText.setTranslationX(dp(8f));
                        mStatusText.animate().alpha(1f).translationX(0f)
                                .setDuration(240)
                                .setInterpolator(SMOOTH)
                                .start();
                    }
                }
                if (mDetailText != null) {
                    String nextDetail = detailStr;
                    CharSequence prevDetail = mDetailText.getText();
                    boolean detailChanged = prevDetail == null || !prevDetail.toString().equals(nextDetail);
                    if (!detailStr.isEmpty()) {
                        mDetailText.setText(detailStr);
                        if (mDetailText.getVisibility() != VISIBLE) {
                            mDetailText.setAlpha(0f);
                            mDetailText.setVisibility(VISIBLE);
                            mDetailText.animate().alpha(1f).setDuration(180).start();
                        } else if (detailChanged) {
                            // subtle crossfade for size updates
                            mDetailText.animate().cancel();
                            mDetailText.animate().alpha(0.78f).setDuration(90).withEndAction(() -> mDetailText.animate().alpha(1f).setDuration(120).start()).start();
                        }
                    } else {
                        if (mDetailText.getVisibility() == VISIBLE) {
                            mDetailText.animate().alpha(0f).setDuration(140).withEndAction(() -> mDetailText.setVisibility(GONE)).start();
                        }
                    }
                }
                boolean isPausedNow = DownloadControl.isPaused(this.progressKey);
                if (mSpeedText != null) {
                    if (speed >= 0 && !isPausedNow) {
                        String nextSpeed = String.format(java.util.Locale.US, "%.1f MB/s", speed);
                        boolean changed = !nextSpeed.equals(mSpeedText.getText().toString());
                        mSpeedText.setText(nextSpeed);
                        if (mSpeedText.getVisibility() != VISIBLE) {
                            mSpeedText.setAlpha(0f);
                            mSpeedText.setVisibility(VISIBLE);
                            mSpeedText.animate().alpha(1f).setDuration(160).start();
                        } else if (changed) {
                            mSpeedText.animate().cancel();
                            mSpeedText.setScaleX(1.06f); mSpeedText.setScaleY(1.06f);
                            mSpeedText.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(SMOOTH).start();
                        }
                    } else {
                        if (mSpeedText.getVisibility() == VISIBLE) mSpeedText.animate().alpha(0f).setDuration(120).withEndAction(() -> mSpeedText.setVisibility(GONE)).start();
                    }
                }
                if (mEtaText != null) {
                    if (!etaStr.isEmpty() && !isPausedNow) {
                        String nextEta = "• " + etaStr;
                        mEtaText.setText(nextEta);
                        if (mEtaText.getVisibility() != VISIBLE) {
                            mEtaText.setAlpha(0f);
                            mEtaText.setVisibility(VISIBLE);
                            mEtaText.animate().alpha(1f).setDuration(160).start();
                        }
                    } else {
                        if (mEtaText.getVisibility() == VISIBLE) mEtaText.animate().alpha(0f).setDuration(120).withEndAction(() -> mEtaText.setVisibility(GONE)).start();
                    }
                }
            });
        }

        @Override
        public void onProgressEnded() {
            post(()-> {
                mLinearLayout.removeView(textView);
            });
        }
    }
}
