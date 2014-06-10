package org.lucasr.twowayview;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;

public class TWStaggeredGridView extends TWView {
    private static final String LOGTAG = "TwoWayStaggeredGridView";

    private static final int NO_LANE = -1;

    private TWLayoutState mLayoutState;
    private SparseIntArray mItemLanes;
    private int mLaneSize;
    private int mLaneCount;
    private boolean mIsVertical;

    public TWStaggeredGridView(Context context) {
        this(context, null);
    }

    public TWStaggeredGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TWStaggeredGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mLaneSize = 0;
        mLaneCount = 3;

        Orientation orientation = getOrientation();
        mLayoutState = new TWLayoutState(orientation, mLaneCount);
        mItemLanes = new SparseIntArray(10);
        mIsVertical = (orientation == Orientation.VERTICAL);
    }

    private int getLaneForPosition(int position, Flow flow) {
        int lane = mItemLanes.get(position, NO_LANE);
        if (lane != NO_LANE) {
            return lane;
        }

        int pos = (flow == Flow.FORWARD ? Integer.MAX_VALUE : Integer.MIN_VALUE);
        for (int i = 0; i < mLaneCount; i++) {
            final Rect laneState = mLayoutState.get(i);

            final int lanePos;
            if (mIsVertical) {
                lanePos = (flow == Flow.FORWARD ? laneState.bottom : laneState.top);
            } else {
                lanePos = (flow == Flow.FORWARD ? laneState.right : laneState.left);
            }

            if ((flow == Flow.FORWARD && lanePos < pos) ||
                (flow == Flow.BACKWARD && lanePos > pos)) {
                pos = lanePos;
                lane = i;
            }
        }

        mItemLanes.put(position, lane);
        return lane;
    }

    @Override
    public void setOrientation(Orientation orientation) {
        super.setOrientation(orientation);
        mIsVertical = (orientation == Orientation.VERTICAL);
    }

    @Override
    public void offsetLayout(int offset) {
        mLayoutState.offset(offset);
    }

    @Override
    public void resetLayout(int offset) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();

        if (mIsVertical) {
            mLaneSize = (getWidth() - paddingLeft - paddingRight) / mLaneCount;
        } else {
            mLaneSize = (getHeight() - paddingTop - paddingBottom) / mLaneCount;
        }

        for (int i = 0; i < mLaneCount; i++) {
            final int l = paddingLeft + (mIsVertical ? i * mLaneSize : offset);
            final int t = paddingTop + (mIsVertical ? offset : i * mLaneSize);
            final int r = (mIsVertical ? l + mLaneSize : l);
            final int b = (mIsVertical ? t : t + mLaneSize);

            mLayoutState.set(i, l, t, r, b);
        }
    }

    @Override
    public int getOuterStartEdge() {
        return mLayoutState.getOuterStartEdge();
    }

    @Override
    public int getInnerStartEdge() {
        return mLayoutState.getInnerStartEdge();
    }

    @Override
    public int getInnerEndEdge() {
        return mLayoutState.getInnerEndEdge();
    }

    @Override
    public int getOuterEndEdge() {
        return mLayoutState.getOuterEndEdge();
    }

    @Override
    public int getChildWidthMeasureSpec(View child, int position, LayoutParams lp) {
        if (!mIsVertical && lp.width == LayoutParams.WRAP_CONTENT) {
            return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        } else if (mIsVertical) {
            return MeasureSpec.makeMeasureSpec(mLaneSize, MeasureSpec.EXACTLY);
        } else {
            return MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        }
    }

    @Override
    public int getChildHeightMeasureSpec(View child, int position, LayoutParams lp) {
        if (mIsVertical && lp.height == LayoutParams.WRAP_CONTENT) {
            return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        } else if (!mIsVertical) {
            return MeasureSpec.makeMeasureSpec(mLaneSize, MeasureSpec.EXACTLY);
        } else {
            return MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        }
    }

    @Override
    public void detachChildFromLayout(View child, int position, Flow flow) {
        final int lane = mItemLanes.get(position, NO_LANE);
        if (lane == NO_LANE) {
            return;
        }

        if (flow == Flow.FORWARD) {
            mLayoutState.offset(lane, mIsVertical ? child.getHeight() : child.getWidth());
        }

        if (mIsVertical) {
            mLayoutState.reduceHeightBy(lane, child.getHeight());
        } else {
            mLayoutState.reduceWidthBy(lane, child.getWidth());
        }
    }

    @Override
    public void attachChildToLayout(View child, int position, Flow flow, Rect childRect) {
        final int childWidth = child.getMeasuredWidth();
        final int childHeight = child.getMeasuredHeight();

        final int lane = getLaneForPosition(position, flow);
        final Rect laneState = mLayoutState.get(lane);

        final int l, t, r, b;
        if (mIsVertical) {
            l = laneState.left;
            t = (flow == Flow.FORWARD ? laneState.bottom : laneState.top - childHeight);
            r = laneState.right;
            b = t + childHeight;
        } else {
            l = (flow == Flow.FORWARD ? laneState.right : laneState.left - childWidth);
            t = laneState.top;
            r = l + childWidth;
            b = laneState.bottom;
        }

        childRect.left = l;
        childRect.top = t;
        childRect.right = r;
        childRect.bottom = b;

        if (flow == Flow.BACKWARD) {
            mLayoutState.offset(lane, mIsVertical ? -childHeight : -childWidth);
        }

        if (mIsVertical) {
            mLayoutState.increaseHeightBy(lane, childHeight);
        } else {
            mLayoutState.increaseWidthBy(lane, childWidth);
        }
    }
}
