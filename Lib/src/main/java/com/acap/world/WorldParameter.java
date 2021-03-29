package com.acap.world;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * <pre>
 * 世界的基础参数
 * - 世界的范围
 * - 摄像机所在位置
 * - 摄像机的高度
 * - ...
 * Created by ACap on 2021/1/27 10:49
 * </pre>
 */
public final class WorldParameter implements Parcelable {
    //开启纵向滚动
    private static final int FLAG_VERTICAL_SCROLL_ENABLE = 0x1;
    //开启纵向滚动
    private static final int FLAG_HORIZONTAL_SCROLL_ENABLE = 0x1 << 1;


    private static final float NO_VALUE_FLOAT = Float.MIN_VALUE - 1; //用于表示未初始化任何值
    private static final int NO_VALUE_INT = Integer.MIN_VALUE - 1; //用于表示未初始化任何值


    /**
     * View的实际大小
     */
    private int view_width, view_height;
    /**
     * 世界相对与View的实际大小
     */
    private int world_width = NO_VALUE_INT, world_height = NO_VALUE_INT;

    /**
     * 世界边缘的可视范围,值为View宽度或高度的比例
     * <p>
     * 正数,表示相机可以超出屏幕范围
     * 负数,表示相机视角在屏幕之内
     */
    private RectF world_visible_range = new RectF();

    /**
     * 相机相对世界的视野大小
     */
    private RectF camera_size = new RectF();

    /**
     * 相机所在的位置
     */
    private int camera_x, camera_y;

    /**
     * 相机的高度比例，相机越高世界就越小
     */
    private float camera_z = 1f;

    private int mFlag;


    private void onChange() {
        if (getWorldWidth() > getViewWidth()) mFlag |= FLAG_HORIZONTAL_SCROLL_ENABLE;
        else mFlag &= ~FLAG_HORIZONTAL_SCROLL_ENABLE;

        if (getWorldHeight() > getViewHeight()) mFlag |= FLAG_VERTICAL_SCROLL_ENABLE;
        else mFlag &= ~FLAG_VERTICAL_SCROLL_ENABLE;
    }

    //横向滚动是否启用
    public boolean isHorizontalScrollEnabled() {
        return (mFlag & FLAG_HORIZONTAL_SCROLL_ENABLE) != 0;
    }

    //纵向滚动是否启用
    public boolean isVerticalScrollEnabled() {
        return (mFlag & FLAG_VERTICAL_SCROLL_ENABLE) != 0;
    }


    //判断世界大小是否初始化
    public boolean isInitWorldSize() {
        return world_width != NO_VALUE_INT && world_height != NO_VALUE_INT;
    }

    //设置世界的大小
    public void setWorldSize(int width, int height) {
        world_width = width;
        world_height = height;
        onChange();
    }


    public int getWorldWidth() {
        return (int) (world_width / getCameraZ());
    }

    public int getWorldHeight() {
        return (int) (world_height / getCameraZ());
    }

    public int getWorldCenterX() {
        return getWorldWidth() / 2;
    }

    public int getWorldCenterY() {
        return getWorldHeight() / 2;
    }


    //设置世界的可见范围
    public void setWorldVisibleRange(float left, float top, float right, float bottom) {
        world_visible_range.set(left, top, right, bottom);
    }

    //相机视野范围
    public void setViewSize(int width, int height) {
        view_width = width;
        view_height = height;
        setCameraZ(camera_z);
        onChange();
    }

    public int getViewWidth() {
        return view_width;
    }

    public int getViewHeight() {
        return view_height;
    }


    public void setCamera(int x, int y) {
        camera_x = x;
        camera_y = y;

    }

    public void setCameraOffset(float dx, float dy) {
        camera_x += dx;
        camera_y += dy;
    }

    //相机的高度 - 还未实现
    @Deprecated
    public void setCameraZ(float z) {
        camera_z = z;
        camera_size.set(0, 0, view_width, view_height);
    }

    public float getCameraZ() {
        return camera_z;
    }

    public float getCameraWidth() {
        return camera_size.width();
    }

    public float getCameraHeight() {
        return camera_size.height();
    }

    public float getCameraLeft() {
        return camera_x + camera_size.left;
    }

    public float getCameraTop() {
        return camera_y + camera_size.top;
    }

    public float getCameraRight() {
        return camera_x + camera_size.right;
    }

    public float getCameraBottom() {
        return camera_y + camera_size.bottom;
    }


    //相机范围限制
    public int getCameraRestrictLeft() {
        return (int) (-this.world_visible_range.left * getViewWidth());
    }

    public int getCameraRestrictTop() {
        return (int) (-this.world_visible_range.top * getViewHeight());
    }

    public int getCameraRestrictRight() {
        return (int) (getWorldWidth() - getCameraWidth() + this.world_visible_range.right * getViewWidth());
    }

    public int getCameraRestrictBottom() {
        return (int) (getWorldHeight() - getCameraHeight() + this.world_visible_range.bottom * getViewHeight());
    }

    //修正相机的位置
    public int trimCameraX(int x) {
        int cameraLeft = getCameraRestrictLeft();
        int cameraRight = getCameraRestrictRight();
        if (x < cameraLeft) x = cameraLeft;
        else if (x > cameraRight) x = cameraRight;


        return x;
    }

    public int trimCameraY(int y) {
        int cameraTop = getCameraRestrictTop();
        int cameraBottom = getCameraRestrictBottom();
        if (y < cameraTop) y = cameraTop;
        else if (y > cameraBottom) y = cameraBottom;
        return y;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    public WorldParameter() {
    }

    protected WorldParameter(Parcel in) {
        view_width = in.readInt();
        view_height = in.readInt();
        world_width = in.readInt();
        world_height = in.readInt();
        world_visible_range = in.readParcelable(RectF.class.getClassLoader());
        camera_size = in.readParcelable(RectF.class.getClassLoader());
        camera_x = in.readInt();
        camera_y = in.readInt();
        camera_z = in.readFloat();
        mFlag = in.readInt();
    }

    public static final Creator<WorldParameter> CREATOR = new Creator<WorldParameter>() {
        @Override
        public WorldParameter createFromParcel(Parcel in) {
            return new WorldParameter(in);
        }

        @Override
        public WorldParameter[] newArray(int size) {
            return new WorldParameter[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(view_width);
        dest.writeInt(view_height);
        dest.writeInt(world_width);
        dest.writeInt(world_height);
        dest.writeParcelable(world_visible_range, flags);
        dest.writeParcelable(camera_size, flags);
        dest.writeInt(camera_x);
        dest.writeInt(camera_y);
        dest.writeFloat(camera_z);
        dest.writeInt(mFlag);
    }
}

