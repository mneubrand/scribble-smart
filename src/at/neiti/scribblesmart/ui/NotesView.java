package at.neiti.scribblesmart.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import at.neiti.scribblesmart.overlay.NotesOverlayService;

/**
 * This view captures hand written notes and saves them in a Bitmap
 * 
 * @author markus
 * 
 */
public class NotesView extends View {

    private Paint activePaint;

    // Currently drawn path
    private Path path;

    private Bitmap drawing;
    private Canvas drawingCanvas;

    private NotesOverlayService service;

    private boolean moved;

    public NotesView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        // Paint
        activePaint = new Paint();
        activePaint.setDither(true);
        activePaint.setAntiAlias(true);
        activePaint.setColor(Color.BLACK);
        activePaint.setStyle(Paint.Style.STROKE);
        activePaint.setStrokeJoin(Paint.Join.ROUND);
        activePaint.setStrokeCap(Paint.Cap.ROUND);
        activePaint.setStrokeWidth(2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.drawing == null) {
            this.drawing = Bitmap.createBitmap(this.getWidth(),
                    this.getHeight(), Config.ARGB_8888);
        }

        if (this.drawingCanvas == null) {
            this.drawingCanvas = new Canvas(drawing);
        }

        super.onDraw(canvas);

        canvas.drawBitmap(drawing, 0, 0, null);
        if (path != null) {
            canvas.drawPath(path, activePaint);
        }
    }

    /**
     * Handle touch event in drawing mode
     * 
     * @param event
     * @return
     */
    public boolean onTouchEvent(MotionEvent event) {
        if (this.isEnabled()) {
            this.service.setModified(true);

            // Log.i(TAG, "Motion at (" + event.getX() + ", " + event.getY() +
            // ") Action " + event.getAction());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                path = new Path();
                moved = false;

                // Initialize coordinates
                path.moveTo(event.getX(), event.getY());
                path.lineTo(event.getX(), event.getY());
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                // Replay historical events
                for (int i = 0; i < event.getHistorySize(); i++) {
                    path.lineTo(event.getHistoricalX(i),
                            event.getHistoricalY(i));
                }

                // Draw line to new coordinates
                path.lineTo(event.getX(), event.getY());
                moved = true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (moved) {
                    // Draw line to new coordinates
                    path.lineTo(event.getX(), event.getY());

                    // Commit the path to the drawing canvas
                    this.drawingCanvas.drawPath(path, activePaint);
                } else {
                    // We haven't moved so we draw a point
                    activePaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    this.drawingCanvas.drawCircle(event.getX(), event.getY(),
                            2, activePaint);
                    activePaint.setStyle(Paint.Style.STROKE);
                }
                // Reset currently drawn path
                path = null;
            }

            if (path != null) {
                RectF bounds = new RectF();
                path.computeBounds(bounds, true);
                Rect dirty = new Rect();
                bounds.roundOut(dirty);
                invalidate(dirty);
            } else {
                invalidate();
            }
            return true;
        } else {
            return false;
        }
    }

    public void clear() {
        this.drawingCanvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
        invalidate();
    }

    public Bitmap getDrawing() {
        return this.drawing;
    }

    public void setDrawing(Bitmap drawing) {
        this.drawing = drawing;
    }

    public void setService(NotesOverlayService service) {
        this.service = service;
    }

}
