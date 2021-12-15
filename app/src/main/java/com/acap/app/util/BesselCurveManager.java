package com.acap.app.util;


import android.graphics.Path;
import android.graphics.PointF;

import com.acap.toolkit.cache.ObjectCacheUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * 贝塞尔曲线计算器
 *
 * <p>
 * <br/>
 * Author:Hope_LFB<br/>
 * Time:2020/12/21 9:51
 */

public class BesselCurveManager {
    private ObjectCacheUtils<Void, CurveControlPoint> mManager = new ObjectCacheUtils<>(r -> new CurveControlPoint());
    private List<CurveControlPoint> mControlPoints = new ArrayList<>();


    public BesselCurveManager() {
    }


    //清理曲线数据
    public void clear() {
        for (int index = mControlPoints.size() - 1; index >= 0; index--) {
            CurveControlPoint remove = mControlPoints.remove(index);
            mManager.recycle(remove);
        }
    }

    //计算曲线数据
    public void reset(List<PointF> pointFs) {
        clear();
        float x_p1, y_p1;
        float x_p2, y_p2;
        int size = pointFs.size() - 1;
        for (int i = 0; i < size; i++) {
            if (i == 0) {
                //第一断1曲线 控制点
                x_p1 = pointFs.get(i).x + (pointFs.get(i + 1).x - pointFs.get(i).x) / 4;
                y_p1 = pointFs.get(i).y + (pointFs.get(i + 1).y - pointFs.get(i).y) / 4;

                x_p2 = pointFs.get(i + 1).x - (pointFs.get(i + 2).x - pointFs.get(i).x) / 4;
                y_p2 = pointFs.get(i + 1).y - (pointFs.get(i + 2).y - pointFs.get(i).y) / 4;

            } else if (i == size - 1) {
                //最后一段曲线 控制点
                x_p1 = pointFs.get(i).x + (pointFs.get(i + 1).x - pointFs.get(i - 1).x) / 4;
                y_p1 = pointFs.get(i).y + (pointFs.get(i + 1).y - pointFs.get(i - 1).y) / 4;

                x_p2 = pointFs.get(i + 1).x - (pointFs.get(i + 1).x - pointFs.get(i).x) / 4;
                y_p2 = pointFs.get(i + 1).y - (pointFs.get(i + 1).y - pointFs.get(i).y) / 4;
            } else {
                x_p1 = pointFs.get(i).x + (pointFs.get(i + 1).x - pointFs.get(i - 1).x) / 4;
                y_p1 = pointFs.get(i).y + (pointFs.get(i + 1).y - pointFs.get(i - 1).y) / 4;

                x_p2 = pointFs.get(i + 1).x - (pointFs.get(i + 2).x - pointFs.get(i).x) / 4;
                y_p2 = pointFs.get(i + 1).y - (pointFs.get(i + 2).y - pointFs.get(i).y) / 4;
            }

            CurveControlPoint obtain = mManager.obtain();
            obtain.p_c1.set(x_p1, y_p1);
            obtain.p_c2.set(x_p2, y_p2);
            obtain.p_start.set(pointFs.get(i).x, pointFs.get(i).y);
            obtain.p_end.set(pointFs.get(i + 1).x, pointFs.get(i + 1).y);
            mControlPoints.add(obtain);
        }
    }

    //将计算好的曲线内容设置到Path中
    public void setPath(Path path) {
        int size = mControlPoints.size();
        if (size > 0) {
            PointF p_start = mControlPoints.get(0).p_start;
            path.moveTo(p_start.x, p_start.y);
            for (int i = 0; i < size; i++) {
                CurveControlPoint CCPoint = mControlPoints.get(i);
                PointF p_c1 = CCPoint.p_c1;
                PointF p_c2 = CCPoint.p_c2;
                PointF p_end = CCPoint.p_end;
                path.cubicTo(p_c1.x, p_c1.y, p_c2.x, p_c2.y, p_end.x, p_end.y);
            }
        }

    }


    //曲线控制点
    public static class CurveControlPoint {
        private PointF p_c1 = new PointF();
        private PointF p_c2 = new PointF();
        private PointF p_start = new PointF();  //曲线段的起点
        private PointF p_end = new PointF();  //曲线段的终点

        public PointF getP_c1() {
            return p_c1;
        }

        public void setP_c1(PointF p_c1) {
            this.p_c1 = p_c1;
        }

        public PointF getP_c2() {
            return p_c2;
        }

        public void setP_c2(PointF p_c2) {
            this.p_c2 = p_c2;
        }
    }

}
