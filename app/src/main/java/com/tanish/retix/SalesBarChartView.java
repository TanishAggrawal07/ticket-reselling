package com.tanish.retix;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * Custom bar chart view for displaying sales trend data.
 * Supports dynamic data via setData().
 */
public class SalesBarChartView extends View {

    // Default data (used as fallback when no API data available)
    private String[] labels = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
    private float[] values = {3200f, 5500f, 4100f, 7800f, 6200f, 9500f};

    private Paint barPaint;
    private Paint labelPaint;
    private Paint valuePaint;
    private Paint axisPaint;

    private int barColor    = 0xFF3A86FF;
    private int barColorDim = 0xFFB3CFFF;
    private int textColor    = 0xFF5A6B7B;
    private int axisColor    = 0xFFE2E8F0;

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
        float density = getResources().getDisplayMetrics().density;

        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(barColor);
        barPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(textColor);
        labelPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 10f, getResources().getDisplayMetrics()));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(barColor);
        valuePaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 9f, getResources().getDisplayMetrics()));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(axisColor);
        axisPaint.setStrokeWidth(2f * density);
    }

    /**
     * Update the chart data dynamically.
     */
    public void setData(String[] labels, float[] values) {
        if (labels == null || values == null || labels.length != values.length || labels.length == 0) {
            return;
        }
        this.labels = labels;
        this.values = values;
        invalidate();
    }

    /**
     * Returns a human-readable date range label from the current data.
     */
    public String getDateRangeLabel() {
        if (labels == null || labels.length == 0) return "No data";
        if (labels.length == 1) return labels[0];
        return labels[0] + " – " + labels[labels.length - 1];
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
        for (float v : values) if (v > maxValue) maxValue = v;
        if (maxValue == 0) maxValue = 1f; // Avoid division by zero

        // Check if all values are zero
        boolean allZero = true;
        for (float v : values) {
            if (v > 0) { allZero = false; break; }
        }
        if (allZero) {
            Paint noDataPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            noDataPaint.setColor(textColor);
            noDataPaint.setTextSize(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 14f, getResources().getDisplayMetrics()));
            noDataPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No sales data yet", width / 2f, height / 2f, noDataPaint);
            return;
        }

        int count = values.length;
        float totalBarWidth = chartWidth / count;
        float barWidth = totalBarWidth * 0.55f;
        float gap = (totalBarWidth - barWidth) / 2f;

        for (int i = 0; i < count; i++) {
            float barHeight = (values[i] / maxValue) * chartHeight;
            float left   = paddingLeft + i * totalBarWidth + gap;
            float right  = left + barWidth;
            float top    = height - paddingBottom - barHeight;
            float bottom = height - paddingBottom;

            // Highlight the highest bar
            barPaint.setColor(values[i] == maxValue ? barColor : barColorDim);

            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, 8f, 8f, barPaint);

            // Label below bar
            canvas.drawText(labels[i], left + barWidth / 2f,
                    height - paddingBottom + 36f, labelPaint);

            // Value above bar
            String valStr;
            if (values[i] >= 1000) {
                valStr = "₹" + (int)(values[i] / 1000) + "k";
            } else {
                valStr = "₹" + (int)values[i];
            }
            canvas.drawText(valStr, left + barWidth / 2f, top - 8f, valuePaint);
        }
    }
}