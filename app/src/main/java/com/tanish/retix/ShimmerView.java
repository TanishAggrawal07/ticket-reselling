package com.tanish.retix;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * ShimmerView — a lightweight skeleton loading animation.
 *
 * Supports rounded corners via {@code app:shimmerCornerRadius} and clipping
 * so the shimmer stays within the intended shape (rectangle or circle).
 */
public class ShimmerView extends View {

    private static final int DEFAULT_DURATION = 1400;

    private Paint shimmerPaint;
    private LinearGradient gradient;
    private ValueAnimator animator;
    private float shimmerPosition = -1f;

    private float cornerRadius = 0f;
    private RectF rect = new RectF();
    private Path clipPath = new Path();
    private boolean circleMode = false;

    // Colors — light-mode defaults
    private int baseColor = 0xFFE8ECF1;
    private int highlightColor = 0xFFF5F7FA;

    public ShimmerView(Context context) {
        this(context, null);
    }

    public ShimmerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShimmerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ShimmerView);
            cornerRadius = a.getDimension(R.styleable.ShimmerView_shimmerCornerRadius, 0f);
            circleMode = a.getBoolean(R.styleable.ShimmerView_shimmerCircle, false);
            baseColor = a.getColor(R.styleable.ShimmerView_shimmerBaseColor, baseColor);
            highlightColor = a.getColor(R.styleable.ShimmerView_shimmerHighlightColor, highlightColor);
            a.recycle();
        }

        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        gradient = null; // force rebuild
    }

    private void ensureGradient() {
        if (gradient != null) return;
        int w = getWidth();
        if (w == 0) return;

        int[] colors = { baseColor, highlightColor, baseColor };
        float[] positions = { 0f, 0.5f, 1f };

        gradient = new LinearGradient(
                -w, 0,
                w * 2, 0,
                colors, positions,
                Shader.TileMode.CLAMP
        );
        shimmerPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        ensureGradient();
        if (gradient == null) return;

        int w = getWidth();
        int h = getHeight();

        // Clip to shape
        if (circleMode) {
            float radius = Math.min(w, h) / 2f;
            canvas.drawCircle(w / 2f, h / 2f, radius, shimmerPaint);
        } else if (cornerRadius > 0f) {
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, shimmerPaint);
        } else {
            canvas.drawRect(0, 0, w, h, shimmerPaint);
        }
    }

    // --- Animation lifecycle ---

    public void startShimmer() {
        if (animator != null && animator.isRunning()) return;

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DEFAULT_DURATION);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.addUpdateListener(animation -> {
            shimmerPosition = (float) animation.getAnimatedValue();
            gradient = null; // rebuild with new position baked in
            // Translate approach: shift the paint shader
            float translateX = -getWidth() + (getWidth() * 3 * shimmerPosition);
            shimmerPaint.setShader(null);
            ensureGradientWithOffset(translateX);
            invalidate();
        });
        animator.start();
    }

    private void ensureGradientWithOffset(float offsetX) {
        int w = getWidth();
        if (w == 0) return;

        int[] colors = { baseColor, highlightColor, baseColor };
        float[] positions = { 0f, 0.5f, 1f };

        gradient = new LinearGradient(
                offsetX - w, 0,
                offsetX + w * 2, 0,
                colors, positions,
                Shader.TileMode.CLAMP
        );
        shimmerPaint.setShader(gradient);
    }

    public void stopShimmer() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getVisibility() == VISIBLE) startShimmer();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopShimmer();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            startShimmer();
        } else {
            stopShimmer();
        }
    }
}