package com.acap.world;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import java.util.ArrayList;

/**
 * <pre>
 * Tip:
 *
 *
 * Created by ACap on 2021/2/24 11:15
 * </pre>
 */
class WorldSaveState extends View.BaseSavedState {
    ArrayList<Object> data = new ArrayList<>();
    private int mNativePtr = 0; // used by native code


    /**
     * 当View被回收的时候，将数据依次写入缓存中
     */
    public void write(Object o) {
        data.add(o);
    }

    /**
     * 在系统恢复View的时候从缓存中依次读取保存的数据
     */
    public <T> T read() {
        return (T) data.get(mNativePtr++);
    }

    /**
     * <pre>
     *    protected Parcelable onSaveInstanceState() {
     *        WorldSaveState state = new WorldSaveState(super.onSaveInstanceState());
     *        state.write(object);
     *        return state;
     *    }
     *
     * </pre>
     *
     * <pre>
     *     protected void onRestoreInstanceState(Parcelable state) {
     *          WorldSaveState ss = (WorldSaveState) state;
     *          super.onRestoreInstanceState(ss.getSuperState());
     *
     *         object isV =  ss.read();
     *      }
     * </pre>
     */
    public WorldSaveState(Parcelable superState) {
        super(superState);
    }


    private WorldSaveState(Parcel in) {
        super(in);
        in.readList(data, WorldSaveState.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeList(data);
    }

    public static final Creator<WorldSaveState> CREATOR
            = new Creator<WorldSaveState>() {
        public WorldSaveState createFromParcel(Parcel in) {
            return new WorldSaveState(in);
        }

        public WorldSaveState[] newArray(int size) {
            return new WorldSaveState[size];
        }
    };
}
