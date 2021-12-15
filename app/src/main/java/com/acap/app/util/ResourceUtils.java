package com.acap.app.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import com.acap.toolkit.app.AppUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ResourceUtils {
    private ResourceUtils() {

    }

    public static Bitmap getBitmap(int id) {
        return BitmapFactory.decodeResource(AppUtils.getApp().getResources(), id);
    }

    public static int getColor(int id) {
        return AppUtils.getApp().getResources().getColor(id);
    }

    public static Drawable getDrawable(int id) {
        Drawable drawable = AppUtils.getApp().getResources().getDrawable(id);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    public static int getSize(int id) {
        return AppUtils.getApp().getResources().getDimensionPixelSize(id);
    }

    public static String getString(int id) {
        return AppUtils.getApp().getResources().getString(id);
    }

    public static BitmapCache getBitmapCache() {
        return new BitmapCache();
    }

    public static DrawableCache getDrawableCache() {
        return new DrawableCache();
    }

    //带有缓存的Bitmap对象
    public final static class BitmapCache {
        private final Map<Integer, Bitmap> mCache = new HashMap<>();
        private Filter mFilter;

        public BitmapCache setFilter(Filter mFilter) {
            this.mFilter = mFilter;
            return this;
        }

        public Bitmap get(int id) {
            if (mCache.get(id) == null) {
                Bitmap bitmap = getBitmap(id);
                if (mFilter != null) {
                    bitmap = mFilter.filter(bitmap);
                }
                mCache.put(id, bitmap);
            }
            return mCache.get(id);
        }

        public interface Filter {
            Bitmap filter(Bitmap bitmap);
        }

        public void release() {
            Iterator<Integer> iterator = mCache.keySet().iterator();
            while (iterator.hasNext()) {
                Integer key = iterator.next();
                Bitmap remove = mCache.remove(key);
                if (remove != null) {
                    remove.recycle();
                }
            }
        }

    }

    //带有缓存的Bitmap对象
    public final static class DrawableCache {
        private final Map<Integer, Drawable> mCache = new HashMap<>();

        public Drawable get(int id) {
            if (mCache.get(id) == null) {
                mCache.put(id, getDrawable(id));
            }
            return mCache.get(id);
        }

        public void release() {
            Iterator<Integer> iterator = mCache.keySet().iterator();
            while (iterator.hasNext()) {
                Integer key = iterator.next();
                Drawable remove = mCache.remove(key);
                if (remove != null) {
                    remove.setCallback(null);
                }
            }
        }

    }

}


