package com.kdt;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * CS PREMIUM GLASS TERMINAL (Phase 3).
 *
 * Keeps every legacy behavior (log toggle, autoscroll toggle, cancel, stream
 * batching, animated line arrival) and adds:
 * - translucent glass chrome + status strip (line counter / PAUSED state)
 * - live SEARCH (filter without losing the raw buffer)
 * - PAUSE (keeps buffering silently, resumes cleanly)
 * - COPY to clipboard · EXPORT via share sheet · CLEAR
 * - severity highlighting: error / warning / success / info / debug
 */
public class LoggerView extends ConstraintLayout {
    private Logger.eventLogListener mLogListener;
    private ToggleButton mLogToggle;
    private RecyclerView mLogRecycler;
    private LogLineAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private TextView mCountText;
    private View mLiveDot;
    private View mCliOverlay;
    private TextView mCliCommand;
    private TextView mCliAscii;
    private TextView mCliBrand;
    private TextView mCliStatus;
    private TextView mCliProgressText;
    private android.widget.ProgressBar mCliProgress;
    private boolean mKeepAutoscroll = true;
    private boolean mPaused = false;
    private boolean mSmartMode = true;
    private boolean mCliIntroStarted = false;
    private boolean mCliIntroFinished = false;
    private int mStartupRawLineCount = 0;
    private final java.util.ArrayDeque<String> mSmartImportantBuffer = new java.util.ArrayDeque<>();
    private final java.util.LinkedHashSet<String> mSmartDedup = new java.util.LinkedHashSet<>();
    private Runnable mCliAnimationRunnable;
    private long mSessionStartedAt;
    private int mSmartWarnings;
    private int mSmartErrors;
    private boolean mReadySummaryShown;
    /** Batches rapid emissions so the animator keeps up with burst logger traffic. */
    private final java.util.ArrayDeque<String> mPendingLines = new java.util.ArrayDeque<>();
    private boolean mFlushScheduled = false;

    public LoggerView(@NonNull Context context) {
        this(context, null);
    }

