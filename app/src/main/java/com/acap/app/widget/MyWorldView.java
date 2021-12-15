package com.acap.app.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;

import com.acap.app.R;
import com.acap.app.util.CanvasUtils;
import com.acap.app.util.ResourceUtils;
import com.acap.toolkit.constant.TimeConstants;
import com.acap.toolkit.phone.ScreenUtils;
import com.acap.toolkit.transform.TimeUtils;
import com.acap.toolkit.view.XPaint;
import com.acap.world.WorldBufferView;
import com.acap.world.WorldParameter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * Tip:
 *      15日TabLayout
 *
 * Created by ACap on 2021/2/23 17:09
 * </pre>
 */
public class MyWorldView extends WorldBufferView {
    public MyWorldView(Context context) {
        super(context);
        init();
    }

    public MyWorldView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyWorldView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private ResourceUtils.BitmapCache mBitmapCache = ResourceUtils.getBitmapCache();
    private ResourceUtils.DrawableCache mDrawableCache = ResourceUtils.getDrawableCache();
    private int mWidth_Tab = ScreenUtils.dip2px(60);
    private int mSize_TranslateBackground = ScreenUtils.dip2px(10);
    private List<Tab> mData;

    private XPaint mPaint = new XPaint();
    private Path mPathTem = new Path();//温度曲线
    private Path mPathBackground = new Path();//背景曲线

    //今日Tab
    private Tab mTabToDay;
    //触摸的Tab
    private Tab mTabPress;
    //选中的
    private Tab mTabSelected;
    private int mTabSelectedIndex = -1;

    private OnTabSelectedListener mOnTabSelectedListener;


    private void init() {
    }

    public void setData(List<Tab> mData) {
        this.mData = mData;

        //更新今日天气数据
        mTabToDay = getToDay();
        //刷新选中项
        if (mTabSelectedIndex != -1) {
            mTabSelected = getTabByIndex(mTabSelectedIndex);
        }

        measureWorldSize();
    }

    public static void setData(MyWorldView view, List<Tab> data) {
        view.setData(data);
    }

    //设置选中项
    public void setSelected(int index) {
        mTabSelectedIndex = index;
        setSelected(getTabByIndex(index));
    }


    private void setSelected(Tab tab) {
        if (tab == null) return;
        if (mTabSelected == tab) return;
        mTabSelected = tab;
        mTabSelectedIndex = tab.getIndex();
        smoothScrollTo(getTabScrollOffsetX(tab), 0);
        ViewCompat.postInvalidateOnAnimation(this);
        onTabChange(tab);
    }

    private void onTabChange(Tab tab) {
        if (mOnTabSelectedListener != null) {
            mOnTabSelectedListener.onTabSelected(tab);
        }
    }

    public void setOnTabSelectedListener(OnTabSelectedListener mOnTabSelectedListener) {
        this.mOnTabSelectedListener = mOnTabSelectedListener;
    }


    @Override
    protected void onMeasureWorldSize(WorldParameter world, int width, int height) {
        if (mData != null && !mData.isEmpty()) {
            world.setWorldSize(mData.size() * mWidth_Tab, height);

            List<PointF> array = new ArrayList<>();
            //求最大最小温度
            double t_min = Double.MAX_VALUE;
            double t_max = Double.MIN_VALUE;
            for (int i = 0; i < mData.size(); i++) {
                Tab tab = mData.get(i);
                t_min = Math.min(t_min, tab.getTemperature());
                t_max = Math.max(t_max, tab.getTemperature());
            }

            //位置信息配置
            for (int i = 0; i < mData.size(); i++) {
                Tab tab = mData.get(i);
                tab.setSize(i, mWidth_Tab, world.getWorldHeight(), t_min, t_max);
                array.add(new PointF(tab.getCenterX(), tab.cy_temperature));
            }
            array.add(0, new PointF(0, mData.get(0).cy_temperature));
            array.add(new PointF(world.getWorldWidth(), mData.get(mData.size() - 1).cy_temperature));

            mPathTem.reset();
            mPathBackground.reset();
            CanvasUtils.INSTANCE.getLineGraph(mPathTem, array);
            CanvasUtils.INSTANCE.getLineGraph(mPathBackground, array);
            mPathBackground.lineTo(world.getWorldWidth(), 0);
            mPathBackground.lineTo(0, 0);
            mPathBackground.close();
        } else {
            super.onMeasureWorldSize(world, width, height);
        }
    }


