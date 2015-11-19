package com.alperez.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.alperez.verticalseekbardemo.R;

/**
 * Created by stanislav.perchenko on 13-Nov-15.
 */
public class VerticalSeekBar extends View {
    // Sizes of content items
    private int mThumbSize = 25;
    private float mThumbSizeHalf = 12.5f;
    private int mNotSelectedBarWidth = 4;
    private int mSelectedBarWidth = 10;
    private int mPreferredTouchableAreaWidth;

    // Colors of elements
    private int colorNotSelectedBar;
    private int colorSelectedBar;
    private int colorSelectedBarTouched;
    private int colorThumb;
    private int colorThumbTouched;
    private int colorThumbTouchedWrapper;

    // Arguments for test drawings
    private boolean mTestShowContentArea;
    private int mTestContentAreaColor;
    private boolean mTestShowToucableArea;
    private int mTestToucableAreaColor;

    // Cached values of paddings
    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;

    // Size and start position of content area regarding to View's size and paddings.
    private int contentAreaStartX;
    private int contentAreaStartY;
    private int contentAreaWidth;
    private int contentAreaHeight;
    private float contentAreaCenterHorizontal;
    private float thumbPositionYTop;
    private float thumbPositionYBot;
    private float touchAreaStartX;
    private float touchAreaEndX;


    //----  Status fields  ----
    private boolean targetIsBeingTouched;

    private float mPixelYPosition;


    //----  Real progress values  ----
    private float mMin = 0;
    private float mMax = 100f;
    private float mProgress = 50f;



    private boolean wasLayout;

    private Paint mPaintStroke;
    private Paint mPaintFill;

    public VerticalSeekBar(Context context) {
        super(context);
        init(context);
    }

    public VerticalSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalSeekBar(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        extractArguments(attrs, defStyle);
        init(context);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VerticalSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        extractArguments(attrs, defStyleAttr);
        init(context);
    }

