package net.kdt.pojavlaunch.csclient;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.Nullable;
import net.kdt.pojavlaunch.R;

/** V3.1 — Completely new CS Client installer — dark white black, circular progress, premium shimmer */
public final class CsClientInstallView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ringRect = new RectF();
    private final Bitmap logo;
    private ValueAnimator motion;
    private float phase, shown, target;
    private String status = "PREPARING CLIENT V3.1";
    private String detail = "Atomic profile installation — dark white black";
    private boolean done, failed;
    private final String[] stages = {"LOADER","PACK","CS CLIENT","MODS","FINALIZE"};

    public CsClientInstallView(Context c){this(c,null);}
    public CsClientInstallView(Context c,@Nullable AttributeSet a){this(c,a,0);}
    public CsClientInstallView(Context c,@Nullable AttributeSet a,int s){
        super(c,a,s);
        logo = BitmapFactory.decodeResource(getResources(), R.drawable.cs_logo);
        init();
    }
    private void init(){
        setLayerType(LAYER_TYPE_HARDWARE,null);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setStrokeWidth(dp(6));
        motion = ValueAnimator.ofFloat(0,1);
        motion.setDuration(2200);
        motion.setRepeatCount(ValueAnimator.INFINITE);
        motion.setInterpolator(new LinearInterpolator());
        motion.addUpdateListener(a->{
            phase = (float)a.getAnimatedValue();
            float d = target - shown;
            shown = Math.abs(d) < 0.08f ? target : shown + d*0.12f;
            invalidate();
        });
    }
    public void start(){ if(!motion.isStarted()) motion.start(); }
    public void stop(){ motion.cancel(); }
    public void setInstallProgress(int pct,String text){ target = Math.max(0, Math.min(100,pct)); detail = text==null?"":text; status = stageFor(pct); invalidate(); }
    public void complete(String text){ target=100; done=true; failed=false; status="PROFILE READY — V3.1"; detail=text; invalidate(); }
    public void fail(String text){ failed=true; done=false; status="INSTALLATION STOPPED"; detail=text; invalidate(); }
    private String stageFor(int n){
        if(n<18) return "RESOLVING FABRIC LOADER";
        if(n<45) return "MERGING CLIENT PACK";
        if(n<58) return "INSTALLING CS CLIENT V3.1";
        if(n<90) return "INSTALLING VERIFIED MODULES";
        return "FINALIZING PROFILE";
    }
    @Override protected void onAttachedToWindow(){ super.onAttachedToWindow(); start(); }
    @Override protected void onDetachedFromWindow(){ stop(); super.onDetachedFromWindow(); }
    @Override protected void onDraw(Canvas c){
        float w=getWidth(), h=getHeight(), cx=w*0.5f, cy=h*0.42f;
        // Background — pure dark #0B0D11
        c.drawColor(0xFF0B0D11);
        // Subtle white radial glow top
        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setShader(new RadialGradient(w*0.5f, -h*0.2f, w*0.8f, new int[]{0x14FFFFFF, 0x05FFFFFF, 0x00000000}, new float[]{0f,0.5f,1f}, Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,glow);
        // Fine grid
        paint.setColor(0x0AFFFFFF);
        paint.setStrokeWidth(dp(0.6f));
        for(int i=1;i<8;i++) c.drawLine(w*i/8f,0,w*i/8f,h,paint);
        for(int i=1;i<5;i++) c.drawLine(0,h*i/5f,w,h*i/5f,paint);
        // Circular progress ring — white
        float radius = Math.min(w,h)*0.18f;
        ringRect.set(cx-radius, cy-radius, cx+radius, cy+radius);
        // Track
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(6));
        paint.setColor(0xFF1A1D24);
        paint.setStrokeCap(Paint.Cap.ROUND);
        c.drawArc(ringRect, -90, 360, false, paint);
        // Progress white
        ringPaint.setColor(done ? 0xFFFFFFFF : failed ? 0xFFFF6B6B : 0xFFFFFFFF);
        float sweep = 360f * shown / 100f;
        // Add shimmer rotation
        float start = -90 + phase*12f;
        c.drawArc(ringRect, start, sweep, false, ringPaint);
        // Inner halo
        Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
        halo.setStyle(Paint.Style.STROKE);
        halo.setStrokeWidth(dp(10));
        halo.setColor(0x0DFFFFFF);
        c.drawArc(ringRect, -90, sweep, false, halo);
        // Logo
        if(logo!=null){
            float logoSize = radius*0.9f;
            RectF r = new RectF(cx-logoSize, cy-logoSize, cx+logoSize, cy+logoSize);
            paint.setAlpha(done?255:220);
            c.drawBitmap(logo, null, r, paint);
            paint.setAlpha(255);
        }
        // Pulsing outer ring
        float pulse = (float)(Math.sin(phase*Math.PI*2)*0.5+0.5);
        Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(dp(1));
        pulsePaint.setColor(0x1AFFFFFF);
        pulsePaint.setAlpha((int)(30+40*pulse));
        c.drawCircle(cx, cy, radius+dp(12)+pulse*dp(4), pulsePaint);
        // Texts — white
        drawText(c, "CS CLIENT", cx, cy+radius+dp(36), dp(22), 0xFFFFFFFF, true, Paint.Align.CENTER);
        drawText(c, "V3.1 • " + status, cx, cy+radius+dp(58), dp(10), done?0xFFFFFFFF:(failed?0xFFFF6B6B:0xFFB8BCC8), true, Paint.Align.CENTER);
        drawText(c, trim(detail,68), cx, cy+radius+dp(74), dp(8), 0xFF7F8894, false, Paint.Align.CENTER);
        // Bottom rail — white pill with shimmer
        float left=w*0.18f, right=w*0.82f, y=h*0.78f;
        Paint railBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        railBg.setColor(0xFF1A1D24);
        railBg.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new RectF(left, y, right, y+dp(7)), dp(3), dp(3), railBg);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(done?0xFFFFFFFF:0xFFFFFFFF);
        float fillW = left + (right-left)*shown/100f;
        c.drawRoundRect(new RectF(left, y, Math.max(left,fillW), y+dp(7)), dp(3), dp(3), fill);
        if(!done && !failed){
            float travel = (phase*1.4f)%1f;
            float sx = left + (right-left)*travel;
            Paint sh = new Paint(Paint.ANTI_ALIAS_FLAG);
            sh.setShader(new LinearGradient(sx-dp(40),0,sx+dp(20),0, new int[]{0x00FFFFFF,0xAAFFFFFF,0x00FFFFFF}, new float[]{0f,0.5f,1f}, Shader.TileMode.CLAMP));
            c.drawRect(Math.max(left,sx-dp(42)), y, Math.min(right,sx+dp(22)), y+dp(7), sh);
        }
        drawText(c, Math.round(shown)+"%", right+dp(14), y+dp(6), dp(10), 0xFFFFFFFF, true, Paint.Align.LEFT);
        // Stages dots — white/black
        int active = shown<18?0:shown<45?1:shown<58?2:shown<90?3:4;
        float gap=(right-left)/4f;
        for(int i=0;i<5;i++){
            float x=left+gap*i;
            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            boolean isActive = i==active;
            boolean isDone = i<active || done;
            dot.setColor(isDone?0xFFFFFFFF:(isActive?0xFFFFFFFF:0xFF3A404C));
            c.drawCircle(x, y+dp(3.5f), dp(isActive?4:2.5f), dot);
            if(isActive){
                Paint glow2 = new Paint(Paint.ANTI_ALIAS_FLAG);
                glow2.setColor(0x33FFFFFF);
                c.drawCircle(x, y+dp(3.5f), dp(8), glow2);
            }
            drawText(c, stages[i], x, y+dp(26), dp(7), isDone||isActive?0xFFB8BCC8:0xFF5A636E, true, Paint.Align.CENTER);
        }
        drawText(c, failed?"ROLLBACK COMPLETE":(done?"VERIFIED • READY TO LAUNCH — V3.1":"VERIFIED FILES • ATOMIC INSTALL • V3.1"), cx, h-dp(18), dp(7), done?0xFFFFFFFF:0xFF5A636E, true, Paint.Align.CENTER);
    }
    private void drawText(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){
        paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size); paint.setTextAlign(align); paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT); c.drawText(s,x,y,paint);
    }
    private String trim(String s,int n){ return s==null?"":s.length()<=n?s:s.substring(0,n-1)+"…"; }
    private float dp(float n){ return n*getResources().getDisplayMetrics().density; }
}
