package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Adaptive contrast for Home's text over any wallpaper. Samples the picture's brightness in the
 * background (a small RGB_565 decode) and draws two subtle gradients only where it is bright: at the
 * top, behind the clock, greeting and pills, and at the bottom, behind the dock row and its title.
 * A dark wallpaper draws nothing: the picture keeps the whole screen. No fixed black layer. Results
 * post in order: the sampler is a single-threaded executor, so the latest picture wins on its own.
 */
final class AdaptiveScrim extends View {

    /** The sampling band, as a share of the picture's height: the top third (the header), the 42% above the bottom edge (the dock). */
    private static final float TOP_BAND = 0.32f, BOTTOM_BAND = 0.42f;
    /** The share of the header's width the clock, the date and the greeting sit on. */
    private static final float LEFT_SHARE = 0.62f;
    /** Luminance where support starts (0.40, just under mid-grey) and peaks (0.85, really bright); how far at the peak. */
    private static final float RAMP_FROM = 0.40f, RAMP_TO = 0.85f, TOP_ALPHA = 0.68f, BOTTOM_ALPHA = 0.56f;

    private final Paint top = new Paint(Paint.ANTI_ALIAS_FLAG), bottom = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ExecutorService sampler = Executors.newSingleThreadExecutor();
    /** A theme's wallpaper_dim darkens the picture after the decode; the raw sample scales back to what shows. */
    private float dimFactor = 1f;
    /** Last sampled band luminances; kept for the rebuild a size change asks for. */
    private float lastHead = -1f, lastTail;

    AdaptiveScrim(Context context) {
        this(context, null);
    }

    /**
     * The XML constructor. The view is declared in activity_main.xml, so the inflater looks for
     * (Context, AttributeSet): without it every launch of Home dies with an InflateException.
     */
    AdaptiveScrim(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        setFocusable(false);
    }

    /** config wallpaper_dim: the picture is darkened after the decode, so the raw sample is scaled back. */
    void setDim(int wallpaperDim) {
        dimFactor = (100f - wallpaperDim) / 100f;
        if (lastHead >= 0f) rebuild();
    }

    /**
     * The wallpaper changed (start, rotation, an on-TV pick), null when it is gone. Reads its top and
     * bottom bands in the background.
     */
    void onWallpaperShown(File file) {
        if (file == null) {
            lastHead = -1f;
            post(() -> { top.setShader(null); bottom.setShader(null); invalidate(); });
            return;
        }
        try {
            sampler.execute(() -> {
                float[] bands = sample(file);
                if (bands == null) return;
                post(() -> { lastHead = bands[0]; lastTail = bands[1]; rebuild(); invalidate(); });
            });
        } catch (RejectedExecutionException gone) {
            // The view was detached and its sampler shut down: no gradient, the header keeps as it is.
        }
    }

    private void rebuild() {
        float headA = ramp(lastHead * dimFactor) * TOP_ALPHA, tailA = ramp(lastTail * dimFactor) * BOTTOM_ALPHA;
        int height = getHeight();
        if ((headA <= 0f && tailA <= 0f) || height <= 0) {
            top.setShader(null);
            bottom.setShader(null);
            invalidate();
            return;
        }
        top.setShader(new LinearGradient(0, 0, 0, Math.round(height * TOP_BAND),
                shade(headA), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        bottom.setShader(new LinearGradient(0, Math.round(height * (1 - BOTTOM_BAND)), 0, height,
                Color.TRANSPARENT, shade(tailA), Shader.TileMode.CLAMP));
        invalidate();
    }

    /** 0 below RAMP_FROM, 1 at RAMP_TO and past it, a straight line in between. */
    private static float ramp(float luma) {
        return Math.max(0f, Math.min(1f, (luma - RAMP_FROM) / (RAMP_TO - RAMP_FROM)));
    }

    /** The scrim colour: near-black with a faint cool cast, so it reads as depth rather than as a shade. */
    private static int shade(float alpha) {
        return Color.argb(Math.round(alpha * 255f), 5, 7, 10);
    }

    /** Mean relative luminance (0-1) of the picture's top and bottom bands; null when it cannot be read. */
    private static float[] sample(File file) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), options);
            int width = options.outWidth, height = options.outHeight;
            if (width <= 0 || height <= 0) return null;
            int sample = 1;
            while (width / sample > 128) sample *= 2;
            options.inJustDecodeBounds = false;
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
            if (bitmap == null) return null;
            int bw = bitmap.getWidth(), bh = bitmap.getHeight();
            int topRows = Math.max(1, Math.round(bh * TOP_BAND)), bottomRows = Math.max(1, Math.round(bh * BOTTOM_BAND));
            // The header's ink (clock, date, greeting) sits on the left. Averaging the whole width let a
            // bright right half (a character, a window) wash the left's need for contrast away, so the top
            // band is sampled on the left alone. The pills on the right carry their own dark glass.
            int topCols = Math.max(1, Math.round(bw * LEFT_SHARE));
            float topSum = 0f, bottomSum = 0f;
            for (int y = 0; y < bh; y++) {
                boolean isTop = y < topRows, isBottom = y >= bh - bottomRows;
                if (!isTop && !isBottom) continue;
                float rowTop = 0f, rowBottom = 0f;
                for (int x = 0; x < bw; x++) {
                    if (isTop && x >= topCols) continue;   // the header's ink is on the left
                    int pixel = bitmap.getPixel(x, y);
                    float luma = (Color.red(pixel) * 0.299f + Color.green(pixel) * 0.587f
                            + Color.blue(pixel) * 0.114f) / 255f;
                    if (isTop) rowTop += luma;
                    else rowBottom += luma;
                }
                if (isTop) topSum += rowTop;
                else bottomSum += rowBottom;
            }
            bitmap.recycle();
            return new float[]{topSum / (topRows * topCols), bottomSum / (bottomRows * bw)};
        } catch (Throwable failure) {
            return null;   // unreadable picture: no gradient, the header keeps as it is
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (lastHead >= 0f) rebuild();   // the sample arrived before the first layout
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int height = getHeight();
        if (top.getShader() != null) canvas.drawRect(0, 0, getWidth(), Math.round(height * TOP_BAND), top);
        if (bottom.getShader() != null) canvas.drawRect(0, Math.round(height * (1 - BOTTOM_BAND)), getWidth(), height, bottom);
    }

    @Override
    protected void onDetachedFromWindow() {
        sampler.shutdownNow();
        super.onDetachedFromWindow();
    }
}
