package com.example.firebasecurd.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.firebasecurd.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StockDonutChartView extends View {

    public static class ChartSlice {
        public String label;
        public double value;
        public int color;
        public float percentage;

        public ChartSlice(String label, double value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final List<ChartSlice> slices = new ArrayList<>();
    private double totalValue = 0.0;
    private String centerSubtitle = "TOTAL VALUE";
    private String centerFormattedValue = "Rs. 0.00";

    private Paint arcPaint;
    private Paint centerTextPaint;
    private Paint centerSubTextPaint;
    private RectF arcBounds;
    private Typeface poppinsTypeface;

    public StockDonutChartView(Context context) {
        super(context);
        init(context);
    }

    public StockDonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StockDonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);

        centerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerTextPaint.setTextAlign(Paint.Align.CENTER);

        centerSubTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerSubTextPaint.setTextAlign(Paint.Align.CENTER);

        arcBounds = new RectF();

        try {
            poppinsTypeface = ResourcesCompat.getFont(context, R.font.font_poppins);
        } catch (Exception ignored) {}

        if (poppinsTypeface != null) {
            centerTextPaint.setTypeface(Typeface.create(poppinsTypeface, Typeface.BOLD));
            centerSubTextPaint.setTypeface(poppinsTypeface);
        } else {
            centerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }
    }

    public void setData(List<ChartSlice> newSlices, String subtitle, String formattedValue) {
        slices.clear();
        totalValue = 0.0;
        if (newSlices != null) {
            for (ChartSlice s : newSlices) {
                totalValue += s.value;
            }
            for (ChartSlice s : newSlices) {
                s.percentage = totalValue > 0 ? (float) ((s.value / totalValue) * 100.0) : 0f;
                slices.add(s);
            }
        }
        this.centerSubtitle = subtitle != null ? subtitle : "TOTAL VALUE";
        this.centerFormattedValue = formattedValue != null ? formattedValue : "Rs. 0.00";
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float strokeWidth = Math.min(width, height) * 0.14f; // 14% of diameter
        arcPaint.setStrokeWidth(strokeWidth);

        float padding = strokeWidth / 2f + 10f;
        float diameter = Math.min(width, height) - (padding * 2f);
        float left = (width - diameter) / 2f;
        float top = (height - diameter) / 2f;
        arcBounds.set(left, top, left + diameter, top + diameter);

        float cx = width / 2f;
        float cy = height / 2f;

        if (slices.isEmpty() || totalValue <= 0) {
            // Draw empty placeholder ring
            arcPaint.setColor(0xFFE2E8F0);
            canvas.drawArc(arcBounds, 0, 360, false, arcPaint);

            centerSubTextPaint.setColor(0xFF94A3B8);
            centerSubTextPaint.setTextSize(Math.max(10, diameter * 0.08f));
            canvas.drawText("NO DATA", cx, cy + (diameter * 0.03f), centerSubTextPaint);
            return;
        }

        float startAngle = -90f;
        float gapAngle = slices.size() > 1 ? 2.5f : 0f;

        for (ChartSlice s : slices) {
            float sweepAngle = (float) ((s.value / totalValue) * 360f);
            if (sweepAngle <= 0) continue;

            float actualSweep = Math.max(1f, sweepAngle - gapAngle);
            arcPaint.setColor(s.color);
            canvas.drawArc(arcBounds, startAngle + (gapAngle / 2f), actualSweep, false, arcPaint);
            startAngle += sweepAngle;
        }

        // Draw Center Subtitle (e.g. "TOTAL VALUE")
        centerSubTextPaint.setColor(0xFF64748B);
        float subSize = Math.max(10f, diameter * 0.075f);
        centerSubTextPaint.setTextSize(subSize);
        canvas.drawText(centerSubtitle, cx, cy - (subSize * 0.6f), centerSubTextPaint);

        // Draw Center Formatted Value (e.g. "Rs. 12,450.00")
        centerTextPaint.setColor(0xFF0F172A);
        float valSize = Math.max(13f, diameter * 0.125f);
        centerTextPaint.setTextSize(valSize);
        canvas.drawText(centerFormattedValue, cx, cy + (valSize * 0.7f), centerTextPaint);
    }
}
