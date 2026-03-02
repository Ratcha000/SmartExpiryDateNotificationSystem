package com.example.expirytrack.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.text.Text;

import java.util.List;

/**
 * Custom view to draw bounding boxes around detected text
 */
public class CameraOverlayView extends View {
    private Paint boxPaint;
    private Paint textPaint;
    private List<Text.TextBlock> textBlocks;

    public CameraOverlayView(Context context) {
        super(context);
        init();
    }

    public CameraOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Paint for bounding boxes
        boxPaint = new Paint();
        boxPaint.setColor(0xFF00FF00); // Green
        boxPaint.setStrokeWidth(3);
        boxPaint.setStyle(Paint.Style.STROKE);

        // Paint for highlight
        textPaint = new Paint();
        textPaint.setColor(0x4400FF00); // Semi-transparent green
        textPaint.setStyle(Paint.Style.FILL);
    }

    public void setTextBlocks(List<Text.TextBlock> blocks) {
        this.textBlocks = blocks;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (textBlocks == null || textBlocks.isEmpty()) {
            return;
        }

        for (Text.TextBlock block : textBlocks) {
            Rect boundingBox = block.getBoundingBox();
            if (boundingBox != null) {
                // Draw the bounding box
                canvas.drawRect(boundingBox, boxPaint);
            }
        }
    }
}
