package forpdateam.ru.forpda.views;

/**
 * Created by radiationx on 25.08.17.
 */

import android.content.Context;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.webkit.WebView;


/*
* Обработка событий аккуратно слизана с RecyclerView с некоторыми доработками.
* */
public class NestedGeckoView extends WebView implements NestedScrollingChild {
    private static final String LOG_TAG = NestedGeckoView.class.getSimpleName();

    private OnLongClickListener longClickListener = v -> true;
    private OnTouchListener clickListener = (v, event) -> true;

    private static final int INVALID_POINTER = -1;

    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_SETTLING = 2;
    public static final int SCROLL_STATE_NESTED_SCROLL = 3;
    public static final int SCROLL_STATE_SCROLL = 4;
    private int mScrollState = SCROLL_STATE_IDLE;

    private int mScrollPointerId = INVALID_POINTER;

    private VelocityTracker mVelocityTracker;

    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mLastTouchX;
    private int mLastTouchY;
    private int mTouchSlop;

    private final int mMinFlingVelocity;
    private final int mMaxFlingVelocity;

    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private final int[] mNestedOffsets = new int[2];


    private int longClickTimeout = 500;

    private NestedScrollingChildHelper mChildHelper;

    public NestedGeckoView(Context context) {
        this(context, null);
    }

