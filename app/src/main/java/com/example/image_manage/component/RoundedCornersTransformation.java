package com.example.image_manage.component;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import com.squareup.picasso.Transformation;

public class RoundedCornersTransformation implements Transformation {
    private final int radius;
    private final int margin;

    // radius is the corner radius, margin is the distance from the side of the image
    public RoundedCornersTransformation(int radius, int margin) {
        this.radius = radius;
        this.margin = margin;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(margin, margin, source.getWidth() - margin, source.getHeight() - margin);
        final RectF rectF = new RectF(margin, margin, source.getWidth() - margin, source.getHeight() - margin);
        final float roundPx = radius;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);

        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, rect, rect, paint);

        source.recycle();

        return output;
    }

    @Override
    public String key() {
        return "roundedCornersTransformation(radius=" + radius + ", margin=" + margin + ")";
    }
}