    @Override
    protected void onDrawWorldBuffer(Canvas canvas) {
        super.onDrawWorldBuffer(canvas);
        List<Tab> array = mData;
        if (array == null || array.isEmpty()) return;
        Tab toDay = mTabToDay;

        //背景
        canvas.save();
        canvas.translate(0, mSize_TranslateBackground);
        mPaint.setStyle(PS_BackgroundLight);
        canvas.drawPath(mPathBackground, mPaint);
        canvas.restore();

        mPaint.setStyle(PS_Background);
        canvas.drawPath(mPathBackground, mPaint);


        for (int i = 0; i < array.size(); i++) {
            Tab tab = array.get(i);
            if (tab == null) continue;
            int alpha = 255;
            if (toDay != null) {
                alpha = i < toDay.index ? (int) (255 * 0.48f) : 255;
            }
            //周
            mPaint.setStyle(PS_Week);
            mPaint.setAlpha(alpha);
            CanvasUtils.INSTANCE.drawTextAtCenter(canvas, tab.getWeek(), tab.getCenterX(), tab.cy_week, mPaint);
            //日期
            mPaint.setStyle(PS_Date);
            mPaint.setAlpha(alpha);
            CanvasUtils.INSTANCE.drawTextAtCenter(canvas, tab.getDate(), tab.getCenterX(), tab.cy_date, mPaint);
            //图标
            CanvasUtils.INSTANCE.drawBitmapAtCenter(canvas, mBitmapCache.get(tab.getIco()), tab.getCenterX(), tab.cy_ico, mPaint);
        }

        //温度曲线
        canvas.save();
        if (toDay != null) {
            canvas.clipRect(0f, 0f, toDay.getCenterX(), getHeight());
        }
        mPaint.setStyle(PS_PathTemDotted);
        canvas.drawPath(mPathTem, mPaint);
        canvas.restore();
        canvas.save();
        if (toDay != null) {
            canvas.clipRect(toDay.getCenterX(), 0, getWorldParams().getWorldWidth(), getHeight());
        }
        mPaint.setStyle(PS_PathTem);
        canvas.drawPath(mPathTem, mPaint);
        canvas.restore();


        //温度点
        mPaint.setStyle(PS_P_1);
        for (int i = 0; i < array.size(); i++) {
            Tab tab = array.get(i);
            canvas.drawCircle(tab.getCenterX(), tab.cy_temperature, ScreenUtils.dip2px(3), mPaint);
        }
        if (toDay != null) {
            mPaint.setStyle(PS_P_1);
            canvas.drawCircle(toDay.getCenterX(), toDay.cy_temperature, ScreenUtils.dip2px(5), mPaint);
            mPaint.setStyle(PS_P_2);
            canvas.drawCircle(toDay.getCenterX(), toDay.cy_temperature, ScreenUtils.dip2px(3), mPaint);
        }
    }

    @Override
    protected void onDrawWorldAnimation(Canvas canvas) {
        super.onDrawWorldAnimation(canvas);

        //触摸
        if (mTabPress != null && mTabPress != mTabSelected) {
            RectF mRect = mTabPress.mRect;
            Drawable drawable = mDrawableCache.get(R.drawable.shape_day_15_tab_layout_press);
            drawable.setBounds((int) mRect.left, (int) mRect.top, (int) mRect.right, (int) mRect.bottom);
            drawable.draw(canvas);
        }

        //选中
        if (mTabSelected != null) {
            RectF mRect = mTabSelected.mRect;
            Drawable drawable = mDrawableCache.get(R.drawable.shape_day_15_tab_layout_press);
            drawable.setBounds((int) mRect.left, (int) mRect.top, (int) mRect.right, (int) mRect.bottom);
            drawable.draw(canvas);
        }

    }

    @Override
    protected void onPressStateChange(boolean press, float x, float y) {
        super.onPressStateChange(press, x, y);
        if (press) {
            mTabPress = getTabByPoint(x, y);
        } else {
            mTabPress = null;
        }
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    protected void onSingleTap(float x, float y) {
        super.onSingleTap(x, y);
        Tab tab = getTabByPoint(x, y);
        if (mTabSelected == tab) return;
        setSelected(tab);
    }

    //数据对象
    public static final class Tab implements Parcelable {
        private long time;
        private int ico;
        private double temperature;


        public Tab(long time, int ico, double temperature) {
            this.time = time;
            this.ico = ico;
            this.temperature = temperature;
        }

        protected Tab(Parcel in) {
            time = in.readLong();
            ico = in.readInt();
            temperature = in.readDouble();
            mRect = in.readParcelable(RectF.class.getClassLoader());
            cy_week = in.readFloat();
            cy_date = in.readFloat();
            cy_ico = in.readFloat();
            cy_temperature = in.readFloat();
            index = in.readInt();
        }

        public static final Creator<Tab> CREATOR = new Creator<Tab>() {
            @Override
            public Tab createFromParcel(Parcel in) {
                return new Tab(in);
            }

            @Override
            public Tab[] newArray(int size) {
                return new Tab[size];
            }
        };

        public String getWeek() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String times = TimeUtils.millis2String(time, format);
            if (times.equals(TimeUtils.millis2String(TimeUtils.getNowMills(), format))) {
                return "今天";
            }
            if (times.equals(TimeUtils.millis2String(TimeUtils.getNowMills() - TimeConstants.DAY, format))) {
                return "昨天";
            }
            if (times.equals(TimeUtils.millis2String(TimeUtils.getNowMills() + TimeConstants.DAY, format))) {
                return "明天";
            }
            return TimeUtils.getChineseWeek(time);
        }

        public String getDate() {
            return TimeUtils.millis2String(time, new SimpleDateFormat("MM/dd"));
        }

        public int getIco() {
            return ico;
        }

        public double getTemperature() {
            return temperature;
        }

        //位置参数
        private RectF mRect;
        private float cy_week;
        private float cy_date;
        private float cy_ico;
        private float cy_temperature;
        private int index;

        private void setSize(int index, int width, int height, double temperature_min, double temperature_max) {
            this.index = index;
            mRect = new RectF(0, 0, width, height);
            mRect.offset(index * width, 0);

            float max = 216;
            cy_week = (height * 34 / max);
            cy_date = (height * 71 / max);
            cy_ico = (height * 123 / max);

            float t_min = (height * 164 / max);
            float t_max = (height * 194 / max);
            double r_t = (temperature - temperature_min) / (temperature_max - temperature_min);
            cy_temperature = (float) ((t_max - t_min) * (1 - r_t)) + t_min;

        }

        public float getCenterX() {
            if (mRect == null) return 0;
            return mRect.centerX();
        }

        public int getIndex() {
            return index;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(time);
            dest.writeInt(ico);
            dest.writeDouble(temperature);
            dest.writeParcelable(mRect, flags);
            dest.writeFloat(cy_week);
            dest.writeFloat(cy_date);
            dest.writeFloat(cy_ico);
            dest.writeFloat(cy_temperature);
            dest.writeInt(index);
        }
    }

