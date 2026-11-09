package com.pishug.cat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.View;

/** An original animated ginger cat drawn directly with Canvas. */
public class CatView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float facing = 1f;
    private boolean walking = true;
    private long excitementStarted = 0;
    private long excitementEnds = 0;

    private static final int ORANGE = Color.rgb(222, 120, 47);
    private static final int LIGHT_ORANGE = Color.rgb(246, 170, 84);
    private static final int DARK_ORANGE = Color.rgb(160, 74, 30);
    private static final int CREAM = Color.rgb(255, 229, 186);
    private static final int PINK = Color.rgb(242, 158, 157);
    private static final int INK = Color.rgb(69, 46, 42);

    public CatView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setContentDescription("Рыжий кот Пишуг");
    }

    public void setFacing(float direction) { facing = direction >= 0 ? 1f : -1f; }
    public void setWalking(boolean moving) { walking = moving; }
    public void celebrate() {
        excitementStarted = SystemClock.uptimeMillis();
        excitementEnds = excitementStarted + 950;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long time = SystemClock.uptimeMillis();
        float t = time / 1000f;
        boolean excited = time < excitementEnds;
        float bounce = 0f;
        if (excited) {
            float progress = (time - excitementStarted) / 950f;
            bounce = (float) Math.sin(progress * Math.PI) * 19f;
        }
        float step = walking ? (float) Math.sin(t * 9f) : 0f;
        float tailWave = (float) Math.sin(t * 3.7f) * 13f;
        float blinkPeriod = t % 5.7f;
        boolean blink = blinkPeriod > 5.53f;
        float earWiggle = (float) Math.sin(t * 2.4f) * 2f;

        canvas.save();
        canvas.scale(getWidth() / 180f, getHeight() / 200f);

        // A small shadow anchors Pishug to the screen.
        color(Color.argb(55, 100, 63, 38));
        canvas.drawOval(34, 176, 156, 187, paint);

        canvas.save();
        canvas.translate(0, -bounce - Math.abs(step) * 2.2f);
        if (facing < 0) canvas.scale(-1, 1, 90f, 100f);

        // Curved waving tail behind the body.
        outline(DARK_ORANGE, 20);
        path.reset();
        path.moveTo(129, 134);
        path.cubicTo(162, 133, 171 + tailWave, 109, 153 + tailWave, 94);
        canvas.drawPath(path, paint);
        outline(LIGHT_ORANGE, 11);
        canvas.drawPath(path, paint);

        // Paws and fuzzy orange body.
        color(DARK_ORANGE);
        canvas.drawOval(49 + step * 4, 150, 88 + step * 4, 180, paint);
        canvas.drawOval(108 - step * 4, 150, 148 - step * 4, 180, paint);
        color(ORANGE);
        canvas.drawOval(42, 86, 145, 169, paint);
        color(LIGHT_ORANGE);
        canvas.drawOval(55, 100, 129, 164, paint);
        color(CREAM);
        canvas.drawOval(69, 116, 119, 161, paint);

        // Triangular ears and inner ears.
        triangle(canvas, 43, 68, 47, 17 + earWiggle, 78, 45, ORANGE);
        triangle(canvas, 109, 44, 142, 18 - earWiggle, 144, 69, ORANGE);
        triangle(canvas, 51, 57, 53, 33 + earWiggle, 70, 48, PINK);
        triangle(canvas, 118, 47, 136, 33 - earWiggle, 137, 57, PINK);

        // Head, orange tabby forehead stripes.
        color(ORANGE);
        canvas.drawOval(32, 39, 155, 139, paint);
        color(LIGHT_ORANGE);
        canvas.drawOval(39, 54, 147, 134, paint);
        outline(DARK_ORANGE, 4);
        canvas.drawLine(91, 55, 91, 69, paint);
        canvas.drawLine(79, 57, 84, 69, paint);
        canvas.drawLine(103, 57, 98, 69, paint);
        canvas.drawLine(41, 91, 55, 95, paint);
        canvas.drawLine(143, 91, 130, 95, paint);

        // Sparkly expressive eyes, and periodic blinking.
        if (blink) {
            outline(INK, 4);
            canvas.drawLine(64, 98, 78, 98, paint);
            canvas.drawLine(110, 98, 124, 98, paint);
        } else {
            color(INK);
            canvas.drawOval(66, 90, 79, 108, paint);
            canvas.drawOval(110, 90, 123, 108, paint);
            color(Color.WHITE);
            canvas.drawCircle(73, 95, 2.7f, paint);
            canvas.drawCircle(117, 95, 2.7f, paint);
        }

        // Cream cheeks, a pink nose, smile and whiskers.
        color(CREAM);
        canvas.drawOval(60, 105, 96, 129, paint);
        canvas.drawOval(93, 105, 130, 129, paint);
        triangle(canvas, 88, 111, 102, 111, 95, 119, PINK);
        outline(INK, 2.2f);
        canvas.drawLine(95, 118, 95, 125, paint);
        path.reset();
        path.moveTo(95, 125);
        path.quadTo(85, 137, 79, 125);
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(95, 125);
        path.quadTo(105, 137, 111, 125);
        canvas.drawPath(path, paint);
        outline(INK, 1.8f);
        canvas.drawLine(62, 118, 31, 112, paint);
        canvas.drawLine(62, 124, 30, 128, paint);
        canvas.drawLine(128, 118, 158, 111, paint);
        canvas.drawLine(128, 124, 158, 129, paint);

        if (excited) {
            color(PINK);
            canvas.drawCircle(54, 110, 6, paint);
            canvas.drawCircle(135, 110, 6, paint);
        }
        canvas.restore();
        canvas.restore();

        // Lightweight self-timed Canvas animation; no external animation assets.
        postInvalidateDelayed(45);
    }

    private void color(int c) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(c);
        paint.setStrokeWidth(1);
    }

    private void outline(int c, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(c);
    }

    private void triangle(Canvas canvas, float x1, float y1, float x2, float y2,
                          float x3, float y3, int c) {
        color(c);
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();
        canvas.drawPath(path, paint);
    }
}
