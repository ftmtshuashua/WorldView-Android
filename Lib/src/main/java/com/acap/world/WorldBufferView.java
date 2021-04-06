package com.acap.world;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
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

    private Bitmap mBuffer;
    private BufferBuilder mBufferBuilder;  //Buffer构造器
    private Paint mPaint = new Paint();

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
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearBuffer();
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
     * 清理缓冲数据
     */
    public void clearBuffer() {
        if (mBuffer != null) {
            mBuffer.recycle();
            mBuffer = null;
        }
    }

    /**
     * 刷新缓冲数据
     */
    public void refreshBuffer() {
        builder();
    }

    //开始绘制世界
    void drawWorld(Canvas canvas) {
        if (mBuffer == null) {
            onDrawWorldBufferBuilder(canvas);
        }

        //没有缓冲或者需要刷新时构建缓冲
        if (mBufferBuilder == null && mBuffer == null) {
            builder();
        }

        if (mBuffer != null) {
            canvas.drawBitmap(mBuffer, 0, 0, mPaint);
        }
        onDrawWorldForeground(canvas);
    }

    /**
     * 当缓冲还未构建完成时被调用
     *
     * @param canvas
     */
    protected void onDrawWorldBufferBuilder(Canvas canvas) {

    }

    /**
     * 再线程中构建缓冲
     */
    private void builder() {
        if (mBufferBuilder == null) {
            mBufferBuilder = new BufferBuilder();
            mBufferBuilder.setOnBufferBuildListener(bitmap -> {
                setBuffer(bitmap);
                mBufferBuilder = null;
            });
            mBufferBuilder.start();
        } else {
            mBufferBuilder.cancel();
            mBufferBuilder = null;
            builder();
        }
    }

    /**
     * 同步Buffer
     */
    private void setBuffer(Bitmap bitmap) {
        Bitmap oldbuffer = mBuffer;
        mBuffer = bitmap;
        postInvalidateAtThread();

        if (oldbuffer != null) {
            oldbuffer.recycle();
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
        private boolean isCancel = false;

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
            Bitmap bitmap = onCreateBuffer(params.getWorldWidth(), params.getWorldHeight());
            onDrawWorldBackground(new Canvas(bitmap));

            if (isCancel) {
                bitmap.recycle();
            } else {
                if (mAction != null) {
                    mAction.call(bitmap);
                }
            }
        }

        public void cancel() {
            isCancel = true;
        }
    }

    private interface Action<T> {
        void call(T t);
    }
}