    //监听选中变化
    public interface OnTabSelectedListener {
        void onTabSelected(Tab tab);
    }

    public Tab getTabByPoint(float x, float y) {
        if (mData == null) return null;
        for (Tab tab : mData) {
            if (tab.mRect.contains(x, y)) return tab;
        }

        return null;
    }

    public Tab getToDay() {
        if (mData == null) return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String f_today = TimeUtils.millis2String(TimeUtils.getNowMills(), format);
        for (int i = 0; i < mData.size(); i++) {
            Tab tab = mData.get(i);
            String f_tab = TimeUtils.millis2String(tab.time, format);
            if (f_today.equals(f_tab)) {
                mTabToDay = tab;
                return tab;
            }
        }
        return null;
    }

    public Tab getTabByIndex(int index) {
        if (mData == null) return null;
        if (index >= mData.size()) return null;
        return mData.get(index);
    }

    public int getIndexByTab(Tab tab) {
        if (tab == null) return -1;
        if (mData == null) return -1;
        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i) == tab) return i;
        }

        return -1;
    }

    //获得Tab的滚动偏移量
    private int getTabScrollOffsetX(Tab tab) {
        WorldParameter worldParams = getWorldParams();
        int viewWidth = worldParams.getViewWidth();
        return (int) (tab.getCenterX() - viewWidth / 2);
    }

    //背景
    private XPaint.PStyle PS_Background = (t, paint) -> {
        paint.setColor(0xff737FFF);
        paint.setStyle(Paint.Style.FILL);
    };
    private XPaint.PStyle PS_BackgroundLight = (t, paint) -> {
        paint.setColor(0xFFbecdff);
        paint.setStyle(Paint.Style.FILL);
    };
    //温度曲线
    private XPaint.PStyle PS_PathTem = (t, paint) -> {
        paint.setColor(0xff6165F7);
        paint.setStrokeWidth(ScreenUtils.dip2px(1.5f));
        paint.setStyle(Paint.Style.STROKE);
    };
    private XPaint.PStyle PS_PathTemDotted = new XPaint.PStyle() {
        private DashPathEffect dpe = new DashPathEffect(new float[]{ScreenUtils.dip2px(6f), ScreenUtils.dip2px(6f)}, 0f);

        @Override
        public void onChange(int type, Paint paint) {
            paint.setColor(0xff6165F7);
            paint.setStrokeWidth(ScreenUtils.dip2px(1.5f));
            paint.setStyle(Paint.Style.STROKE);
            paint.setPathEffect(dpe);
        }
    };
    //周
    private XPaint.PStyle PS_Week = (t, paint) -> {
        paint.setTextSize(ScreenUtils.sp2px(14));
        paint.setColor(0xFFffffff);
    };
    //日期
    private XPaint.PStyle PS_Date = (t, paint) -> {
        paint.setTextSize(ScreenUtils.sp2px(12));
        paint.setColor(0xFFffffff);
    };

    //Point
    private XPaint.PStyle PS_P_1 = (t, paint) -> {
        paint.setColor(0xff6165F7);
        paint.setStyle(Paint.Style.FILL);
    };
    private XPaint.PStyle PS_P_2 = (t, paint) -> {
        paint.setColor(0xffffffff);
        paint.setStyle(Paint.Style.FILL);
    };


    public static class TabLayoutOnPageChangeListener implements ViewPager.OnPageChangeListener {
        private final MyWorldView tabLayout;

        public TabLayoutOnPageChangeListener(MyWorldView tabLayout) {
            this.tabLayout = tabLayout;
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
        }

        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(final int position) {
            if (tabLayout == null) return;
            if (tabLayout.mData == null) return;
            if (tabLayout.mTabSelected == null) return;

            if (tabLayout.mTabSelected.getIndex() != position && position < tabLayout.mData.size()) {
                tabLayout.setSelected(position);
            }
        }
    }


}