    public LoggerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        // Triggers the log view shown state by default when viewing it
            if (mLogToggle != null) mLogToggle.setChecked(visibility == VISIBLE);
    }

    private void init(){
        inflate(getContext(), R.layout.view_logger, this);

        // ── Animated terminal stream ──
        mLogRecycler = findViewById(R.id.content_log_recycler);
        mAdapter = new LogLineAdapter();
        mLayoutManager = new LinearLayoutManager(getContext());
        mLogRecycler.setLayoutManager(mLayoutManager);
        mLogRecycler.setAdapter(mAdapter);
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(120);
        animator.setChangeDuration(100);
        animator.setMoveDuration(160);
        animator.setRemoveDuration(120);
        mLogRecycler.setItemAnimator(animator);
        mLogRecycler.setVisibility(GONE);

        mCountText = findViewById(R.id.log_count_text);
        mLiveDot = findViewById(R.id.log_live_dot);
        mCliOverlay = findViewById(R.id.log_cli_overlay);
        mCliCommand = findViewById(R.id.log_cli_command);
        mCliAscii = findViewById(R.id.log_cli_ascii);
        mCliBrand = findViewById(R.id.log_cli_brand);
        mCliStatus = findViewById(R.id.log_cli_status);
        mCliProgress = findViewById(R.id.log_cli_progress);
        mCliProgressText = findViewById(R.id.log_cli_progress_text);

        // Toggle log visibility
        mLogToggle = findViewById(R.id.content_log_toggle_log);
        mLogToggle.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if(isChecked) {
                        // Soft body fade-in: the terminal "opens" instead of popping
                        mLogRecycler.setAlpha(0f);
                        mLogRecycler.setTranslationY(dp(14));
                        mLogRecycler.setVisibility(VISIBLE);
                        mLogRecycler.animate().alpha(1f).translationY(0f)
                                .setDuration(320)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .start();
                        String mode = LauncherPreferences.DEFAULT_PREF != null
                                ? LauncherPreferences.DEFAULT_PREF.getString(
                                        "game_log_display_mode", "animated_smart")
                                : "animated_smart";
                        mSmartMode = !"full_raw".equals(mode);
                        if (mAdapter.getItemCount() == 0) {
                            if (mSmartMode && !mCliIntroStarted) {
                                startCliIntro();
                            } else {
                                mAdapter.appendLine(new LogLineAdapter.LogLine(
                                        tinted("› Waiting for game output…", 0xFF6B7280),
                                        "› Waiting for game output…"));
                            }
                        }
                        Logger.addLogListener(mLogListener);
                    }else{
                        mAdapter.clear();
                        if (mCliAnimationRunnable != null) removeCallbacks(mCliAnimationRunnable);
                        if (mCliOverlay != null) mCliOverlay.setVisibility(GONE);
                        if (mCliIntroStarted) mCliIntroFinished = true;
                        Logger.removeLogListener(mLogListener);
                        mLogRecycler.animate().cancel();
                        mLogRecycler.setVisibility(GONE);
                    }
                    updateStatus();
                });
        mLogToggle.setChecked(false);

        // Remove the loggerView from the user View
        ImageButton cancelButton = findViewById(R.id.log_view_cancel);
        cancelButton.setOnClickListener(view -> LoggerView.this.setVisibility(GONE));

        // Autoscroll switch
        ToggleButton autoscrollToggle = findViewById(R.id.content_log_toggle_autoscroll);
        autoscrollToggle.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    mKeepAutoscroll = isChecked;
                    if(isChecked) scrollToEnd(false);
                }
        );
        autoscrollToggle.setChecked(true);

        // ── Phase 3 controls ──
        EditText search = findViewById(R.id.log_search_input);
        View searchClear = findViewById(R.id.log_search_clear);
        if (searchClear != null) {
            searchClear.setOnClickListener(v -> {
                search.setText("");
                search.clearFocus();
                mAdapter.setFilter(null);
                updateStatus();
                try {
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                            getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
                } catch (Throwable ignored) {}
            });
        }
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                mAdapter.setFilter(s != null ? s.toString() : null);
                if (searchClear != null) {
                    searchClear.setVisibility(s != null && s.length() > 0 ? VISIBLE : GONE);
                }
                if (mKeepAutoscroll) scrollToEnd(false);
                updateStatus();
            }
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            search.clearFocus();
            try {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
            } catch (Throwable ignored) {}
            return true;
        });

        ImageButton pause = findViewById(R.id.log_btn_pause);
        pause.setOnClickListener(v -> togglePause());

        ImageButton copy = findViewById(R.id.log_btn_copy);
        copy.setOnClickListener(v -> copyLogs());

        ImageButton export = findViewById(R.id.log_btn_export);
        export.setOnClickListener(v -> exportLogs());

        ImageButton clear = findViewById(R.id.log_btn_clear);
        clear.setOnClickListener(v -> {
            mAdapter.clear();
            updateStatus();
        });

        // Listen to logs — batched + buttery
        mLogListener = text -> {
            if(mLogRecycler.getVisibility() != VISIBLE) return;
            synchronized (mPendingLines) {
                mPendingLines.add(text);
                // paused → keep buffering silently (bounded) without flushing
                if (mPaused && mPendingLines.size() > 600) mPendingLines.poll();
            }
            if (!mPaused) scheduleFlush();
        };

        // Soft pulse on the live indicator while streaming
        if (mLiveDot != null) {
            android.animation.ValueAnimator pulse = android.animation.ValueAnimator.ofFloat(1f, 0.35f);
            pulse.setDuration(900);
            pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            pulse.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            pulse.addUpdateListener(a -> {
                View dot = mLiveDot;
                if (dot != null) dot.setAlpha((Float) a.getAnimatedValue());
            });
            pulse.start();
        }
    }

    // ═══════════════════════ ACTIONS ═══════════════════════

    private void togglePause() {
        mPaused = !mPaused;
        if (!mPaused) scheduleFlush(); // drain everything buffered while paused
        if (mLiveDot != null) {
            mLiveDot.getBackground().setTint(mPaused ? 0xFFFFB020 : 0xFF22C55E);
        }
        ImageButton pause = findViewById(R.id.log_btn_pause);
        if (pause != null) pause.setColorFilter(mPaused ? 0xFFFFB020 : 0xFFFFFFFF);
        updateStatus();
    }

    private void copyLogs() {
        String text = mAdapter.dumpText();
        if (text.isEmpty()) {
            Toast.makeText(getContext(), R.string.cs_log_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        android.content.ClipboardManager cm =
                (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(android.content.ClipData.newPlainText("game_logs", text));
        }
        Toast.makeText(getContext(), R.string.cs_log_copied, Toast.LENGTH_SHORT).show();
    }

    private void exportLogs() {
        String text = mAdapter.dumpText();
        if (text.isEmpty()) {
            Toast.makeText(getContext(), R.string.cs_log_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "CS Launcher V3 — Game Log");
        send.putExtra(Intent.EXTRA_TEXT, text);
        try {
            getContext().startActivity(Intent.createChooser(send,
                    getContext().getString(R.string.cs_log_export)));
        } catch (Exception ignored) {}
    }

    private void updateStatus() {
        if (mCountText == null) return;
        String state = mPaused ? "PAUSED" : "LIVE";
        int raw = mAdapter.rawCount();
        int vis = mAdapter.visibleCount();
        String mode = mSmartMode ? "SMART" : "RAW";
        String health = mSmartMode
                ? " • " + mSmartWarnings + " warn • " + mSmartErrors + " err" : "";
        mCountText.setText((vis == raw
                ? raw + " lines"
                : vis + "/" + raw + " lines") + " • " + mode + health + " • " + state);
    }

    // ═══════════════════════ SMART CLI INTRO ═══════════════════════

    private void startCliIntro() {
        mCliIntroStarted = true;
        mCliIntroFinished = false;
        mStartupRawLineCount = 0;
        mSmartImportantBuffer.clear();
        mSmartDedup.clear();
        mSmartWarnings = 0;
        mSmartErrors = 0;
        mReadySummaryShown = false;
        mSessionStartedAt = android.os.SystemClock.elapsedRealtime();

        final String command = "> Initializing CS Launcher...";
        final String[] spinner = {"-", "\\", "|", "/"};
        if (mCliOverlay == null) {
            mCliIntroFinished = true;
            return;
        }
        mCliOverlay.animate().cancel();
        mCliOverlay.setVisibility(VISIBLE);
        mCliOverlay.setAlpha(0f);
        mCliOverlay.setScaleX(0.97f);
        mCliOverlay.setScaleY(0.97f);
        mCliOverlay.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(240).withLayer().start();
        mCliCommand.setText("▋");
        mCliAscii.setAlpha(0f);
        mCliAscii.setTranslationY(8f * getResources().getDisplayMetrics().density);
        mCliBrand.setAlpha(0f);
        mCliBrand.setScaleX(0.92f);
        mCliBrand.setScaleY(0.92f);
        mCliStatus.setText("");
        mCliProgress.setProgress(0);
        mCliProgressText.setText("[----------] 0%");

        mCliAnimationRunnable = new Runnable() {
            int stage = 0;
            int charIndex = 0;
            int progress = 0;
            int spin = 0;

            @Override public void run() {
                if (mCliIntroFinished) return;
                if (stage == 0) {
                    if (charIndex < command.length()) {
                        charIndex++;
                        mCliCommand.setText(command.substring(0, charIndex) + "▋");
                        postDelayed(this, 30L);
                        return;
                    }
                    mCliCommand.setText(command);
                    mCliAscii.animate().alpha(1f).translationY(0f)
                            .setDuration(360)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f))
                            .withLayer().start();
                    mCliBrand.animate().alpha(1f).scaleX(1f).scaleY(1f)
                            .setStartDelay(110L).setDuration(320)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f))
                            .withLayer().start();
                    stage = 1;
                    postDelayed(this, 380L);
                    return;
                }

                progress = Math.min(100, progress + 6);
                String status;
                if (progress < 32) {
                    status = "> Runtime ........ READY\n> Renderer ....... " + spinner[spin++ % spinner.length];
                } else if (progress < 65) {
                    status = "> Runtime ........ READY\n> Renderer ....... READY";
                } else if (progress < 92) {
                    status = "> Renderer ....... READY\n> Mod loader ..... " + spinner[spin++ % spinner.length];
                } else {
                    status = "> Mod loader ..... READY\n> Starting Minecraft...";
                }
                mCliStatus.setText(status);
                mCliProgress.setProgress(progress);
                int filled = Math.min(10, progress / 10);
                StringBuilder bar = new StringBuilder("[");
                for (int i = 0; i < 10; i++) bar.append(i < filled ? '#' : '-');
                bar.append("] ").append(progress).append('%');
                mCliProgressText.setText(bar.toString());
                if (progress < 100) {
                    postDelayed(this, 85L);
                } else {
                    postDelayed(() -> hideCliOverlayAndFinish(false), 240L);
                }
            }
        };
        post(mCliAnimationRunnable);
    }

    private void hideCliOverlayAndFinish(boolean immediate) {
        if (mCliOverlay == null) {
            finishCliIntro();
            return;
        }
        mCliOverlay.animate().cancel();
        if (immediate) {
            mCliOverlay.setVisibility(GONE);
            finishCliIntro();
            return;
        }
        mCliOverlay.animate().alpha(0f).scaleX(0.985f).scaleY(0.985f)
                .setDuration(220).withLayer()
                .withEndAction(() -> {
                    mCliOverlay.setVisibility(GONE);
                    mCliOverlay.setAlpha(1f);
                    mCliOverlay.setScaleX(1f);
                    mCliOverlay.setScaleY(1f);
                    finishCliIntro();
                }).start();
    }

    private void finishCliIntro() {
        if (mCliIntroFinished) return;
        mCliIntroFinished = true;
        if (mCliAnimationRunnable != null) removeCallbacks(mCliAnimationRunnable);
        appendSmartImportant("[SYSTEM] CS Launcher bootstrap complete");
        while (!mSmartImportantBuffer.isEmpty()) {
            appendSmartImportant(mSmartImportantBuffer.poll());
        }
        if (mKeepAutoscroll) scrollToEnd(true);
        updateStatus();
    }

    /** Deduplicate repetitive mod-loader messages before they reach the user. */
    private boolean appendSmartImportant(@Nullable String important) {
        if (important == null || important.trim().isEmpty()) return false;
        String key = important.replaceAll("\\s+", " ").trim();
        if (!mSmartDedup.add(key)) return false;
        if (mSmartDedup.size() > 240) {
            java.util.Iterator<String> it = mSmartDedup.iterator();
            if (it.hasNext()) { it.next(); it.remove(); }
        }
        String up = important.toUpperCase(java.util.Locale.US);
        if (up.startsWith("[ERROR]")) mSmartErrors++;
        else if (up.startsWith("[WARNING]")) mSmartWarnings++;
        mAdapter.appendLine(new LogLineAdapter.LogLine(
                styledSmartLine(important), important));
        if (up.startsWith("[READY]") && !mReadySummaryShown) {
            mReadySummaryShown = true;
            String summary = mSmartErrors == 0
                    ? "[SESSION] Launch healthy • " + mSmartWarnings + " actionable warning(s)"
                    : "[SESSION] Launch completed with " + mSmartErrors + " error(s)";
            mAdapter.appendLine(new LogLineAdapter.LogLine(styledSmartLine(summary), summary));
        }
        return true;
    }

    /** Timestamp + colored level + readable message, similar to modern log viewers. */
    private CharSequence styledSmartLine(String important) {
        long elapsed = Math.max(0L, android.os.SystemClock.elapsedRealtime() - mSessionStartedAt);
        String stamp = String.format(java.util.Locale.US, "%02d:%02d.%d  ",
                elapsed / 60000L, (elapsed / 1000L) % 60L, (elapsed / 100L) % 10L);
        String full = stamp + important;
        android.text.SpannableString styled = new android.text.SpannableString(full);
        styled.setSpan(new android.text.style.ForegroundColorSpan(0xFF5F626D),
                0, stamp.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int tagStart = stamp.length();
        int tagEnd = important.indexOf(']') + 1;
        if (tagEnd > 0) {
            tagEnd += stamp.length();
            String tag = important.substring(0, important.indexOf(']') + 1).toUpperCase(java.util.Locale.US);
            int color = tag.contains("ERROR") ? 0xFFFF6B74
                    : tag.contains("WARN") ? 0xFFFFB020
                    : tag.contains("READY") ? 0xFF8FD3A5
                    : tag.contains("MOD") ? 0xFFB8A7E8
                    : tag.contains("GRAPHICS") ? 0xFF91B7D9
                    : 0xFFB9BBC4;
            styled.setSpan(new android.text.style.ForegroundColorSpan(color),
                    tagStart, tagEnd, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            styled.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    tagStart, tagEnd, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (tagEnd < full.length()) {
                styled.setSpan(new android.text.style.ForegroundColorSpan(0xFFD1D2D8),
                        tagEnd, full.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return styled;
    }

    private boolean isCriticalLine(String line) {
        String up = line.toUpperCase(java.util.Locale.US);
        return up.contains("FATAL") || up.contains("ERROR") || up.contains("EXCEPTION")
                || up.contains("CAUSED BY") || up.contains("OUTOFMEMORY")
                || up.contains("UNSATISFIEDLINK") || up.contains("MIXIN APPLY FAILED")
                || up.contains("CRASH REPORT");
    }

    /** Return a user-readable important line, or null for harmless startup spam. */
    @Nullable
    private String toSmartLine(String line) {
        if (line == null) return null;
        String clean = line.trim();
        String up = clean.toUpperCase(java.util.Locale.US);
        if (clean.isEmpty()) return null;

        // Errors remain complete: diagnostics must never be simplified away.
        if (isCriticalLine(clean)) return "[ERROR] " + clean;

        // Hide harmless warning spam; surface warnings a player can act on.
        if (up.contains("WARN")) {
            boolean actionable = up.contains("MOD") || up.contains("MIXIN")
                    || up.contains("RENDER") || up.contains("OPENGL")
                    || up.contains("VULKAN") || up.contains("MISSING")
                    || up.contains("UNSUPPORTED") || up.contains("MEMORY")
                    || up.contains("AUTH") || up.contains("FAILED")
                    || up.contains("INCOMPATIB");
            return actionable ? "[WARNING] " + clean : null;
        }

        if (up.contains("FABRIC LOADER"))
            return "[MOD LOADER] Fabric Loader initialized";
        if (up.contains("NEOFORGE"))
            return "[MOD LOADER] NeoForge initialized";
        if (up.contains("FORGE MOD LOADER") || up.contains("MINECRAFTFORGE")
                || up.contains("MODLAUNCHER RUNNING"))
            return "[MOD LOADER] Forge initialized";
        if (up.contains("QUILT LOADER"))
            return "[MOD LOADER] Quilt Loader initialized";

        java.util.regex.Matcher count = java.util.regex.Pattern
                .compile("(?i)(?:loading|loaded|discover(?:ed)?|found)\\s+(\\d+)\\s+mods?")
                .matcher(clean);
        if (count.find()) return "[MODS] " + count.group(1) + " mods discovered";

        if (up.contains("SODIUM")) return "[MOD] Sodium performance engine active";
        if (up.contains("IRIS")) return "[MOD] Iris shader support active";
        if (up.contains("OPTIFINE")) return "[MOD] OptiFine detected";
        if (up.contains("IMMEDIATELYFAST")) return "[MOD] ImmediatelyFast active";
        if (up.contains("LITHIUM")) return "[MOD] Lithium optimization active";

        // Renderer/JVM implementation chatter is intentionally hidden in Smart
        // mode; the CLI intro already reports readiness. Real failures/warnings
        // were handled above and are never suppressed.
        if (up.contains("MOBILEGLUES") || up.contains(" ZINK")
                || up.contains("GALLIUM_DRIVER=ZINK") || up.contains("GL4ES")
                || up.contains("OPENGL VENDOR") || up.contains("BACKEND LIBRARY")
                || up.contains("LWJGL VERSION") || up.contains("JAVA VERSION")
                || up.contains("JAVA RUNTIME") || up.contains("JVM INITIALIZED")) {
            return null;
        }

        if (up.contains("SOUND ENGINE STARTED") || up.contains("OPENAL INITIALIZED"))
            return "[AUDIO] Game audio engine ready";
        if (up.contains("RELOADING RESOURCEMANAGER") || up.contains("RESOURCE RELOAD"))
            return "[RESOURCES] Resource packs loaded";
        if (up.contains("AUTHENTICATED") || up.contains("LOGGED IN AS"))
            return "[ACCOUNT] Premium session authenticated";
        if (up.contains("DATAFIXER") && up.contains("OPTIM"))
            return "[GAME] Optimizing Minecraft world data";
        if (up.contains("SETTING USER")) return "[GAME] Player profile attached";
        if (up.contains("STARTING MINECRAFT")) return "[GAME] Minecraft process started";
        if (up.contains("PREPARING SPAWN AREA")) return "[WORLD] Preparing spawn area";
        if (up.contains("JOINING WORLD") || up.contains("JOINED THE GAME"))
            return "[WORLD] Entering the Minecraft world";
        if (up.contains("CONNECTING TO") && (up.contains("SERVER") || up.contains(":")))
            return "[NETWORK] Connecting to multiplayer server";
        if (up.contains("SAVING CHUNKS")) return "[WORLD] Saving world data";
        if (up.contains("DONE (") && (up.contains("MAIN") || up.contains("SERVER")))
            return "[READY] Minecraft finished loading";
        if (up.contains("JAVA EXIT CODE")) return "[GAME] " + clean;

        // Intentionally hidden: JVM argument dumps, env variables, classpath,
        // individual mixin registrations and repetitive debug/info chatter.
        return null;
    }

    // ═══════════════════════ STREAM ═══════════════════════

    private void scheduleFlush() {
        synchronized (mPendingLines) {
            if (mFlushScheduled) return;
            mFlushScheduled = true;
        }
        postDelayed(this::flushPending, 90);
    }

    private void flushPending() {
        String line;
        boolean inserted = false;
        for (;;) {
            synchronized (mPendingLines) {
                line = mPendingLines.poll();
                if (line == null) {
                    mFlushScheduled = false;
                    break;
                }
            }
            if (!mSmartMode) {
                mAdapter.appendLine(new LogLineAdapter.LogLine(colorizeLine(line), line));
                inserted = true;
                continue;
            }

            String important = toSmartLine(line);
            if (!mCliIntroFinished) {
                // Raw logging continues normally in Logger/latestlog.txt. During
                // the non-blocking visual intro we retain only readable events.
                mStartupRawLineCount++;
                if (isCriticalLine(line)) {
                    hideCliOverlayAndFinish(true);
                    appendSmartImportant(important);
                    inserted = true;
                } else if (important != null && mSmartImportantBuffer.size() < 80) {
                    mSmartImportantBuffer.add(important);
                }
                continue;
            }

            if (important != null && appendSmartImportant(important)) {
                inserted = true;
            }
        }
        if (inserted) {
            if (mKeepAutoscroll) scrollToEnd(true);
            updateStatus();
        }
    }

    /** Glide to the newest line; immediate jump for toggle-driven seeks. */
    private void scrollToEnd(boolean smooth) {
        int last = mAdapter.getItemCount() - 1;
        if (last < 0) return;
        if (smooth) {
            mLogRecycler.smoothScrollToPosition(last);
        } else {
            mLogRecycler.scrollToPosition(last);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private static CharSequence tinted(String text, int color) {
        android.text.SpannableString s = new android.text.SpannableString(text);
        s.setSpan(new android.text.style.ForegroundColorSpan(color), 0, text.length(),
                android.text.SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    /** Severity highlighting: error → red, warn → amber, success → green, info → silver. */
    private CharSequence colorizeLine(String line) {
        int color = 0xFFD0D0D0; // default body tone
        String up = line.toUpperCase(java.util.Locale.US);
        if (up.contains("ERROR") || up.contains("FATAL") || up.contains("EXCEPTION")
                || up.contains("CAUSED BY") || up.contains("FAILED")) {
            color = 0xFFFF6B74; // error red
        } else if (up.contains("WARN")) {
            color = 0xFFFFB020; // warning amber
        } else if (up.startsWith("[READY]") || up.contains("SUCCESS")
                || up.contains("DONE!") || up.contains("DONE ")
                || up.contains("FINISHED") || up.contains("STARTED SERVER")
                || up.contains("SUCCESSFULLY")) {
            color = 0xFF22C55E; // success green
        } else if (up.startsWith("[INFO]") || up.contains(" INFO ")) {
            color = 0xFFFFFFFF; // silver
        } else if (up.contains("DEBUG")) {
            color = 0xFF6B7280; // dim
        }
        return tinted(line, color);
    }
}
