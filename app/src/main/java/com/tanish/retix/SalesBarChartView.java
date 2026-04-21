package com.tanish.retix;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Simple custom bar chart view for displaying sales trend data.
 * No external library required.
 */
public class SalesBarChartView extends View {

    // Dummy monthly sales data (Jan–Jun)
    private static final String[] LABELS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
    private static final float[] VALUES = {3200f, 5500f, 4100f, 7800f, 6200f, 9500f};

    private Paint barPaint;
    private Paint labelPaint;
    private Paint valuePaint;
    private Paint linePaint;
    private Paint axisPaint;

    private int barColor    = 0xFF3A86FF; // accent_blue
    private int barColorDim = 0xFFB3CFFF;
    private int textColor   = 0xFF5A6B7B;
    private int axisColor   = 0xFFE2E8F0;

    public SalesBarChartView(Context context) {
        super(context);
        init();
    }

    public SalesBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SalesBarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(barColor);
        barPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(textColor);
        labelPaint.setTextSize(28f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(barColor);
        valuePaint.setTextSize(26f);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(axisColor);
        axisPaint.setStrokeWidth(2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width  = getWidth();
        int height = getHeight();

        float paddingLeft   = 20f;
        float paddingRight  = 20f;
        float paddingTop    = 40f;
        float paddingBottom = 50f;

        float chartWidth  = width  - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop  - paddingBottom;

        // Draw baseline
        canvas.drawLine(paddingLeft, height - paddingBottom,
                width - paddingRight, height - paddingBottom, axisPaint);

        float maxValue = 0;
        for (float v : VALUES) if (v > maxValue) maxValue = v;

        int count = VALUES.length;
        float totalBarWidth = chartWidth / count;
        float barWidth = totalBarWidth * 0.55f;
        float gap = (totalBarWidth - barWidth) / 2f;

        for (int i = 0; i < count; i++) {
            float barHeight = (VALUES[i] / maxValue) * chartHeight;
            float left   = paddingLeft + i * totalBarWidth + gap;
            float right  = left + barWidth;
            float top    = height - paddingBottom - barHeight;
            float bottom = height - paddingBottom;

            // Highlight the highest bar
            barPaint.setColor(VALUES[i] == maxValue ? barColor : barColorDim);

            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, 8f, 8f, barPaint);

            // Label below bar
            canvas.drawText(LABELS[i], left + barWidth / 2f,
                    height - paddingBottom + 36f, labelPaint);

            // Value above bar
            String valStr = "₹" + (int)(VALUES[i] / 1000) + "k";
            canvas.drawText(valStr, left + barWidth / 2f, top - 8f, valuePaint);
        }
    }
}
