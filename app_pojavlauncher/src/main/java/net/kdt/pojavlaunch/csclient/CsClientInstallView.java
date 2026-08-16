package net.kdt.pojavlaunch.csclient;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * CS Client-only installation visual. A restrained orbit/core animation is
 * combined with real installer progress; it is never used by normal downloads.
 */
public final class CsClientInstallView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private final Path diamond = new Path();
    private ValueAnimator orbitAnimator;
    private float orbit;
    private float displayedProgress;
    private float targetProgress;
    private String stage = "PREPARING";
    private String detail = "Atomic profile installation";
    private final String[] steps = {"LOADER", "PACK", "BLAZE", "MODS", "FINAL"};

    public CsClientInstallView(Context context) { super(context); init(); }
    public CsClientInstallView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public CsClientInstallView(Context context, @Nullable AttributeSet attrs, int style) { super(context, attrs, style); init(); }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        orbitAnimator = ValueAnimator.ofFloat(0f, 1f);
        orbitAnimator.setDuration(2400L);
        orbitAnimator.setRepeatCount(ValueAnimator.INFINITE);
        orbitAnimator.setInterpolator(new LinearInterpolator());
        orbitAnimator.addUpdateListener(a -> {
            orbit = (float) a.getAnimatedValue();
            float delta = targetProgress - displayedProgress;
            if (Math.abs(delta) > .15f) displayedProgress += delta * .12f;
            else displayedProgress = targetProgress;
            invalidate();
        });
    }

    public void setInstallProgress(int percent, String status) {
        targetProgress = Math.max(0, Math.min(100, percent));
        if (status != null && !status.trim().isEmpty()) {
            detail = status;
            stage = stageFor(percent);
        }
        if (getVisibility() == VISIBLE && !orbitAnimator.isStarted()) orbitAnimator.start();
        invalidate();
    }

    public void start() {
        if (!orbitAnimator.isStarted()) orbitAnimator.start();
    }

    public void stop() {
        if (orbitAnimator != null) orbitAnimator.cancel();
    }

    private String stageFor(int p) {
        if (p < 18) return "RESOLVING FABRIC LOADER";
        if (p < 45) return "MERGING MODPACK";
        if (p < 58) return "INSTALLING BLAZE CLIENT";
        if (p < 90) return "INSTALLING VERIFIED MODS";
        return "FINALIZING PROFILE";
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getVisibility() == VISIBLE) start();
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        final float w = getWidth(), h = getHeight();
        final float cx = w / 2f, cy = h * .39f;
        final float r = Math.min(w, h) * .19f;
        final int cyan = Color.rgb(34, 201, 203);

        // Quiet halo and progress track.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(3));
        paint.setColor(0x2622C9CB);
        c.drawCircle(cx, cy, r, paint);
        arc.set(cx-r, cy-r, cx+r, cy+r);
        paint.setColor(cyan);
        c.drawArc(arc, -90, 360f * displayedProgress / 100f, false, paint);

        // Two segmented orbital arcs, rotating in opposite visual directions.
        paint.setStrokeWidth(dp(1.4f));
        paint.setColor(0xB322C9CB);
        arc.set(cx-r-dp(9), cy-r-dp(9), cx+r+dp(9), cy+r+dp(9));
        for (int i=0;i<4;i++) c.drawArc(arc, orbit*360f+i*90f, 42f, false, paint);
        paint.setColor(0x66FFFFFF);
        arc.inset(-dp(7), -dp(7));
        for (int i=0;i<3;i++) c.drawArc(arc, -orbit*240f+i*120f, 28f, false, paint);

        // Data particles travelling along a short beam into the core.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x5522C9CB);
        c.drawRect(cx-r-dp(38), cy-dp(.5f), cx-dp(18), cy+dp(.5f), paint);
        for (int i=0;i<4;i++) {
            float t=(orbit+i*.24f)%1f;
            float x=(cx-r-dp(35))+(r+dp(17))*t;
            paint.setAlpha(90+(int)(150*t));
            c.drawRect(x,cy-dp(2),x+dp(3),cy+dp(1),paint);
        }
        paint.setAlpha(255);

        // CS core: a crisp diamond/cube mark.
        float d=dp(16);
        diamond.reset(); diamond.moveTo(cx,cy-d); diamond.lineTo(cx+d,cy);
        diamond.lineTo(cx,cy+d); diamond.lineTo(cx-d,cy); diamond.close();
        paint.setColor(0xFF11171A); c.drawPath(diamond,paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); paint.setColor(cyan); c.drawPath(diamond,paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE); paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); paint.setTextSize(dp(9));
        c.drawText("CS",cx,cy+dp(3),paint);

        paint.setColor(Color.WHITE); paint.setTextSize(dp(21));
        c.drawText(Math.round(displayedProgress)+"%",cx,cy+r+dp(38),paint);
        paint.setColor(cyan); paint.setTextSize(dp(9)); paint.setLetterSpacing(.08f);
        c.drawText(stage,cx,cy+r+dp(57),paint); paint.setLetterSpacing(0);
        paint.setColor(0xFF9A9CA5); paint.setTypeface(android.graphics.Typeface.DEFAULT); paint.setTextSize(dp(9));
        c.drawText(ellipsize(detail, 48),cx,cy+r+dp(73),paint);

        // Five-stage timeline.
        float left=dp(25), right=w-dp(25), y=h-dp(35), gap=(right-left)/4f;
        paint.setStrokeWidth(dp(2)); paint.setColor(0xFF303239);
        c.drawLine(left,y,right,y,paint);
        int active = displayedProgress < 18 ? 0 : displayedProgress < 45 ? 1 : displayedProgress < 58 ? 2 : displayedProgress < 90 ? 3 : 4;
        paint.setColor(cyan); c.drawLine(left,y,left+gap*active,y,paint);
        for(int i=0;i<5;i++){
            paint.setColor(i<=active?cyan:0xFF3C3E46); c.drawCircle(left+gap*i,y,dp(i==active?5:4),paint);
            paint.setTextSize(dp(7)); paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(i<=active?0xFFE8FFFF:0xFF74767F); c.drawText(steps[i],left+gap*i,y+dp(17),paint);
        }
    }

    private String ellipsize(String s,int max){return s.length()<=max?s:s.substring(0,max-1)+"…";}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
}
