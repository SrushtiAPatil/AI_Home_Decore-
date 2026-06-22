package com.example.imageeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PhotoEditorView extends View {

    private Bitmap backgroundBitmap;
    private List<EditableObject> objects;
    private EditableObject selectedObject;
    private Paint paint;

    private PointF lastPoint;
    private ScaleGestureDetector scaleDetector;
    private float rotationAngle = 0f;
    private PointF midPoint;

    public PhotoEditorView(Context context) {
        super(context);
        init();
    }

    public PhotoEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        objects = new ArrayList<>();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        lastPoint = new PointF();
        midPoint = new PointF();

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
    }

    public void setBackgroundBitmap(Bitmap bitmap) {
        this.backgroundBitmap = bitmap;
        invalidate();
    }

    public void addObject(int resourceId) {
        Bitmap objBitmap = BitmapFactory.decodeResource(getResources(), resourceId);

        // Scale object to reasonable size
        int maxSize = Math.min(getWidth(), getHeight()) / 4;
        if (objBitmap.getWidth() > maxSize || objBitmap.getHeight() > maxSize) {
            float scale = (float) maxSize / Math.max(objBitmap.getWidth(), objBitmap.getHeight());
            int newWidth = (int) (objBitmap.getWidth() * scale);
            int newHeight = (int) (objBitmap.getHeight() * scale);
            objBitmap = Bitmap.createScaledBitmap(objBitmap, newWidth, newHeight, true);
        }

        EditableObject obj = new EditableObject(objBitmap);
        obj.x = getWidth() / 2f - obj.bitmap.getWidth() / 2f;
        obj.y = getHeight() / 2f - obj.bitmap.getHeight() / 2f;

        objects.add(obj);
        selectedObject = obj;
        invalidate();
    }

    public void clearObjects() {
        objects.clear();
        selectedObject = null;
        invalidate();
    }

    public Bitmap getCurrentBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    public Bitmap getBackgroundOnlyBitmap() {
        if (backgroundBitmap == null) return null;

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw only background
        if (backgroundBitmap != null) {
            float scale = Math.min(
                    (float) getWidth() / backgroundBitmap.getWidth(),
                    (float) getHeight() / backgroundBitmap.getHeight()
            );

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            matrix.postTranslate(
                    (getWidth() - backgroundBitmap.getWidth() * scale) / 2,
                    (getHeight() - backgroundBitmap.getHeight() * scale) / 2
            );

            canvas.drawBitmap(backgroundBitmap, matrix, paint);
        }

        return bitmap;
    }

    public Bitmap getObjectOnlyBitmap() {
        if (objects.isEmpty()) return null;

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw only objects
        for (EditableObject obj : objects) {
            obj.draw(canvas, paint);
        }

        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background image
        if (backgroundBitmap != null) {
            float scale = Math.min(
                    (float) getWidth() / backgroundBitmap.getWidth(),
                    (float) getHeight() / backgroundBitmap.getHeight()
            );

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            matrix.postTranslate(
                    (getWidth() - backgroundBitmap.getWidth() * scale) / 2,
                    (getHeight() - backgroundBitmap.getHeight() * scale) / 2
            );

            canvas.drawBitmap(backgroundBitmap, matrix, paint);
        }

        // Draw all objects
        for (EditableObject obj : objects) {
            obj.draw(canvas, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastPoint.set(event.getX(), event.getY());

                // Select object
                selectedObject = null;
                for (int i = objects.size() - 1; i >= 0; i--) {
                    if (objects.get(i).contains(event.getX(), event.getY())) {
                        selectedObject = objects.get(i);
                        break;
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1 && selectedObject != null) {
                    // Single finger - move
                    float dx = event.getX() - lastPoint.x;
                    float dy = event.getY() - lastPoint.y;
                    selectedObject.x += dx;
                    selectedObject.y += dy;
                    lastPoint.set(event.getX(), event.getY());
                    invalidate();
                } else if (event.getPointerCount() == 2 && selectedObject != null) {
                    // Two fingers - rotate
                    float angle = rotation(event);
                    selectedObject.rotation += angle - rotationAngle;
                    rotationAngle = angle;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    midPoint(midPoint, event);
                    rotationAngle = rotation(event);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private float rotation(MotionEvent event) {
        double deltaX = event.getX(0) - event.getX(1);
        double deltaY = event.getY(0) - event.getY(1);
        double radians = Math.atan2(deltaY, deltaX);
        return (float) Math.toDegrees(radians);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (selectedObject != null) {
                selectedObject.scale *= detector.getScaleFactor();
                selectedObject.scale = Math.max(0.1f, Math.min(selectedObject.scale, 5.0f));
                invalidate();
            }
            return true;
        }
    }

    // Inner class for editable objects
    private static class EditableObject {
        Bitmap bitmap;
        float x, y;
        float scale = 1.0f;
        float rotation = 0f;

        EditableObject(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        void draw(Canvas canvas, Paint paint) {
            canvas.save();

            // Calculate center position
            float centerX = x + bitmap.getWidth() * scale / 2;
            float centerY = y + bitmap.getHeight() * scale / 2;

            // ENHANCED REALISTIC SHADOW
            // Draw multiple shadow layers for depth
            drawMultiLayerShadow(canvas, centerX, centerY);

            // Draw main object with enhanced lighting
            Paint enhancedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            enhancedPaint.setFilterBitmap(true);
            enhancedPaint.setDither(true);

            // Add subtle edge darkening for depth
            enhancedPaint.setShadowLayer(2f, 0f, 1f, Color.argb(25, 0, 0, 0));

            canvas.translate(centerX, centerY);
            canvas.rotate(rotation);
            canvas.scale(scale, scale);

            // Apply subtle lighting gradient
            Bitmap litObject = applyLightingGradient(bitmap);
            canvas.drawBitmap(litObject, -litObject.getWidth() / 2f, -litObject.getHeight() / 2f, enhancedPaint);

            canvas.restore();
        }

        private void drawMultiLayerShadow(Canvas canvas, float centerX, float centerY) {
            // Layer 1: Soft outer shadow (ambient)
            Paint outerShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
            outerShadow.setAlpha(30);
            outerShadow.setMaskFilter(new android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL));

            canvas.save();
            canvas.translate(centerX + 3, centerY + 10);
            canvas.rotate(rotation);
            canvas.scale(scale * 1.05f, scale * 0.3f);

            Bitmap outerShadowBitmap = createShadowBitmap(bitmap, 0.3f);
            canvas.drawBitmap(outerShadowBitmap, -outerShadowBitmap.getWidth() / 2f, -outerShadowBitmap.getHeight() / 2f, outerShadow);
            canvas.restore();

            // Layer 2: Medium contact shadow
            Paint midShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
            midShadow.setAlpha(50);
            midShadow.setMaskFilter(new android.graphics.BlurMaskFilter(12f, android.graphics.BlurMaskFilter.Blur.NORMAL));

            canvas.save();
            canvas.translate(centerX + 2, centerY + 6);
            canvas.rotate(rotation);
            canvas.scale(scale * 0.95f, scale * 0.25f);

            Bitmap midShadowBitmap = createShadowBitmap(bitmap, 0.5f);
            canvas.drawBitmap(midShadowBitmap, -midShadowBitmap.getWidth() / 2f, -midShadowBitmap.getHeight() / 2f, midShadow);
            canvas.restore();

            // Layer 3: Sharp inner shadow (direct)
            Paint innerShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
            innerShadow.setAlpha(70);
            innerShadow.setMaskFilter(new android.graphics.BlurMaskFilter(6f, android.graphics.BlurMaskFilter.Blur.NORMAL));

            canvas.save();
            canvas.translate(centerX + 1, centerY + 3);
            canvas.rotate(rotation);
            canvas.scale(scale * 0.92f, scale * 0.2f);

            Bitmap innerShadowBitmap = createShadowBitmap(bitmap, 0.7f);
            canvas.drawBitmap(innerShadowBitmap, -innerShadowBitmap.getWidth() / 2f, -innerShadowBitmap.getHeight() / 2f, innerShadow);
            canvas.restore();
        }

        private Bitmap createShadowBitmap(Bitmap source, float darkness) {
            Bitmap shadow = source.copy(Bitmap.Config.ARGB_8888, true);
            Canvas shadowCanvas = new Canvas(shadow);

            Paint darkPaint = new Paint();
            android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix();
            colorMatrix.set(new float[]{
                    0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0,
                    0, 0, 0, darkness, 0
            });
            darkPaint.setColorFilter(new android.graphics.ColorMatrixColorFilter(colorMatrix));
            shadowCanvas.drawBitmap(source, 0, 0, darkPaint);

            return shadow;
        }

        private Bitmap applyLightingGradient(Bitmap source) {
            Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(result);

            // Create subtle lighting from top-left
            Paint lightPaint = new Paint();
            lightPaint.setAntiAlias(true);

            android.graphics.LinearGradient gradient = new android.graphics.LinearGradient(
                    0, 0, result.getWidth(), result.getHeight(),
                    Color.argb(15, 255, 255, 255),
                    Color.argb(0, 0, 0, 0),
                    android.graphics.Shader.TileMode.CLAMP
            );

            lightPaint.setShader(gradient);
            lightPaint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN));

            canvas.drawRect(0, 0, result.getWidth(), result.getHeight(), lightPaint);

            return result;
        }

        boolean contains(float touchX, float touchY) {
            float left = x;
            float top = y;
            float right = x + bitmap.getWidth() * scale;
            float bottom = y + bitmap.getHeight() * scale;
            return touchX >= left && touchX <= right && touchY >= top && touchY <= bottom;
        }
    }
}