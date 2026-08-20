package net.kdt.pojavlaunch.csclient;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.R;

/** Landscape cinematic CS Client installer; real progress drives its rail and stages. */
public final class CsClientInstallView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();private final RectF rect=new RectF();
    private final Bitmap logo;private ValueAnimator motion;private float phase,shown,target;private String status="PREPARING CLIENT";private String detail="Atomic profile installation";private boolean done,failed;
    private final String[] stages={"LOADER","PACK","CS CLIENT","MODS","FINALIZE"};

    public CsClientInstallView(Context c){this(c,null);}public CsClientInstallView(Context c,@Nullable AttributeSet a){this(c,a,0);}public CsClientInstallView(Context c,@Nullable AttributeSet a,int s){super(c,a,s);logo=BitmapFactory.decodeResource(getResources(),R.drawable.cs_logo);init();}
    private void init(){setLayerType(LAYER_TYPE_HARDWARE,null);motion=ValueAnimator.ofFloat(0,1);motion.setDuration(2600);motion.setRepeatCount(ValueAnimator.INFINITE);motion.setInterpolator(new LinearInterpolator());motion.addUpdateListener(a->{phase=(float)a.getAnimatedValue();float d=target-shown;shown=Math.abs(d)<.08f?target:shown+d*.1f;invalidate();});}
    public void start(){if(!motion.isStarted())motion.start();}public void stop(){motion.cancel();}
    public void setInstallProgress(int pct,String text){target=Math.max(0,Math.min(100,pct));detail=text==null?"":text;status=stageFor(pct);invalidate();}
    public void complete(String text){target=100;done=true;failed=false;status="PROFILE READY";detail=text;invalidate();}
    public void fail(String text){failed=true;done=false;status="INSTALLATION STOPPED";detail=text;invalidate();}
    private String stageFor(int n){if(n<18)return"RESOLVING FABRIC LOADER";if(n<45)return"MERGING CLIENT PACK";if(n<58)return"INSTALLING CS CLIENT";if(n<90)return"INSTALLING VERIFIED MODULES";return"FINALIZING PROFILE";}

    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();start();}@Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),cx=w*.5f,cy=h*.42f;int silver=0xffbcc3cc,white=0xfff2f3f5,whiteDone=0xffffffff;
        // Faceted cinematic background with restrained parallax.
        c.drawColor(0xff090d11);p.setStyle(Paint.Style.FILL);float shift=(phase-.5f)*dp(18);
        facet(c,new float[]{0,0,w*.34f+shift,0,w*.57f,h,0,h},0xff0d1319);facet(c,new float[]{w*.29f,0,w*.68f,0,w*.53f,h,w*.13f,h},0xff10161d);facet(c,new float[]{w*.68f-shift,0,w,0,w,h,w*.47f,h},0xff0c1117);
        p.setShader(new LinearGradient(0,0,w,0,0x00131920,0x55131920,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        // Fine technical grid/rails.
        p.setStrokeWidth(dp(.65f));p.setColor(0x182f3944);for(int i=1;i<10;i++)c.drawLine(w*i/10f,0,w*i/10f,h,p);for(int i=1;i<6;i++)c.drawLine(0,h*i/6f,w,h*i/6f,p);
        // Logo and lock-on brackets.
        float logoSize=Math.min(w,h)*.145f;if(logo!=null){rect.set(cx-logoSize/2,cy-logoSize/2,cx+logoSize/2,cy+logoSize/2);p.setAlpha(done?255:225);c.drawBitmap(logo,null,rect,p);p.setAlpha(255);}float lock=logoSize*.8f+dp(20)*(1-(float)Math.sin(phase*Math.PI)*.14f);drawBrackets(c,cx,cy,lock,silver);
        // Incoming data fragments.
        p.setColor(0x99bcc3cc);for(int i=0;i<7;i++){float t=(phase+i*.137f)%1f;float side=i%2==0?-1:1;float x=cx+side*(logoSize*.8f+(1-t)*w*.18f);float y=cy+(i-3)*dp(7)*(1-t);float sz=dp(1.5f+2*t);p.setAlpha((int)(35+160*t));c.drawRect(x,y,x+sz,y+sz,p);}p.setAlpha(255);
        text(c,"CS CLIENT",cx,cy+logoSize*.76f+dp(34),dp(24),white,true,Paint.Align.CENTER);
        text(c,status,cx,cy+logoSize*.76f+dp(57),dp(8.5f),done?whiteDone:(failed?0xffff7e86:silver),true,Paint.Align.CENTER);
        text(c,trim(detail,66),cx,cy+logoSize*.76f+dp(73),dp(7.5f),0xff7f8994,false,Paint.Align.CENTER);
        // Thin exclusive progress rail.
        float left=w*.23f,right=w*.77f,y=h*.76f;p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.SQUARE);p.setStrokeWidth(dp(1));p.setColor(0xff59636e);c.drawRect(left,y,right,y+dp(7),p);p.setStyle(Paint.Style.FILL);float fill=left+(right-left)*shown/100f;p.setColor(done?whiteDone:white);c.drawRect(left+dp(2),y+dp(2),Math.max(left+dp(2),fill),y+dp(5),p);
        if(!done&&!failed){float travel=(phase*1.22f)%1f;float sx=left+(right-left)*travel;p.setShader(new LinearGradient(sx-dp(32),0,sx+dp(16),0,0x00ffffff,0xffffffff,Shader.TileMode.CLAMP));c.drawRect(Math.max(left,sx-dp(34)),y+dp(1),Math.min(right,sx+dp(18)),y+dp(6),p);p.setShader(null);}
        text(c,Math.round(shown)+"%",right+dp(18),y+dp(7),dp(9),white,true,Paint.Align.LEFT);
        int active=shown<18?0:shown<45?1:shown<58?2:shown<90?3:4;float gap=(right-left)/4f;for(int i=0;i<5;i++){float x=left+gap*i;p.setColor(i<active||done?whiteDone:(i==active?white:0xff46505a));c.drawCircle(x,y+dp(3.5f),dp(i==active?3:2),p);text(c,stages[i],x,y+dp(24),dp(6.5f),i<=active?silver:0xff59616b,true,Paint.Align.CENTER);}
        text(c,failed?"ROLLBACK COMPLETE":(done?"VERIFIED  •  READY TO LAUNCH":"VERIFIED FILES  •  ATOMIC INSTALL  •  ROLLBACK SAFE"),cx,h-dp(22),dp(7),done?whiteDone:0xff717b86,true,Paint.Align.CENTER);
    }
    private void facet(Canvas c,float[]v,int color){path.reset();path.moveTo(v[0],v[1]);for(int i=2;i<v.length;i+=2)path.lineTo(v[i],v[i+1]);path.close();p.setColor(color);c.drawPath(path,p);}
    private void drawBrackets(Canvas c,float x,float y,float r,int color){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(color);float l=dp(17);for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1}){float bx=x+sx*r,by=y+sy*r;c.drawLine(bx,by,bx-sx*l,by,p);c.drawLine(bx,by,bx,by-sy*l,p);}p.setStyle(Paint.Style.FILL);}
    private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTextAlign(align);p.setTypeface(bold?android.graphics.Typeface.DEFAULT_BOLD:android.graphics.Typeface.DEFAULT);c.drawText(s,x,y,p);}
    private String trim(String s,int n){return s==null?"":s.length()<=n?s:s.substring(0,n-1)+"…";}private float dp(float n){return n*getResources().getDisplayMetrics().density;}
}
