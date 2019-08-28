package com.zhaoss.weixinrecorded.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.zhaoss.weixinrecorded.R;

/**
 * RecordView
 *
 * @blame Android Team
 */
public class RecordView extends View {

    private Paint paint;
    private OnGestureListener mOnGestureListener;

    private int downColor;
    private int upColor;

    private float slideDis;

    private float radiusDis;
    private float currentRadius;
    private float downRadius;
    private float upRadius;

    private float strokeWidthDis;
    private float currentStrokeWidth;
    private float minStrokeWidth;
    private float maxStrokeWidth;

    public RecordView(Context context) {
        super(context);
        init();
    }

    public RecordView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        downColor = R.color.video_gray;
        upColor = R.color.white;

        paint = new Paint();
        //抗锯齿
        paint.setAntiAlias(true);
        //画笔属性是空心圆
        paint.setStyle(Paint.Style.STROKE);
        currentStrokeWidth = getResources().getDimension(R.dimen.dp10);
        //设置画笔粗细
        paint.setStrokeWidth(currentStrokeWidth);

        slideDis = getResources().getDimension(R.dimen.dp10);
        radiusDis = getResources().getDimension(R.dimen.dp3);
        strokeWidthDis = getResources().getDimension(R.dimen.dp1) / 4;

        minStrokeWidth = currentStrokeWidth;
        maxStrokeWidth = currentStrokeWidth * 2;
    }

    /**
     * @param changed 是否改变
     * @param left    左
     * @param top     上
     * @param right   右
     * @param bottom  下
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (downRadius == 0) {
            downRadius = getWidth() * 0.5f - currentStrokeWidth;
            upRadius = getWidth() * 0.3f - currentStrokeWidth;
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mOnGestureListener != null) {
                down = true;
                invalidate();
                mOnGestureListener.onDown();
            }
        }
    };

    public boolean isDown() {
        return down;
    }

    private boolean down;
    private float downX;
    private float downY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                ViewGroup parent = (ViewGroup) getParent();
                parent.requestDisallowInterceptTouchEvent(true);
                downX = event.getRawX();
                downY = event.getRawY();
                mHandler.sendEmptyMessageDelayed(0, 200);
                break;
            case MotionEvent.ACTION_MOVE:

                break;
            case MotionEvent.ACTION_UP:
                ViewGroup parent1 = (ViewGroup) getParent();
                parent1.requestDisallowInterceptTouchEvent(false);
                float upX = event.getRawX();
                float upY = event.getRawY();
                if (mHandler.hasMessages(0)) {
                    mHandler.removeMessages(0);
                    if (Math.abs(upX - downX) < slideDis && Math.abs(upY - downY) < slideDis) {
                        if (mOnGestureListener != null) {
                            mOnGestureListener.onClick();
                        }
                    }
                } else {
                    if (mOnGestureListener != null) {
                        mOnGestureListener.onUp();
                    }
                }
                initState();
                break;
            default:
                break;
        }
        return true;
    }

    public void initState() {
        down = false;
        mHandler.removeMessages(0);
        invalidate();
    }

    public void setOnGestureListener(OnGestureListener listener) {
        this.mOnGestureListener = listener;
    }

    public interface OnGestureListener {
        /**
         * 按下
         */
        void onDown();

        /**
         * 抬起
         */
        void onUp();

        /**
         * 点击
         */
        void onClick();
    }

    boolean changeStrokeWidth;
    boolean isAdd;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (down) {
            paint.setColor(ContextCompat.getColor(getContext(), downColor));
            if (changeStrokeWidth) {
                if (isAdd) {
                    currentStrokeWidth += strokeWidthDis;
                    if (currentStrokeWidth > maxStrokeWidth) {
                        isAdd = false;
                    }
                } else {
                    currentStrokeWidth -= strokeWidthDis;
                    if (currentStrokeWidth < minStrokeWidth) {
                        isAdd = true;
                    }
                }
                paint.setStrokeWidth(currentStrokeWidth);
                currentRadius = getWidth() * 0.5f - currentStrokeWidth;
            } else {
                if (currentRadius < downRadius) {
                    currentRadius += radiusDis;
                } else if (currentRadius >= downRadius) {
                    currentRadius = downRadius;
                    isAdd = true;
                    changeStrokeWidth = true;
                }
            }
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, currentRadius, paint);
            invalidate();
        } else {
            changeStrokeWidth = false;
            currentStrokeWidth = minStrokeWidth;
            paint.setStrokeWidth(currentStrokeWidth);
            paint.setColor(ContextCompat.getColor(getContext(), upColor));
            if (currentRadius > upRadius) {
                currentRadius -= radiusDis;
                invalidate();
            } else if (currentRadius < upRadius) {
                currentRadius = upRadius;
                invalidate();
            }
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, currentRadius, paint);
        }
    }
}
