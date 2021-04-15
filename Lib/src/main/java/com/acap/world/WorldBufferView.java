package com.acap.world;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

/**
 * <pre>
 * Tip:
 *     当有大量绘制操作时候,使用带有缓冲功能WorldView，能提高绘制性能
 *
 * Created by ACap on 2021/4/2 17:58
 * </pre>
 */
public class WorldBufferView extends WorldView {

    /**
     * 缓冲对象
     */
    private Bitmap mBuffer;
    /**
     * 用于创建缓冲
     */
    private BufferBuilder mBufferBuilder;  //Buffer构造器

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * 构建缓冲的标志位，当它为TRUE时候，缓冲将被刷新
     */
    private boolean mIsBuildBuffer = true;


    public WorldBufferView(Context context) {
        super(context);
    }

    public WorldBufferView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WorldBufferView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void measureWorldSize() {
        super.measureWorldSize();
        setBufferReset();
    }

    /**
     * 创建缓存对象 ,为了降低对内存的使用默认使用4444编码.
     *
     * @param width  缓冲对象的宽度
     * @param height 缓冲对象的高度
     * @return 缓冲对象
     */
    protected Bitmap onCreateBuffer(int width, int height) {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
    }

    /**
     * 重置缓冲数据，使用新的缓冲数据替代原来的缓冲数据
     */
    public void setBufferReset() {
        mIsBuildBuffer = true;
        postInvalidateAtThread();
    }

    //开始绘制世界
    protected final void onDrawWorld(Canvas canvas) {
        if (mBuffer == null) {
            onDrawWorldLauncher(canvas);
        }

        onBuildBuffer();

        if (mBuffer != null) {
            canvas.drawBitmap(mBuffer, 0, 0, mPaint);
        }

        onDrawWorldAnimation(canvas);
    }

    /**
     * 当缓冲还未构建完成时被调用
     *
     * @param canvas
     */
    protected void onDrawWorldLauncher(Canvas canvas) {

    }

    /**
     * 绘制缓冲内容，通常这部分内容是静止不动的.
     *
     * @param canvas
     */
    protected void onDrawWorldBuffer(Canvas canvas) {
    }

    /**
     * 绘制世界的动效，一些动态效果在这里被绘制
     *
     * @param canvas
     */
    protected void onDrawWorldAnimation(Canvas canvas) {

    }


    //buffer的构建
    private void onBuildBuffer() {
        //如果需要构建Buffer,需要验证是否正在构建,正在构建的Buffer通常的历史的
        if (mIsBuildBuffer) {
            if (mBufferBuilder == null) {
                mIsBuildBuffer = false;
                mBufferBuilder = new BufferBuilder();
                mBufferBuilder.setOnBufferBuildListener(bitmap -> {
                    setBuffer(bitmap);


                    mBufferBuilder = null;
                });
                mBufferBuilder.start();
            } else {
                mBufferBuilder.stop();
                mBufferBuilder = null;
                postInvalidateAtThread();
            }
        }
    }


    /**
     * 同步Buffer
     */
    private void setBuffer(Bitmap bitmap) {
        final Bitmap buffer_old = mBuffer;
        mBuffer = bitmap;
        postInvalidateAtThread();

        if (buffer_old != null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> buffer_old.recycle(), 100);
        }

    }

    private void clearBuffer() {
        if (mBuffer != null) {
            mBuffer.recycle();
            mBuffer = null;
        }
    }

    private final void postInvalidateAtThread() {
        if (Build.VERSION.SDK_INT >= 16) {
            this.postInvalidateOnAnimation();
        } else {
            this.postInvalidate();
        }
    }

    //缓存构建
    private final class BufferBuilder extends Thread {

        private Action<Bitmap> mAction;

        public BufferBuilder() {
            super();

        }

        public BufferBuilder setOnBufferBuildListener(Action<Bitmap> action) {
            mAction = action;
            return this;
        }

        @Override
        public void run() {
            WorldParameter params = getWorldParams();
            int worldWidth = params.getWorldWidth();
            int worldHeight = params.getWorldHeight();
            if (worldWidth > 0 && worldHeight > 0) {
                Bitmap bitmap = onCreateBuffer(worldWidth, worldHeight);
                if (bitmap != null) {
                    onDrawWorldBuffer(new Canvas(bitmap));
                    if (isInterrupted()) {
                        bitmap.recycle();
                    } else {
                        if (mAction != null) {
                            mAction.call(bitmap);
                        }
                    }
                }
            }
        }

    }

    private interface Action<T> {
        void call(T t);
    }
}
