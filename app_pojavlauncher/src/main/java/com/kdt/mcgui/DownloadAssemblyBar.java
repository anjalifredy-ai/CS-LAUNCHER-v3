package com.kdt.mcgui;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;

/**
 * DARK • WHITE • BLACK — Premium top-beam.
 * Track #111318 (near black) + fill pure white → soft grey gradient.
 * No blue/teal. Halo white, shimmer white. Matches dark theme.
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
    private float shimmerPhase;
    private int state = 0; //0 normal,1 success,2 error,3 paused

    public DownloadAssemblyBar(Context c){this(c,null);}
    public DownloadAssemblyBar(Context c,@Nullable AttributeSet a){
        super(c,a);
        setWillNotDraw(false);
        setProgressDrawable(null);
        trackPaint.setColor(0xFF0F1014);
        trackPaint.setStyle(Paint.Style.FILL);
        shimmerAnim = ValueAnimator.ofFloat(0f,1f);
        shimmerAnim.setDuration(1700);
        shimmerAnim.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnim.setInterpolator(new LinearInterpolator());
        shimmerAnim.addUpdateListener(v->{ shimmerPhase=(float)v.getAnimatedValue(); if(getProgress()>2 && getProgress()<100 && state==0) invalidate(); });
    }
    public void setBarState(int s){ state=s; invalidate(); }
    @Override protected void onAttachedToWindow(){ super.onAttachedToWindow(); if(!shimmerAnim.isStarted()) shimmerAnim.start(); }
    @Override protected void onDetachedFromWindow(){ shimmerAnim.cancel(); super.onDetachedFromWindow(); }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){ super.onSizeChanged(w,h,oldw,oldh); trackRect.set(0,0,w,h); }
    @Override protected synchronized void onDraw(Canvas canvas){
        int w=getWidth(), h=getHeight();
        if(w<=0||h<=0) return;
        float radius=h/2f;
        float done=Math.max(0f,Math.min(1f,getProgress()/100f));
        float fillW=w*done;
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);
        // subtle inner line
        Paint line=new Paint(Paint.ANTI_ALIAS_FLAG); line.setColor(0x0AFFFFFF);
        canvas.drawRoundRect(new RectF(0,0,w,h), radius, radius, line);
        if(done<=0.001f) return;
        // halo
        haloPaint.setColor(state==2?0x22FF5252: state==1?0x14FFFFFF : 0x12FFFFFF);
        canvas.drawRoundRect(new RectF(-2, -2, fillW+2, h+2), radius+2, radius+2, haloPaint);
        // fill: white luminous
        int cStart, cMid, cEnd;
        if(state==2){ cStart=0xFFFF8A8A; cMid=0xFFFF6B6B; cEnd=0xFFFF5252; }
        else if(state==1){ cStart=0xFFFFFFFF; cMid=0xFFF1F1F3; cEnd=0xFFE8E8EA; }
        else if(state==3){ cStart=0xFFD8C79A; cMid=0xFFE8D5B5; cEnd=0xFFF5E8D0; }
        else { cStart=0xFFFFFFFF; cMid=0xFFF2F2F5; cEnd=0xFFE8E8EC; }
        fillRect.set(0, 0.8f, fillW, h-0.8f);
        LinearGradient lg=new LinearGradient(0,0,fillW,0, new int[]{cStart,cMid,cEnd}, new float[]{0f,0.55f,1f}, Shader.TileMode.CLAMP);
        fillPaint.setShader(lg); fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(fillRect, radius-0.8f, radius-0.8f, fillPaint);
        fillPaint.setShader(null);
        // specular top highlight
        Paint spec=new Paint(Paint.ANTI_ALIAS_FLAG); spec.setColor(0x66FFFFFF);
        canvas.drawRoundRect(new RectF(1,0.8f,fillW-1,h*0.38f), radius*0.6f, radius*0.6f, spec);
        // shimmer
        if(state==0 && done>0.04f && done<0.99f){
            canvas.save();
            clipRect.set(0,0,fillW,h);
            canvas.clipRect(clipRect);
            float sweepW=54*getResources().getDisplayMetrics().density;
            float x=-sweepW + (w + sweepW*2)*shimmerPhase;
            if(x>-sweepW && x<fillW+sweepW){
                LinearGradient sg=new LinearGradient(x,0,x+sweepW,0, new int[]{0x00FFFFFF,0xCCFFFFFF,0x00FFFFFF}, new float[]{0f,0.5f,1f}, Shader.TileMode.CLAMP);
                shimmerPaint.setShader(sg); shimmerPaint.setStyle(Paint.Style.FILL);
                canvas.drawRect(x,0,x+sweepW,h, shimmerPaint);
                shimmerPaint.setShader(null);
            }
            canvas.restore();
        }
        // leading glow
        if(done>0.01f && done<0.99f){
            leadPaint.setColor(0xDDFFFFFF);
            float lx=fillW-1.2f*getResources().getDisplayMetrics().density;
            canvas.drawRoundRect(new RectF(lx,1, lx+2.4f*getResources().getDisplayMetrics().density, h-1), 2,2, leadPaint);
        }
    }
    private float dp(float n){ return n*getResources().getDisplayMetrics().density; }
}
