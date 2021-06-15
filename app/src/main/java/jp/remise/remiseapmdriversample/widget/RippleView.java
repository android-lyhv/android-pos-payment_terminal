package jp.remise.remiseapmdriversample.widget;


import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import jp.remise.remiseapmdriversample.R;

public class RippleView extends View {
    public static final int MODE_IN = 1;
    public static final int MODE_OUT = 2;
    private static final int DEFAULT_RIPPLE_COLOR = Color.rgb(0x33, 0x99, 0xcc);
    private int mRippleColor = DEFAULT_RIPPLE_COLOR;
    private int mMinSize = 200;
    private boolean animationRunning = false;
    private int currentProgress = 0;
    private int mRippleNum = 4;
    private int mTotalTime = 1000 * 1000;
    private int mode = MODE_OUT;
    private int mPeriod = 30;
    private int mCenterX;
    private int mCenterY;
    private int mRadius;
    private Paint mPaint;
    private ObjectAnimator mAnimator;
    private TypeEvaluator mProgressEvaluator = (fraction, startValue, endValue) -> {
        fraction = (fraction * mTotalTime / mPeriod) % 100;
        return fraction;
    };

    public RippleView(Context context) {
        super(context);
        initPaint();
        initAnimation();
    }

    public RippleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initValue(attrs);
        initPaint();
        initAnimation();
        invalidate();
    }

    public RippleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initValue(attrs);
        initPaint();
        initAnimation();
    }

    private void initValue(AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        final TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.RippleView);
        mRippleColor = typedArray.getColor(R.styleable.RippleView_rv_color, Color.BLACK);
        mRippleNum = typedArray.getInt(R.styleable.RippleView_rv_rippleCount, 4);
        mRadius = typedArray.getInt(R.styleable.RippleView_rv_radius, 200);
        mTotalTime = typedArray.getInt(R.styleable.RippleView_rv_duration, 1000 * 1000);
        typedArray.recycle();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mRippleColor);
    }

    private void initAnimation() {
        mAnimator = ObjectAnimator.ofInt(this, "currentProgress", 0, 100);
        mAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        mAnimator.setRepeatMode(ObjectAnimator.RESTART);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setEvaluator(mProgressEvaluator);
        mAnimator.setDuration(mTotalTime);
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setColor(int color) {
        mRippleColor = color;
        mPaint.setColor(mRippleColor);
    }

    public void startRippleAnimation() {
        if (!animationRunning) {
            mAnimator.start();
            animationRunning = true;
        }
    }

    public void stopRippleAnimation() {
        if (animationRunning) {
            mAnimator.end();
            animationRunning = false;
        }
    }

    public boolean isRippleAnimationRunning() {
        return animationRunning;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
        this.invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int resultWidth;
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (modeWidth == MeasureSpec.EXACTLY) {
            resultWidth = sizeWidth;
        } else {
            resultWidth = mMinSize;
            if (modeWidth == MeasureSpec.AT_MOST) {
                resultWidth = Math.min(resultWidth, sizeWidth);
            }
        }
        int resultHeight;
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (modeHeight == MeasureSpec.EXACTLY) {
            resultHeight = sizeHeight;
        } else {
            resultHeight = mMinSize;
            if (modeHeight == MeasureSpec.AT_MOST) {
                resultHeight = Math.min(resultHeight, sizeHeight);
            }
        }
        mCenterX = resultWidth / 2;
        mCenterY = resultHeight / 2;
        mRadius = Math.max(resultWidth, resultHeight) / 2;
        setMeasuredDimension(resultWidth, resultHeight);
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (int i = 0; i < mRippleNum; i++) {
            int progress = (currentProgress + i * 100 / (mRippleNum)) % 100;
            if (mode == 1) {
                progress = 100 - progress;
            }
            mPaint.setAlpha(255 - 255 * progress / 100);
            canvas.drawCircle(mCenterX, mCenterY, mRadius * progress / 100, mPaint);
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isRippleAnimationRunning())
            stopRippleAnimation();
    }
}
