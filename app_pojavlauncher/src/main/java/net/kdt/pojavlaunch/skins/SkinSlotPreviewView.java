package net.kdt.pojavlaunch.skins;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.File;

/** Lightweight full-body skin thumbnail: one Canvas, no extra GL context per slot. */
public class SkinSlotPreviewView extends View {
    private final Paint paint = new Paint(); // nearest-neighbour keeps Minecraft pixels crisp
    private final Paint shade = new Paint();
    private Bitmap skin;

    public SkinSlotPreviewView(Context context) { super(context); init(); }
    public SkinSlotPreviewView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    private void init() { shade.setColor(0x28000000); setLayerType(LAYER_TYPE_SOFTWARE, null); }

    public void setSkin(@Nullable File file) {
        if (skin != null && !skin.isRecycled()) skin.recycle();
        skin = file != null && file.isFile() ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (skin == null || skin.isRecycled()) {
            paint.setColor(0xFF5D606A); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2f);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            canvas.drawCircle(cx, cy - 10, 11, paint);
            canvas.drawRoundRect(new RectF(cx - 15, cy + 4, cx + 15, cy + 39), 6, 6, paint);
            paint.setStyle(Paint.Style.FILL); return;
        }
        float unit = Math.min(getWidth() / 14f, getHeight() / 35f);
        float ox = (getWidth() - unit * 12f) / 2f;
        float oy = (getHeight() - unit * 32f) / 2f;
        // soft depth shadow
        canvas.drawRoundRect(new RectF(ox + unit * 2.4f, oy + unit * 1.2f,
                ox + unit * 10.7f, oy + unit * 32.5f), unit, unit, shade);
        // head + hat
        part(canvas, 8, 8, 8, 8, ox + 2*unit, oy, 8*unit, 8*unit);
        part(canvas, 40, 8, 8, 8, ox + 2*unit, oy, 8*unit, 8*unit);
        // torso + jacket
        part(canvas, 20, 20, 8, 12, ox + 2*unit, oy + 8*unit, 8*unit, 12*unit);
        part(canvas, 20, 36, 8, 12, ox + 2*unit, oy + 8*unit, 8*unit, 12*unit);
        // arms
        part(canvas, 44, 20, 4, 12, ox - 2*unit, oy + 8*unit, 4*unit, 12*unit);
        part(canvas, 44, 36, 4, 12, ox - 2*unit, oy + 8*unit, 4*unit, 12*unit);
        part(canvas, 44, 20, 4, 12, ox + 10*unit, oy + 8*unit, 4*unit, 12*unit);
        // legs
        part(canvas, 4, 20, 4, 12, ox + 2*unit, oy + 20*unit, 4*unit, 12*unit);
        part(canvas, 4, 36, 4, 12, ox + 2*unit, oy + 20*unit, 4*unit, 12*unit);
        part(canvas, skin.getHeight() >= skin.getWidth() ? 20 : 4,
                skin.getHeight() >= skin.getWidth() ? 52 : 20, 4, 12,
                ox + 6*unit, oy + 20*unit, 4*unit, 12*unit);
        // subtle right-side depth edges
        Paint edge = shade; edge.setColor(0x32000000);
        canvas.drawRect(ox + 9.55f*unit, oy + .5f*unit, ox + 10*unit, oy + 31.7f*unit, edge);
    }

    private void part(Canvas canvas, int x, int y, int w, int h,
                      float dx, float dy, float dw, float dh) {
        float scale = skin.getWidth() / 64f;
        Rect src = new Rect((int)(x*scale), (int)(y*scale),
                Math.min(skin.getWidth(), (int)((x+w)*scale)),
                Math.min(skin.getHeight(), (int)((y+h)*scale)));
        if (src.width() <= 0 || src.height() <= 0) return;
        paint.setColor(Color.WHITE);
        canvas.drawBitmap(skin, src, new RectF(dx, dy, dx + dw, dy + dh), paint);
    }

    @Override protected void onDetachedFromWindow() {
        if (skin != null && !skin.isRecycled()) skin.recycle();
        skin = null; super.onDetachedFromWindow();
    }
}