    public NestedGeckoView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.webViewStyle);
    }

    public NestedGeckoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        longClickTimeout = ViewConfiguration.getLongPressTimeout();
        //setWillNotDraw(getOverScrollMode() == View.OVER_SCROLL_NEVER);
        changeLongClickable(false);
    }

    private void changeLongClickable(boolean enable) {
        /*Log.e("SUKA", "SET CLICKABLE " + enable);
        if (enable) {
            setOnLongClickListener(null);
            //setOnTouchListener(null);
            setLongClickable(true);
            setClickable(true);
            setHapticFeedbackEnabled(true);
        } else {
            setOnLongClickListener(longClickListener);
            //setOnTouchListener(clickListener);
            setLongClickable(false);
            setClickable(false);
            setHapticFeedbackEnabled(false);
        }*/
    }


    private Handler clickHandler = new Handler();

    private Runnable reservedStart = () -> {
        //changeLongClickable(true);
        Log.e("SUKA", "RESERV START " + mScrollState);
        if (mScrollState == SCROLL_STATE_IDLE) {
            callReserved();
        }
    };

    private Runnable enableClick = () -> {
        //changeLongClickable(true);
    };

    private MotionEvent reserved = null;
    private long lastDownTime = 0;

    private void callReserved() {
        if (reserved != null) {
            Log.e("SUKA", "CALL RESERV");
            super.onTouchEvent(reserved);
            reserved.recycle();
        }
        reserved = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        boolean eventAddedToVelocityTracker = false;

        final MotionEvent vtev = MotionEvent.obtain(e);
        final int action = MotionEventCompat.getActionMasked(e);
        //Log.d("SUKA", "ONTOUCH S=" + mScrollState + "; A=" + action);
        final int actionIndex = MotionEventCompat.getActionIndex(e);

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
        }
        vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);
        //Log.e("SUKAA", "ONTOUCH " + mScrollState + " : " + action);
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                lastDownTime = System.currentTimeMillis();
                mScrollPointerId = e.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);

                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
                nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
                startNestedScroll(nestedScrollAxis);
                reserved = MotionEvent.obtain(e);
                clickHandler.removeCallbacks(reservedStart);
                clickHandler.postDelayed(reservedStart, longClickTimeout);
                //super.onTouchEvent(e);
                /*if (mScrollState == SCROLL_STATE_SCROLL) {
                } else {
                    super.onTouchEvent(e);

                }*/
            }
            break;

            case MotionEvent.ACTION_MOVE: {
                final int index = e.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    Log.e(LOG_TAG, "Error processing scroll; pointer index for id " +
                            mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }

                final int x = (int) (e.getX(index) + 0.5f);
                final int y = (int) (e.getY(index) + 0.5f);
                int dx = mLastTouchX - x;
                int dy = mLastTouchY - y;

                final boolean preScrollConsumed = dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset);

                if (preScrollConsumed) {
                    dx -= mScrollConsumed[0];
                    dy -= mScrollConsumed[1];
                    vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                    mNestedOffsets[0] += mScrollOffset[0];
                    mNestedOffsets[1] += mScrollOffset[1];
                }

                if (preScrollConsumed) {
                    setScrollState(SCROLL_STATE_NESTED_SCROLL);
                } else {
                    if (isLongClickable()) {
                        changeLongClickable(false);
                    }
                    mLastTouchX = x - mScrollOffset[0];
                    mLastTouchY = y - mScrollOffset[1];

                    if (dy <= 0 && getScrollY() == 0) {
                        final boolean scrollConsumed = dispatchNestedScroll(0, 0, dx, dy, mScrollOffset);
                        if (scrollConsumed) {
                            mLastTouchX -= mScrollOffset[0];
                            mLastTouchY -= mScrollOffset[1];
                            vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                            mNestedOffsets[0] += mScrollOffset[0];
                            mNestedOffsets[1] += mScrollOffset[1];
                        }
                        setScrollState(SCROLL_STATE_NESTED_SCROLL);
                    } else {
                        if (dy != 0) {
                            if (reserved != null) {
                                Log.e("SUKA", "CALL FROM WV");
                                callReserved();
                            }
                            Log.e("SUKA", "WV SCROLL " + dy);
                            super.onTouchEvent(e);
                            setScrollState(SCROLL_STATE_SCROLL);
                        } else {
                            setScrollState(SCROLL_STATE_NESTED_SCROLL);
                        }
                    }
                }
            }
            break;


            case MotionEvent.ACTION_UP: {
                clickHandler.removeCallbacks(reservedStart);
                /*mVelocityTracker.addMovement(vtev);
                eventAddedToVelocityTracker = true;
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                final float xvel = -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId);
                final float yvel = -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId);
                if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
                }*/
                Log.d("SUKA", "A UP " + mScrollState);
                if (mScrollState == SCROLL_STATE_IDLE) {
                    callReserved();
                    if (System.currentTimeMillis() - lastDownTime < longClickTimeout) {
                        super.onTouchEvent(e);
                    }
                } else if (mScrollState == SCROLL_STATE_NESTED_SCROLL) {
                    reserved = null;
                    super.onTouchEvent(e);
                } else {
                    callReserved();
                    super.onTouchEvent(e);
                    /*if (System.currentTimeMillis() - lastDownTime >= 500){
                        clickHandler.postDelayed(()->{

                        }, )
                    }*/
                }
                setScrollState(SCROLL_STATE_IDLE);
                resetTouch();
                //super.onTouchEvent(e);
                changeLongClickable(true);
                //clickHandler.postDelayed(enableClick, 500);
            }
            break;

            case MotionEvent.ACTION_CANCEL: {
                cancelTouch();
                reserved = null;
                super.onTouchEvent(e);
                changeLongClickable(true);
                //clickHandler.postDelayed(enableClick, 500);
            }
            break;
        }
        if (!eventAddedToVelocityTracker) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();

        return true;
    }

    public boolean fling(int velocityX, int velocityY) {
        if (Math.abs(velocityX) < mMinFlingVelocity) {
            velocityX = 0;
        }

        if (Math.abs(velocityY) < mMinFlingVelocity) {
            velocityY = 0;
        }

        if (velocityX == 0 && velocityY == 0) {
            // If we don't have any velocity, return false
            return false;
        }

        if (!dispatchNestedPreFling(velocityX, velocityY)) {
            dispatchNestedFling(velocityX, velocityY, true);
            return true;
        }
        return false;
    }

    private void resetTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
        stopNestedScroll();
    }

    private void cancelTouch() {
        resetTouch();
        setScrollState(SCROLL_STATE_IDLE);
    }

    private void onPointerUp(MotionEvent e) {
        final int actionIndex = MotionEventCompat.getActionIndex(e);
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = e.getPointerId(newIndex);
            mInitialTouchX = mLastTouchX = (int) (e.getX(newIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (e.getY(newIndex) + 0.5f);
        }
    }

    void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        mScrollState = state;
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }
}