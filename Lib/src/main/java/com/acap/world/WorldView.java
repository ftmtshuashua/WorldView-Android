package com.acap.world;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;


/**
 * <pre>
 * Tip:
 *      高度自由的自定义View继承基类
 *      抽象View中的内容为世界
 *      WorldView为摄像机窗口
 *
 *  View的绘制流程 - State at 21409
 *  draw(){
 *      -drawBackground()            - 绘制View的背景，设置BackgroundBounds 并 确保背景一直处于摄像机内
 *      if(FadingEdge启用){
 *      +   onDraw()
 *      +   dispatchDraw()
 *         -drawAutofilledHighlight()                   - Autofilled功能相关绘制内容
 *         -mOverlay.getOverlayView().dispatchDraw()    - 覆盖物绘制
 *      +   onDrawForeground()                          - 装饰物，滚动条 等
 *         -drawDefaultFocusHighlight()                 - 焦点高亮
 *         -debugDrawFocus()                            - 控制(View.DEBUG_DRAW,mAttachInfo.mDebugLayout)
 *      }else{
 *          canvas.save()
 *      +   onDraw()
 *      +   dispatchDraw()
 *          //滚动缓存？
 *          canvas.restore()
 *          -drawAutofilledHighlight()
 *          -mOverlay.getOverlayView().dispatchDraw()
 *      +   onDrawForeground()
 *          -debugDrawFocus()
 *      }
 *  }
 *
 * Created by ACap on 2021/1/26 17:24
 * </pre>
 */
public class WorldView extends View {

    /**
     * 标记允许相机高度无限增加,改模式下相机的视野可能会超出世界的边界
     */
    private static final int FLAG_CAMERA_HEIGHT_INFINITE = 0x1;

    private int flag;
    private WorldParameter mWorldParams;

    private GestureDetector mGestureDetector;
    private GestureListener mGestureListener;
    private int mTouchSlop;//被认为是滑动的最小位移距离
    private int mTouchSlopSquare; //被认为是滑动的最小位移距离

    private Scroller mScroller;

    private int TIME_FLING_ANIM = 1000;  //甩动动画的时间

    private OnWorldCameraChangeListener mOnWorldCameraChangeListener;

    /**
     * 如果用户正在拖动这个View，则为True
     */
    private boolean mIsBeingDragged = false;
    //开始监测用户是否拖动这个View
    private boolean mIsMonitorDragged = false;
    //前一个事件的位置
    private float mLastFocusX;
    private float mLastFocusY;
    //按下时的位置
    private float mDownFocusX;
    private float mDownFocusY;
    //当前位置
    private float mFocusX;
    private float mFocusY;