    private void extractArguments(@Nullable AttributeSet attrs, int defStyleAttr) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.VerticalSeekBar, defStyleAttr, 0);
            try {
                mThumbSize = a.getDimensionPixelSize(R.styleable.VerticalSeekBar_thumbSize, mThumbSize);
                mThumbSizeHalf = (float)mThumbSize / 2f;
                mNotSelectedBarWidth = a.getDimensionPixelSize(R.styleable.VerticalSeekBar_notSelectedBarWidth, mNotSelectedBarWidth);
                mSelectedBarWidth = a.getDimensionPixelSize(R.styleable.VerticalSeekBar_selectedBarWidth, mSelectedBarWidth);
                mPreferredTouchableAreaWidth = a.getDimensionPixelSize(R.styleable.VerticalSeekBar_prefTouchAreaWidth, 0);

                colorNotSelectedBar = a.getColor(R.styleable.VerticalSeekBar_notSelectedBarColor, colorNotSelectedBar);
                colorSelectedBar = a.getColor(R.styleable.VerticalSeekBar_selectedBarColor, colorSelectedBar);
                colorSelectedBarTouched = a.getColor(R.styleable.VerticalSeekBar_selectedBarColorTouched, colorSelectedBarTouched);
                colorThumb = a.getColor(R.styleable.VerticalSeekBar_thumbColor, colorThumb);
                colorThumbTouched = a.getColor(R.styleable.VerticalSeekBar_thumbTouchedColor, colorThumbTouched);
                colorThumbTouchedWrapper = a.getColor(R.styleable.VerticalSeekBar_thumbTouchedWrapperColor, colorThumbTouchedWrapper);

                mTestShowContentArea = a.getBoolean(R.styleable.VerticalSeekBar_testDrawContentArea, false);
                if (mTestShowContentArea) {
                    mTestContentAreaColor = a.getColor(R.styleable.VerticalSeekBar_testContentAreaColor, 0);
                }
                mTestShowToucableArea = a.getBoolean(R.styleable.VerticalSeekBar_testShowTouchableArea, false);
                if (mTestShowToucableArea) {
                    mTestToucableAreaColor = a.getColor(R.styleable.VerticalSeekBar_testTouchableAreaColor, 0);
                }
            } finally {
                a.recycle();
            }
        }
    }

    private void init(Context context) {
        mPaintStroke = new Paint();
        mPaintStroke.setAntiAlias(true);
        mPaintStroke.setStyle(Paint.Style.STROKE);
        mPaintFill = new Paint();
        mPaintFill.setAntiAlias(true);
        mPaintFill.setStyle(Paint.Style.FILL);
        validateElementsSize();
        updatePaddingInternal();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int minW = mThumbSize + paddingLeft + paddingRight;
        final int minH = 5*mThumbSize + paddingTop + paddingBottom;
        setMinimumWidth(minW);
        setMinimumHeight(minH);

        setMeasuredDimension(getActualViewSize(minW, widthMeasureSpec), getActualViewSize(minH, heightMeasureSpec));
        //super.setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    public static int getActualViewSize(int proposedMinSize, int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                return proposedMinSize;
            case MeasureSpec.EXACTLY:
                return specSize;
            case MeasureSpec.AT_MOST:
                return (proposedMinSize <= specSize) ? proposedMinSize : specSize;
            default:
                return proposedMinSize;
        }
    }

    /*********************************  Layout-related  *******************************************/




    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        updatePaddingInternal();
        if (wasLayout) {
            updateContentAreaSize(getWidth(), getHeight());
            updateTouchableAreaYBounds();
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        wasLayout = false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!wasLayout || changed) {
            targetIsBeingTouched = false;
            wasLayout = true;
            final int width = right - left;
            final int height = bottom - top;

            updateContentAreaSize(width, height);
            updateTouchableAreaYBounds();
            updateYPixelPositionByProgress();
        }
    }

    private void updatePaddingInternal() {
        paddingLeft = getPaddingLeft();
        paddingTop = getPaddingTop();
        paddingRight = getPaddingRight();
        paddingBottom = getPaddingBottom();
    }

    private void updateContentAreaSize(int viewWidth, int viewHeight) {
        contentAreaStartX = paddingLeft;
        contentAreaStartY = paddingTop;
        contentAreaWidth = viewWidth - paddingLeft - paddingRight;
        if (contentAreaWidth < 0) contentAreaWidth = 0;
        contentAreaHeight = viewHeight - paddingTop - paddingBottom;
        if (contentAreaHeight < 0) contentAreaHeight = 0;
        contentAreaCenterHorizontal = (float)(2*contentAreaStartX + contentAreaWidth) / 2f;

        thumbPositionYTop = contentAreaStartY + mThumbSizeHalf;
        thumbPositionYBot = contentAreaStartY + contentAreaHeight - mThumbSizeHalf;
    }

    private void updateTouchableAreaYBounds() {
        if (mPreferredTouchableAreaWidth < mThumbSize) {
            touchAreaStartX = contentAreaCenterHorizontal - mThumbSizeHalf;
            touchAreaEndX = touchAreaStartX + mThumbSize;
        } else {
            touchAreaStartX = contentAreaCenterHorizontal - (float)mPreferredTouchableAreaWidth / 2f;
            touchAreaEndX = touchAreaStartX + mPreferredTouchableAreaWidth;

            // Check that touchable area not exceeds paddings
            if (touchAreaStartX < contentAreaStartX) {
                touchAreaStartX = contentAreaStartX;
            }
            if (touchAreaEndX > (contentAreaStartX + contentAreaWidth)) {
                touchAreaEndX = contentAreaStartX + contentAreaWidth;
            }
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (wasLayout) {
            final int action = MotionEventCompat.getActionMasked(event);
            final float x = event.getX();
            final float y = event.getY();
            switch(action) {
                case MotionEvent.ACTION_DOWN:
                    if (!targetIsBeingTouched && isTouchValid(x, y)) {
                        startTouchMode(y);
                        super.invalidate();
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (targetIsBeingTouched) {
                        updateThumbPositionInTouchMode(y);
                        super.invalidate();
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (targetIsBeingTouched) {
                        targetIsBeingTouched = false;
                        super.invalidate();
                        return true;
                    }
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    private boolean isTouchValid(float touchX, float touchY) {
        if ((touchX >= touchAreaStartX) && (touchX < touchAreaEndX)) {
            if ((touchY >= contentAreaStartY) && (touchY < contentAreaStartY + contentAreaHeight)) {
                return true;
            }
        }

        final float halfContentWidth = (float)contentAreaWidth / 2f;
        float minY = mPixelYPosition - halfContentWidth;
        if (minY < contentAreaStartY) minY = contentAreaStartY;
        float maxY = mPixelYPosition + halfContentWidth;
        if (maxY >= contentAreaStartY+contentAreaHeight) maxY = contentAreaStartY+contentAreaHeight;
        if ((touchY >= minY) && (touchY < maxY)) {
            if ((touchX >= contentAreaStartX) && (touchX < contentAreaStartX*contentAreaWidth)) {
                return true;
            }
        }

        return false;
    }

    private void startTouchMode(final float pointerY) {
        if (targetIsBeingTouched) throw new IllegalStateException("This method can be called only when the View is not in touch mode");

        targetIsBeingTouched = true;
        updateThumbPositionInTouchMode(pointerY);
    }

    private void updateThumbPositionInTouchMode(final float pointerY) {
        if (!targetIsBeingTouched)
            throw new IllegalStateException("This method can be called only when the View is in touch mode");

        if (pointerY < thumbPositionYTop) {
            mPixelYPosition = thumbPositionYTop;
        }  else if (pointerY > thumbPositionYBot) {
            mPixelYPosition = thumbPositionYBot;
        } else {
            mPixelYPosition = pointerY;
        }

        final float pxProgress = thumbPositionYBot - mPixelYPosition;
        mProgress = (mMax - mMin) * pxProgress / (thumbPositionYBot - thumbPositionYTop);
    }


    private Rect contentAreaRect;
    private Path drawingAreaPath;


    @Override
    protected void onDraw(Canvas canvas) {
        if (contentAreaWidth == 0 || contentAreaHeight == 0) return;
        if (thumbPositionYBot - thumbPositionYTop <= 0) return;
        canvas.save();
        if (contentAreaRect == null) {
            contentAreaRect = new Rect(contentAreaStartX, contentAreaStartY, contentAreaStartX+contentAreaWidth, contentAreaStartY+contentAreaHeight);
        } else {
            contentAreaRect.set(contentAreaStartX, contentAreaStartY, contentAreaStartX+contentAreaWidth, contentAreaStartY+contentAreaHeight);
        }
        canvas.clipRect(contentAreaRect);

        //--- Test drawing of content area ---
        if (mTestShowContentArea) {
            mPaintFill.setColor(mTestContentAreaColor);
            canvas.drawColor(mTestContentAreaColor);
        }

        //--- Test drawing of the touchable area's borders ---
        if (mTestShowToucableArea) {

            if (drawingAreaPath == null) {
                drawingAreaPath = new Path();
            } else {
                drawingAreaPath.reset();
            }

            final float halfContentWidth = (float)contentAreaWidth / 2f;
            int minY = Math.round(mPixelYPosition - halfContentWidth);
            int maxY = Math.round(mPixelYPosition + halfContentWidth);

            if (minY <= contentAreaStartY) {
                drawingAreaPath.moveTo(contentAreaStartX, contentAreaStartY);
                drawingAreaPath.lineTo(contentAreaStartX+contentAreaWidth-1, contentAreaStartY);
                drawingAreaPath.lineTo(contentAreaStartX+contentAreaWidth-1, maxY-1);
                drawingAreaPath.lineTo(touchAreaEndX-1, maxY-1);
                drawingAreaPath.lineTo(touchAreaEndX-1, contentAreaStartY+contentAreaHeight-1);
                drawingAreaPath.lineTo(touchAreaStartX, contentAreaStartY+contentAreaHeight-1);
                drawingAreaPath.lineTo(touchAreaStartX, maxY-1);
                drawingAreaPath.lineTo(contentAreaStartX, maxY-1);
            } else if (maxY >= contentAreaStartY+contentAreaHeight) {
                drawingAreaPath.moveTo(touchAreaStartX, contentAreaStartY);
                drawingAreaPath.lineTo(touchAreaEndX-1, contentAreaStartY);
                drawingAreaPath.lineTo(touchAreaEndX-1, minY);
                drawingAreaPath.lineTo(contentAreaStartX+contentAreaWidth-1, minY);
                drawingAreaPath.lineTo(contentAreaStartX+contentAreaWidth-1, contentAreaStartY+contentAreaHeight-1);
                drawingAreaPath.lineTo(contentAreaStartX, contentAreaStartY+contentAreaHeight-1);
                drawingAreaPath.lineTo(contentAreaStartX, minY);
                drawingAreaPath.lineTo(touchAreaStartX, minY);
            } else {
                drawingAreaPath.moveTo(touchAreaStartX, contentAreaStartY);
                drawingAreaPath.lineTo(touchAreaEndX-1, contentAreaStartY);
                drawingAreaPath.lineTo(touchAreaEndX-1, minY);
                drawingAreaPath.lineTo(contentAreaStartX+contentAreaWidth-1, minY);
                drawingAreaPath.lineTo(contentAreaStartX+contentAreaWidth-1, maxY-1);
                drawingAreaPath.lineTo(touchAreaEndX-1, maxY-1);
                drawingAreaPath.lineTo(touchAreaEndX-1, contentAreaStartY+contentAreaHeight-1);
                drawingAreaPath.lineTo(touchAreaStartX, contentAreaStartY+contentAreaHeight-1);
                drawingAreaPath.lineTo(touchAreaStartX, maxY-1);
                drawingAreaPath.lineTo(contentAreaStartX, maxY-1);
                drawingAreaPath.lineTo(contentAreaStartX, minY);
                drawingAreaPath.lineTo(touchAreaStartX, minY);
            }
            drawingAreaPath.close();

            mPaintStroke.setAntiAlias(false);
            mPaintStroke.setStrokeWidth(0);
            mPaintStroke.setColor(mTestToucableAreaColor);
            canvas.drawPath(drawingAreaPath, mPaintStroke);
            mPaintStroke.setAntiAlias(true);
        }

        //--- Draw non-selected bar ---
        mPaintStroke.setColor(colorNotSelectedBar);
        mPaintStroke.setStrokeWidth(mNotSelectedBarWidth);
        canvas.drawLine(contentAreaCenterHorizontal, thumbPositionYTop, contentAreaCenterHorizontal, thumbPositionYBot, mPaintStroke);

        //--- Draw selected bar ---
        mPaintStroke.setColor(targetIsBeingTouched ? colorSelectedBarTouched : colorSelectedBar);
        mPaintStroke.setStrokeWidth(mSelectedBarWidth);
        canvas.drawLine(contentAreaCenterHorizontal, mPixelYPosition, contentAreaCenterHorizontal, thumbPositionYBot, mPaintStroke);

        //--- Draw thumb wrapper ---
        if (targetIsBeingTouched) {
            mPaintFill.setColor(colorThumbTouchedWrapper);
            canvas.drawCircle(contentAreaCenterHorizontal, mPixelYPosition, contentAreaWidth/2, mPaintFill);
        }

        //--- Draw thumb ---
        mPaintFill.setColor(targetIsBeingTouched ? colorThumbTouched : colorThumb);
        canvas.drawCircle(contentAreaCenterHorizontal, mPixelYPosition, mThumbSizeHalf, mPaintFill);

        canvas.restore();
    }


    /**********************************************************************************************/
    /**********************************  Setters for parameters  **********************************/
    /**********************************************************************************************/
    public void setElementsSize(int thumbSize, int notSelectedBarWidth, int selectedBarWidth) {
        mThumbSize = thumbSize;
        mThumbSizeHalf = (float) thumbSize / 2f;
        mNotSelectedBarWidth = notSelectedBarWidth;
        mSelectedBarWidth = selectedBarWidth;
        validateElementsSize();
    }

    public void setBarColors(int colorNotSelected, int colorSelected, int colorSelectedTouched) {
        this.colorNotSelectedBar = colorNotSelected;
        this.colorSelectedBar = colorSelected;
        this.colorSelectedBarTouched = colorSelectedTouched;
    }

    public void setThumbColors(int colorThumb, int colorThumbTouched, int colorThumbTouchedWrapper) {
        this.colorThumb = colorThumb;
        this.colorThumbTouched = colorThumbTouched;
        this.colorThumbTouchedWrapper = colorThumbTouchedWrapper;
    }

    public void setPreferredTouchableAreaWidth(int width) {
        mPreferredTouchableAreaWidth = width;
        updateTouchableAreaYBounds();
    }

    private void validateElementsSize() {
        if (mSelectedBarWidth > mThumbSize)
            throw new IllegalArgumentException("Selected bar width must not exceed thumb size");
        if (mNotSelectedBarWidth > mSelectedBarWidth)
            throw new IllegalArgumentException("Not selected bar width must not exceed selected bar width");
    }


    public void setProgress(float progress) {
        if (progress > mMax) {
            mProgress = mMax;
        } else if (progress < mMin) {
            mProgress = mMin;
        } else {
            mProgress = progress;
        }
        invalidate();
    }

    public float getProgress() {
        return mProgress;
    }

    public void setRange(float min, float max) {
        if (min >= max) throw new IllegalArgumentException("Min value must be less then Max value");
        mMin = min;
        mMax = max;
        if (mProgress < mMin) {
            mProgress = mMin;
        } else if (mProgress > mMax) {
            mProgress = mMax;
        }
        invalidate();
    }

    public float getMin() {
        return mMin;
    }

    public float getMax() {
        return mMax;
    }


    @Override
    public void invalidate() {
        updateYPixelPositionByProgress();
        super.invalidate();
    }

    private void updateYPixelPositionByProgress() {
        if (wasLayout) {
            mPixelYPosition = thumbPositionYBot - (thumbPositionYBot - thumbPositionYTop) * (mProgress) / (mMax - mMin);
        }
    }
}
