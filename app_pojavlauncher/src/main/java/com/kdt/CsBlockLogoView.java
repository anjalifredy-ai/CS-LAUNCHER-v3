package com.kdt;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * The "CS" mark of the animated launch console, drawn as an LED pixel grid.
 *
 * <p>Why not an ASCII-art TextView (what this replaces): the old art was built from box-drawing
 * and block characters. Android's monospace font does not ship glyphs for most of them, so the
 * system silently fell back to a proportional font for those characters — every row then had a
 * different advance width and the logo sheared apart, which is why the "CS" never looked right.
 * On top of that the TextView centred each line individually and wrapped the art on narrow
 * screens, shredding it further.
 *
 * <p>Here the letters are a bitmap pattern drawn as rounded cells, so the result is pixel-exact
 * on every device and font, always centred as one block, and always scaled to fit.
 *
 * <p>Animation: cells switch on in a diagonal wave, then a highlight band keeps sweeping across
 * the mark with a soft glow — a terminal booting up rather than text appearing.
 */
public final class CsBlockLogoView extends View {

    /** 9 rows; '#' = lit cell. Two glyphs separated by a spacer column. */
    private static final String[] GLYPH_C = {
            "..######",
            ".#######",
            "##......",
            "##......",
            "##......",
            "##......",
            "##......",
            ".#######",
            "..######",
    };
    private static final String[] GLYPH_S = {
            ".#######",
            "########",
            "##......",
            "###.....",
            ".######.",
            ".....###",
            "......##",
            "########",
            "#######.",
    };
    private static final int SPACER_COLS = 2;

    private static final int ROWS = 9;
    private static final int COLS = 8 + SPACER_COLS + 8;

    private static final long REVEAL_MS = 900L;
    private static final long SWEEP_PERIOD_MS = 2600L;

    private final boolean[][] mCells = new boolean[ROWS][COLS];
    private final Paint mCellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mCellRect = new RectF();

    private ValueAnimator mAnimator;
    /** 0 → nothing lit, 1 → fully lit. */
    private float mReveal = 1f;
    /** Continuously running sweep position, in columns. */
    private float mSweep = -4f;
    private long mRevealStartedAt = -1L;

    private int mBaseColor = 0xFFE9EBF2;
    private int mDimColor = 0xFF2A2F3A;
    private int mHotColor = 0xFFFFFFFF;

    public CsBlockLogoView(Context context) { this(context, null); }
    public CsBlockLogoView(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public CsBlockLogoView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        buildCells();
        mCellPaint.setStyle(Paint.Style.FILL);
        mGlowPaint.setStyle(Paint.Style.FILL);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    private void buildCells() {
        for (int r = 0; r < ROWS; r++) {
            String c = GLYPH_C[r];
            String s = GLYPH_S[r];
            for (int i = 0; i < 8; i++) {
                mCells[r][i] = c.charAt(i) == '#';
                mCells[r][8 + SPACER_COLS + i] = s.charAt(i) == '#';
            }
        }
    }

    /** Restarts the boot-up wave. Safe to call repeatedly. */
    public void startReveal() {
        mReveal = 0f;
        mRevealStartedAt = -1L;
        ensureAnimator();
        invalidate();
    }

    /** Shows the mark fully lit without replaying the wave. */
    public void showInstantly() {
        mReveal = 1f;
        mRevealStartedAt = 0L;
        ensureAnimator();
        invalidate();
    }

    private void ensureAnimator() {
        if (mAnimator != null && mAnimator.isRunning()) return;
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(SWEEP_PERIOD_MS);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            // sweep travels a bit beyond both edges so the band enters and leaves cleanly
            mSweep = -5f + f * (COLS + 10f);
            if (mRevealStartedAt < 0L) mRevealStartedAt = android.os.SystemClock.uptimeMillis();
            if (mReveal < 1f) {
                long dt = android.os.SystemClock.uptimeMillis() - mRevealStartedAt;
                mReveal = Math.min(1f, dt / (float) REVEAL_MS);
            }
            invalidate();
        });
        mAnimator.start();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ensureAnimator();
    }

    @Override protected void onDetachedFromWindow() {
        if (mAnimator != null) { mAnimator.cancel(); mAnimator = null; }
        super.onDetachedFromWindow();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(dp(200), widthMeasureSpec);
        // keep the mark's aspect ratio whatever width we are given
        int desiredHeight = Math.round(width * (ROWS / (float) COLS));
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        // One cell size for both axes keeps the grid square and the logo undistorted.
        float cell = Math.min(w / (float) COLS, h / (float) ROWS);
        float gridW = cell * COLS, gridH = cell * ROWS;
        float left = (w - gridW) * 0.5f;
        float top = (h - gridH) * 0.5f;
        float inset = Math.max(0.6f, cell * 0.12f);
        float radius = Math.max(1f, cell * 0.22f);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (!mCells[r][c]) continue;

                // Diagonal boot wave: left-top cells light up first.
                float cellDelay = (c + r * 0.6f) / (COLS + ROWS * 0.6f);
                float local = (mReveal - cellDelay * 0.75f) / 0.25f;
                if (local <= 0f) continue;
                float on = Math.min(1f, local);
                on = 1f - (1f - on) * (1f - on); // ease-out

                // Highlight band riding across the mark.
                float dist = Math.abs(c - mSweep);
                float hot = dist > 3.2f ? 0f : (float) Math.pow(1f - dist / 3.2f, 2.0);

                float x = left + c * cell;
                float y = top + r * cell;
                mCellRect.set(x + inset, y + inset, x + cell - inset, y + cell - inset);

                if (hot > 0.05f) {
                    mGlowPaint.setColor(mHotColor);
                    mGlowPaint.setAlpha((int) (70 * hot * on));
                    float g = cell * 0.35f * hot;
                    canvas.drawRoundRect(mCellRect.left - g, mCellRect.top - g,
                            mCellRect.right + g, mCellRect.bottom + g, radius + g, radius + g, mGlowPaint);
                }

                mCellPaint.setColor(lerpColor(mBaseColor, mHotColor, hot));
                mCellPaint.setAlpha((int) (255 * (0.72f + 0.28f * hot) * on));

                float pop = 0.86f + 0.14f * on;
                float cxp = mCellRect.centerX(), cyp = mCellRect.centerY();
                float hw = mCellRect.width() * 0.5f * pop, hh = mCellRect.height() * 0.5f * pop;
                canvas.drawRoundRect(cxp - hw, cyp - hh, cxp + hw, cyp + hh, radius, radius, mCellPaint);
            }
        }

        // Unlit cells of the grid rectangle stay invisible on purpose: a faint matrix would
        // fight with the terminal text underneath on small screens.
    }

    private static int lerpColor(int from, int to, float t) {
        if (t <= 0f) return from;
        if (t >= 1f) return to;
        int fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        int r = (int) (fr + (tr - fr) * t);
        int g = (int) (fg + (tg - fg) * t);
        int b = (int) (fb + (tb - fb) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
