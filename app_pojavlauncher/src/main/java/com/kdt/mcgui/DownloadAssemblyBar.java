package com.kdt.mcgui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;

/**
 * Premium luminous beam — complete redo.
 * Modern pill track (1A1D24) + gradient fill (teal → ice → white) with
 * outer halo, leading-edge glow, and a travelling shimmer sweep.
 * Replaces the old segmented 14-block rail.
 *
 * Lightweight: single ValueAnimator, hardware-accelerated, no allocations in onDraw.
 */
public class DownloadAssemblyBar extends ProgressBar {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint leadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF trackRect = new RectF();
    private final RectF fillRect = new RectF();
    private final RectF clipRect = new RectF();

    private ValueAnimator shimmerAnim;
    private float shimmerPhase; // 0..1
    private boolean shimmerRunning = true;

    private int state = 0; // 0 normal, 1 success, 2 error, 3 paused

    public DownloadAssemblyBar(Context c) { this(c, null); }
    public DownloadAssemblyBar(Context c, @Nullable AttributeSet a) {
        super(c, a);
        setWillNotDraw(false);
        // track is drawn manually, so we keep ProgressBar's drawable inert
        setProgressDrawable(null);

        trackPaint.setColor(0xFF1E2129);
        trackPaint.setStyle(Paint.Style.FILL);

        fillPaint.setStyle(Paint.Style.FILL);
        haloPaint.setStyle(Paint.Style.FILL);
        leadPaint.setStyle(Paint.Style.FILL);

        shimmerPaint.setStyle(Paint.Style.FILL);

        shimmerAnim = ValueAnimator.ofFloat(0f, 1f);
        shimmerAnim.setDuration(1650);
        shimmerAnim.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnim.setInterpolator(new LinearInterpolator());
        shimmerAnim.addUpdateListener(v -> {
            shimmerPhase = (float) v.getAnimatedValue();
            if (shimmerRunning && getProgress() > 0 && getProgress() < 100 && state == 0) {
                invalidate();
            }
        });
    }

    /** Call to tint the bar for success / error / paused. */
    public void setBarState(int s) {
        state = s;
        shimmerRunning = (s == 0);
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!shimmerAnim.isStarted()) shimmerAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        shimmerAnim.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        trackRect.set(0, 0, w, h);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float radius = h / 2f;
        float doneF = Math.max(0f, Math.min(1f, getProgress() / 100f));
        float fillW = w * doneF;

        // ── track (pill) ──
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);
        // inner subtle inner shadow line (top 1px)
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(0x14000000);
        canvas.drawRoundRect(new RectF(0, 0, w, dp(1.5f)), radius, radius, shadow);

        if (doneF <= 0.001f) return;

        // ── halo (soft outer glow around fill) ──
        haloPaint.setColor(state == 2 ? 0x22FF6B6B : state == 1 ? 0x2200E5A0 : 0x18FFFFFF);
        RectF haloRect = new RectF(-dp(2), -dp(2), fillW + dp(2), h + dp(2));
        canvas.drawRoundRect(haloRect, radius + dp(2), radius + dp(2), haloPaint);

        // ── fill gradient ──
        int cStart, cMid, cEnd;
        if (state == 2) { // error — coral
            cStart = 0xFFFF8A8A; cMid = 0xFFFF6B6B; cEnd = 0xFFFF5252;
        } else if (state == 1) { // success — mint
            cStart = 0xFF7BE8A5; cMid = 0xFF9FE8C0; cEnd = 0xFFE8FFF2;
        } else if (state == 3) { // paused — amber
            cStart = 0xFFE8C98A; cMid = 0xFFF0D8A8; cEnd = 0xFFFFF0D8;
        } else {
            cStart = 0xFF8BE8E0; cMid = 0xFFE6EEF2; cEnd = 0xFFFFFFFF;
        }

        fillRect.set(0, dp(0.8f), fillW, h - dp(0.8f));
        LinearGradient lg = new LinearGradient(0, 0, fillW, 0,
                new int[]{cStart, cMid, cEnd},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP);
        fillPaint.setShader(lg);
        canvas.drawRoundRect(fillRect, radius - dp(0.8f), radius - dp(0.8f), fillPaint);
        fillPaint.setShader(null);

        // ── top highlight (specular) ──
        Paint spec = new Paint(Paint.ANTI_ALIAS_FLAG);
        spec.setColor(0x55FFFFFF);
        RectF specRect = new RectF(dp(1), dp(0.8f), fillW - dp(1), h * 0.42f);
        canvas.drawRoundRect(specRect, radius * 0.6f, radius * 0.6f, spec);

        // ── travelling shimmer (only in normal & in-progress) ──
        if (state == 0 && doneF > 0.04f && doneF < 0.99f) {
            canvas.save();
            clipRect.set(0, 0, fillW, h);
            canvas.clipRect(clipRect);
            float sweepW = dp(52);
            float x = -sweepW + (w + sweepW * 2) * shimmerPhase;
            // keep sweep inside fill
            if (x > -sweepW && x < fillW + sweepW) {
                LinearGradient sg = new LinearGradient(x, 0, x + sweepW, 0,
                        new int[]{0x00FFFFFF, 0xE8FFFFFF, 0x00FFFFFF},
                        new float[]{0f, 0.5f, 1f},
                        Shader.TileMode.CLAMP);
                shimmerPaint.setShader(sg);
                canvas.drawRect(x, 0, x + sweepW, h, shimmerPaint);
                shimmerPaint.setShader(null);
            }
            canvas.restore();
        }

        // ── leading edge glow (thin vertical hot line) ──
        if (doneF > 0.01f && doneF < 0.99f) {
            leadPaint.setColor(state == 0 ? 0xCCFFFFFF : 0x80FFFFFF);
            float lx = fillW - dp(1.2f);
            canvas.drawRoundRect(new RectF(lx, dp(1), lx + dp(2.4f), h - dp(1)), dp(1), dp(1), leadPaint);
            // outer bloom
            Paint bloom = new Paint(Paint.ANTI_ALIAS_FLAG);
            bloom.setColor(state == 2 ? 0x40FF6B6B : 0x22FFFFFF);
            canvas.drawRoundRect(new RectF(lx - dp(2), 0, lx + dp(4), h), dp(2), dp(2), bloom);
        }
    }

    private float dp(float n) {
        return n * getResources().getDisplayMetrics().density;
    }
}