    public WorldView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public WorldView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public WorldView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }


    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        mWorldParams = new WorldParameter();
        mGestureListener = new GestureListener();
        mGestureDetector = new GestureDetector(context, mGestureListener);
        mScroller = new Scroller(context);
        setLongPress(false);

        int touchSlop;
        if (context == null) {
            touchSlop = ViewConfiguration.getTouchSlop();
        } else {
            final ViewConfiguration configuration = ViewConfiguration.get(context);
            touchSlop = configuration.getScaledTouchSlop();
        }
        mTouchSlop = touchSlop;
        mTouchSlopSquare = touchSlop * touchSlop;
    }

    //设置世界变化监听
    public void setOnWorldCameraChangeListener(OnWorldCameraChangeListener mOnWorldCameraChangeListener) {
        this.mOnWorldCameraChangeListener = mOnWorldCameraChangeListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int width = getWidth();
        int height = getHeight();
//        LogUtils.i("Scroll", MessageFormat.format("onMeasure() -> 测量:{0,number,0}x{1,number,0} - 真实:{2,number,0}x{3,number,0}", measuredWidth, measuredHeight, width, height));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
//        LogUtils.i("Scroll", MessageFormat.format("onLayout({0,number,0},{1,number,0},{2,number,0},{3,number,0})", left, top, right, bottom));
//        LogUtils.i("Scroll", MessageFormat.format("onLayout() -> 测量:{0,number,0}x{1,number,0} - 真实:{2,number,0}x{3,number,0}", getMeasuredWidth(), getMeasuredHeight(), getWidth(), getHeight()));
        final int width = getWidth();
        final int height = getHeight();

        mWorldParams.setViewSize(width, height);
        onMeasureWorldSize(mWorldParams, width, height);

        if (!mWorldParams.isInitWorldSize()) {
            mWorldParams.setWorldSize(width, height);
        }
    }

    //判断横向滚动是否启用
    protected boolean isScrollHorizontalEnable() {
        return computeHorizontalScrollRange() > computeHorizontalScrollExtent();
    }

    //判断纵向滚动是否启用
    protected boolean isScrollVerticalEnable() {
        return computeVerticalScrollRange() > computeVerticalScrollExtent();
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean isScrollVerticalEnable = isScrollVerticalEnable();
        boolean isScrollHorizontalEnable = isScrollHorizontalEnable();
        if (!isScrollVerticalEnable && !isScrollHorizontalEnable) {
            return super.dispatchTouchEvent(event);
        }

        int action = event.getAction();

        //计算手势位置给mDownFocusX/mDownFocusY/mLastFocusX/mLastFocusY/mFocusX/mFocusY
        final boolean pointerUp = (action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP;
        final int skipIndex = pointerUp ? event.getActionIndex() : -1;
        // 确认焦点
        float sumX = 0, sumY = 0;
        final int count = event.getPointerCount();
        for (int i = 0; i < count; i++) {
            if (skipIndex == i) continue;
            sumX += event.getX(i);
            sumY += event.getY(i);
        }
        final int div = pointerUp ? count - 1 : count;
        mFocusX = sumX / div;
        mFocusY = sumY / div;
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_DOWN:
                mDownFocusX = mLastFocusX = mFocusX;
                mDownFocusY = mLastFocusY = mFocusY;
                break;
        }


        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsMonitorDragged = true;
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float scrollX = mFocusX - mLastFocusX;
                float scrollY = mFocusY - mLastFocusY;
                float disX = mFocusX - mDownFocusX;
                float disY = mFocusY - mDownFocusY;

                if (mIsMonitorDragged) {
                    float absY = Math.abs(disY);
                    float absX = Math.abs(disX);
                    if (isScrollVerticalEnable && isScrollHorizontalEnable) {
                        float distance = absY * absX;
                        if (distance > mTouchSlopSquare) {
                            mIsMonitorDragged = false;
                            mIsBeingDragged = true;
                        }
                    } else if (isScrollVerticalEnable) {//只有纵向滚动
                        if (absX > mTouchSlop) {
                            mIsMonitorDragged = false;
                            mIsBeingDragged = false;
                        } else if (absY > mTouchSlop) {
                            mIsMonitorDragged = false;
                            mIsBeingDragged = true;
                        }
                    } else if (isScrollHorizontalEnable) {//只有横向滚动
                        if (absY > mTouchSlop) {
                            mIsMonitorDragged = false;
                            mIsBeingDragged = false;
                        } else if (absX > mTouchSlop) {
                            mIsMonitorDragged = false;
                            mIsBeingDragged = true;
                        }
                    }
                }

                if (mIsBeingDragged) {  //计算动态退出滑动的状态
                    boolean isVertically = scrollY == 0 || (scrollY > 0 && canScrollVertically(-1)) || (scrollY < 0 && canScrollVertically(1));
                    boolean isHorizontally = scrollX == 0 || (scrollX > 0 && canScrollHorizontally(-1)) || (scrollX < 0 && canScrollHorizontally(1));

                    if (isScrollVerticalEnable && isScrollHorizontalEnable) {
                        if (!isVertically && !isHorizontally) {
                            mIsBeingDragged = false;
                        }
                    } else if (isScrollVerticalEnable) {//只有纵向滚动
                        mIsBeingDragged = isVertically;
                    } else if (isScrollHorizontalEnable) {//只有横向滚动
                        mIsBeingDragged = isHorizontally;
                    }
                }
                break;
        }

        boolean processed = mIsMonitorDragged || mIsBeingDragged;

        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(processed);
        }

        super.dispatchTouchEvent(event);
        return processed;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean isScrollVerticalEnable = isScrollVerticalEnable();
        boolean isScrollHorizontalEnable = isScrollHorizontalEnable();
        if (!isScrollVerticalEnable && !isScrollHorizontalEnable) {
            return false;
        }

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mGestureDetector.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                boolean processed = mIsMonitorDragged || mIsBeingDragged;
                if (processed) {
                    mGestureDetector.onTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mGestureDetector.onTouchEvent(event);
                mGestureListener.setPressCancel();
                break;

        }
        return true;
    }

    @Override
    protected int computeVerticalScrollRange() {
        return getWorldParams().getWorldHeight();
    }

    @Override
    protected int computeHorizontalScrollRange() {
        return getWorldParams().getWorldWidth();
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {//判断Scroller是否执行完毕
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        WorldParameter world = getWorldParams();
        x = world.trimCameraX(x);
        y = world.trimCameraY(y);

        onScroll(getScrollX(), getScrollY(), x, y, x - getScrollX(), y - getScrollY());

        super.scrollTo(x, y);
        world.setCamera(x, y);
    }


    /**
     * 平滑的滚动到某个位置
     *
     * @param destX
     * @param destY
     */
    public void smoothScrollTo(int destX, int destY) {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
//        LogUtils.i("Scroll", MessageFormat.format("call - smoothScrollTo({0,number,0},{1,number,0})", destX, destY));
        int duration = TIME_FLING_ANIM;

        WorldParameter world = getWorldParams();
        int trim_x = world.trimCameraX(destX);
        int trim_y = world.trimCameraY(destY);
        int trim_dx = trim_x - getScrollX();
        int trim_dy = trim_y - getScrollY();
        double dis_dest = getDistance(getScrollX(), destX, getScrollY(), destY);
        double dis_trim = getDistance(getScrollX(), trim_x, getScrollY(), trim_y);

        duration = (int) (duration * (dis_trim / dis_dest));

        mScroller.startScroll(getScrollX(), getScrollY(), trim_dx, trim_dy, duration);

        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    protected final void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int saveCount = canvas.getSaveCount();
        onDrawWorld(canvas);
        canvas.restoreToCount(saveCount);

        if (mOnWorldCameraChangeListener != null) mOnWorldCameraChangeListener.onChange(getWorldParams());
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
    }

    /**
     * 请求重新测量世界的大小
     */
    protected void measureWorldSize() {
        if (getWidth() > 0 && getHeight() > 0) {
            onMeasureWorldSize(mWorldParams, getWidth(), getHeight());
            ViewCompat.postInvalidateOnAnimation(this);
        } else {//重新测量
            requestLayout();
        }
    }

    /**
     * 当测量到了初始世界的宽度和高度时，用于初始化真实世界的大小
     *
     * @param world  世界参数
     * @param width  View的大小
     * @param height View的高度
     */
    protected void onMeasureWorldSize(WorldParameter world, int width, int height) {
        world.setWorldSize(width, height);
    }

    /**
     * 在这里将整个世界绘制出来
     *
     * @param canvas
     */
    protected void onDrawWorld(Canvas canvas) {
    }

    @Nullable
    @Override
    protected final Parcelable onSaveInstanceState() {
        ViewSavedStateUtils state = new ViewSavedStateUtils(super.onSaveInstanceState());
        state.write(mWorldParams);
        onSaveWorldState(state);
        return state;
    }

    @Override
    protected final void onRestoreInstanceState(Parcelable state) {
        ViewSavedStateUtils ss = (ViewSavedStateUtils) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mWorldParams = ss.read();
        onRestoreWorldState(ss);
    }

    /**
     * 当需要储存View状态时候
     *
     * @param save
     */
    protected void onSaveWorldState(ViewSavedStateUtils save) {

    }

    /**
     * 当需要回复View状态时
     *
     * @param read
     */
    protected void onRestoreWorldState(ViewSavedStateUtils read) {

    }


    public WorldParameter getWorldParams() {
        return mWorldParams;
    }

    /**
     * 设置长按功能启用状态，当长按功能启用并被触发之后将不响应后续事件
     * ，该功能默认关闭
     *
     * @param enable 是否启用长按功能
     */
    public void setLongPress(boolean enable) {
        mGestureDetector.setIsLongpressEnabled(enable);
    }

    /**
     * 当用户触摸到View时回调
     *
     * @param x
     * @param y
     */
    protected void onDown(float x, float y) {
    }

    /**
     * 检测到用户的触摸状态时回调
     *
     * @param press 用户是否触摸
     * @param x     用户触摸位置的x坐标
     * @param y     用户触摸位置的y坐标
     */
    protected void onPressStateChange(boolean press, float x, float y) {

    }

    /**
     * 用户单击事件
     *
     * @param x 用户触摸位置的x坐标
     * @param y 用户触摸位置的y坐标
     */
    protected void onSingleTap(float x, float y) {
    }

    /**
     * 如果需要检测用户的双击事件，则该方法返回用户的单击事件
     *
     * @param x 用户触摸位置的x坐标
     * @param y 用户触摸位置的y坐标
     */
    protected void onSingleTapByDouble(float x, float y) {
    }

    /**
     * 用户双击事件
     *
     * @param x 用户触摸位置的x坐标
     * @param y 用户触摸位置的y坐标
     */
    protected void onDoubleTap(float x, float y) {
    }

    /**
     * 当用户长按时触发，该功能默认关闭，开启需要 setLongPress(true)
     *
     * @param x
     * @param y
     */
    protected void onLongPress(float x, float y) {
    }

    /**
     * 当发生甩动手势时回调
     *
     * @param velocityX x轴方向的速度
     * @param velocityY y轴方向的速度
     */
    protected void onFling(float velocityX, float velocityY) {
    }

    /**
     * 当滚动时回调
     *
     * @param oldX      滚动前的位置x
     * @param oldY      滚动前的位置y
     * @param newX      滚动后的位置x
     * @param newY      滚动后的位置y
     * @param distanceX 滚动的位移x
     * @param distanceY 滚动的位移y
     */
    protected void onScroll(int oldX, int oldY, int newX, int newY, int distanceX, int distanceY) {

    }

    private final class GestureListener implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
        private float speed = 1f - 0.618f;

        /**
         * 用户的触摸状态，当用户触摸View并且未滑动时为True
         */
        private boolean mIsPress = false;

        //取消触摸效果
        private void setPressCancel() {
            if (mIsPress) {
                mIsPress = false;
                onPressStateChange(false, 0, 0);
            }
        }

        private void setPress(float x, float y) {
            if (!mIsPress) {
                mIsPress = true;
                onPressStateChange(true, x, y);
            }
        }

        //获得触摸点对应在世界中的位置
        private float getWorldX(MotionEvent e) {
            return e.getX() + getWorldParams().getCameraLeft();
        }

        //获得触摸点对应在世界中的位置
        private float getWorldY(MotionEvent e) {
            return e.getY() + getWorldParams().getCameraTop();
        }

        @Override
        public boolean onDown(MotionEvent e) {
            WorldView.this.onDown(getWorldX(e), getWorldY(e));
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            setPress(getWorldX(e), getWorldY(e));
        }

        @Override
        public void onLongPress(MotionEvent e) {
            WorldView.this.onLongPress(getWorldX(e), getWorldY(e));
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            WorldView.this.onSingleTap(getWorldX(e), getWorldY(e));
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            WorldView.this.onSingleTapByDouble(getWorldX(e), getWorldY(e));
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            WorldView.this.onDoubleTap(getWorldX(e), getWorldY(e));
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
//            LogUtils.i("手势", MessageFormat.format("onDoubleTapEvent({0,number,0.##},{1,number,0.##})", 0, 0));
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mIsBeingDragged) {
                setPressCancel();
                scrollTo((int) (getScrollX() + distanceX), (int) (getScrollY() + distanceY));
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            WorldView.this.onFling(velocityX, velocityY);
            smoothScrollTo((int) (getScrollX() - velocityX * speed), (int) (getScrollY() - velocityY * speed));
            return true;
        }
    }


    /**
     * 获得两点间的距离
     */
    private static final double getDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

}