package com.tarasantoshchuk.randomlayout;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RandomLayout extends ViewGroup {
    private static final int DEFAULT_CHILD_HEIGHT = 70;
    private static final int DEFAULT_CHILD_WIDTH = 70;

    private static final int DEFAULT_BORDER_PADDING = 1;
    private static final int DEFAULT_BORDER_WIDTH = 2;
    private static final int DEFAULT_BORDER_COLOR = Color.BLACK;

    private final static int ANIMATION_DURATION = (int) TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);

    private int mChildHeight;
    private int mChildWidth;

    private int mBorderPadding;
    private int mBorderWidth;
    private int mBorderColor;

    private GestureDetector mDetector;

    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random mRandom = new Random();
    private final RectF mBorderRect = new RectF();

    private List<Point> mChildrenTopLeftCorners;

    private Animator.AnimatorListener mAnimationListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            invalidate();
        }

        @Override
        public void onAnimationCancel(Animator animator) {
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }
    };

    public RandomLayout(Context context) {
        this(context, null);
    }

    public RandomLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RandomLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setWillNotDraw(false);

        mDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                int childToAnimate = getChildCount();

                View view = getChildAt(mRandom.nextInt(childToAnimate));

                PointF finalLocation = getNearestAvailableLocation(e.getX(), e.getY(), childToAnimate);

                view
                        .animate()
                        .x(finalLocation.x)
                        .y(finalLocation.y)
                        .setDuration(ANIMATION_DURATION)
                        .setListener(mAnimationListener)
                        .start();

                return true;
            }
        });

        mChildrenTopLeftCorners = new ArrayList<>();

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.RandomLayout);

        mChildHeight = attributes.getDimensionPixelSize(R.styleable.RandomLayout_child_height, dpToPx(DEFAULT_CHILD_HEIGHT));
        mChildWidth = attributes.getDimensionPixelSize(R.styleable.RandomLayout_child_width, dpToPx(DEFAULT_CHILD_WIDTH));

        mBorderPadding = attributes.getDimensionPixelOffset(R.styleable.RandomLayout_border_padding, dpToPx(DEFAULT_BORDER_PADDING));
        mBorderWidth = attributes.getDimensionPixelSize(R.styleable.RandomLayout_border_width, dpToPx(DEFAULT_BORDER_WIDTH));

        mBorderColor = attributes.getColor(R.styleable.RandomLayout_border_color, DEFAULT_BORDER_COLOR);

        attributes.recycle();
    }

    private PointF getNearestAvailableLocation(float x, float y, int childToAnimate) {
        //TODO: implement along with cleverer random
        return new PointF(x, y);
    }

    private int dpToPx(int dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getResources().getDisplayMetrics());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.EXACTLY) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        } else {
            width = 2 * getFullChildWidth() * getChildCount();
        }

        if (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.EXACTLY) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            height = 2 * getFullChildHeight() * getChildCount();
        }

        int childWidthSpec = MeasureSpec.makeMeasureSpec(mChildWidth, MeasureSpec.AT_MOST);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(mChildHeight, MeasureSpec.AT_MOST);

        for(int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(childWidthSpec, childHeightSpec);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mDetector.onTouchEvent(event);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();

        View child;
        for (int i = 0; i < childCount; i++) {
            child = getChildAt(i);

            int left;
            int top;


            //TODO: this might take too much time - cleverer random needed
            do {
                left = mRandom.nextInt(getWidth() - getFullChildWidth());
                top = mRandom.nextInt(getHeight() - getFullChildHeight());
            } while(isOverlapping(top, left, i));


            int right = left + mChildWidth;
            int bottom = top + mChildHeight;

            if (mChildrenTopLeftCorners.size() > i) {
                mChildrenTopLeftCorners.get(i).set(left, top);
            } else {
                mChildrenTopLeftCorners.add(new Point(left, top));
            }

            child.layout(left, top, right, bottom);
        }
    }

    private boolean isOverlapping(int top, int left, int childIndex) {
        for (int i = 0; i < childIndex; i++) {
            Point childCorner = mChildrenTopLeftCorners.get(i);

            if(overlappingCoordinate(top, childCorner.y, getFullChildHeight()) ||
                    overlappingCoordinate(left, childCorner.x, getFullChildWidth())) {
                return true;
            }
        }

        return false;
    }

    private int getFullChildHeight() {
        return mChildHeight + 2 * mBorderPadding + mBorderWidth;
    }

    private int getFullChildWidth() {
        return mChildWidth + 2 * mBorderPadding + mBorderWidth;
    }

    private boolean overlappingCoordinate(int first, int second, int delta) {
        return Math.abs(first - second) < delta;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mShadowPaint.setColor(mBorderColor);
        mShadowPaint.setStyle(Paint.Style.STROKE);
        mShadowPaint.setStrokeWidth(mBorderWidth);

        for(int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);

            mBorderRect.top = view.getY() - mBorderPadding;
            mBorderRect.left = view.getX() - mBorderPadding;
            mBorderRect.bottom = view.getY() + view.getHeight() + mBorderPadding;
            mBorderRect.right = view.getX() + view.getWidth() + mBorderPadding;

            canvas.drawRect(mBorderRect, mShadowPaint);
        }
    }


}
