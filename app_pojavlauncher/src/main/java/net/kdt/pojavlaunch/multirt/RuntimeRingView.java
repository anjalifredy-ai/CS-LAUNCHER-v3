package net.kdt.pojavlaunch.multirt;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Aggregate install ring for the runtime setup deck: a thin glowing amber arc
 * that sweeps with the overall installation progress. Distinct from the normal
 * downloader visuals on purpose (system-install design language).
 */
public class RuntimeRingView extends View {

    private final Paint mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mArcRect = new RectF();
    private float mProgress = 0f;      // 0..1 displayed (animated)
    private float mTargetProgress = 0f;
    private ValueAnimator mAnimator;
    private float mPadding;

    public RuntimeRingView(Context context) { super(context); init(); }
    public RuntimeRingView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public RuntimeRingView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(); }

    private void init() {
        float d = getResources().getDisplayMetrics().density;
        mPadding = 10 * d;
        mTrackPaint.setStyle(Paint.Style.STROKE);
        mTrackPaint.setStrokeWidth(5f * d);
        mTrackPaint.setStrokeCap(Paint.Cap.ROUND);
        mTrackPaint.setColor(0xFF1B1B22);

        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(5f * d);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mArcPaint.setColor(0xFFD8C79A);

        mGlowPaint.setStyle(Paint.Style.STROKE);
        mGlowPaint.setStrokeWidth(11f * d);
        mGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        mGlowPaint.setColor(0x26D8C79A);
    }

    /** Animate the ring toward the new 0..1 progress. */
    public void setProgress(float target) {
        target = Math.max(0f, Math.min(1f, target));
        if (Math.abs(target - mTargetProgress) < 0.001f) return;
        mTargetProgress = target;
        if (mAnimator != null) mAnimator.cancel();
        mAnimator = ValueAnimator.ofFloat(mProgress, target);
        mAnimator.setDuration(300);
        mAnimator.setInterpolator(new DecelerateInterpolator());
        mAnimator.addUpdateListener(a -> {
            mProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        mAnimator.start();
    }

    public float getProgress() { return mTargetProgress; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w=getWidth(),h=getHeight(),d=getResources().getDisplayMetrics().density;
        Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);fill.setStyle(Paint.Style.FILL);Paint line=new Paint(Paint.ANTI_ALIAS_FLAG);line.setStyle(Paint.Style.STROKE);line.setStrokeWidth(1.2f*d);
        int blocks=6;float bw=w*.48f,bh=Math.max(8*d,h*.085f),gap=4*d,left=(w-bw)/2f,bottom=h-mPadding;
        int complete=(int)Math.floor(mProgress*blocks+0.001f);
        for(int i=0;i<blocks;i++){float y=bottom-(i+1)*bh-i*gap;RectF r=new RectF(left,y,left+bw,y+bh);fill.setColor(i<complete?0xffd8c79a:0xff202832);line.setColor(i<complete?0xfff0e2b8:0xff56616d);canvas.drawRoundRect(r,3*d,3*d,fill);canvas.drawRoundRect(r,3*d,3*d,line);}
        float scanY=bottom-(h-mPadding*2)*mProgress;fill.setColor(0x99ffffff);canvas.drawRoundRect(left-8*d,scanY,left+bw+8*d,scanY+2*d,1*d,1*d,fill);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAnimator != null) mAnimator.cancel();
        super.onDetachedFromWindow();
    }
}
