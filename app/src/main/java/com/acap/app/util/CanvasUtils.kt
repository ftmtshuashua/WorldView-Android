package com.acap.app.util

import android.graphics.*
import kotlin.math.*


/**
 * <pre>
 * Tip:
 *
 * @author A·Cap
 * @date 2021/12/15 14:35
 * </pre>
 */
object CanvasUtils {

    private var mCacheRect: Rect? = null
    private var mCacheRectF: RectF? = null
    private fun getCacheRect(): Rect {
        if (mCacheRect == null) mCacheRect = Rect()
        return mCacheRect!!
    }

    private fun getCacheRectF(): RectF {
        if (mCacheRectF == null) mCacheRectF = RectF()
        return mCacheRectF!!
    }


    //从中心点开始绘制文本
    fun drawTextAtCenter(canvas: Canvas, text: String, centerX: Float, centerY: Float, paint: Paint) {
        val fontMetrics = paint.fontMetrics
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            text,
            centerX,
            centerY - (fontMetrics.bottom + fontMetrics.ascent) / 2F,
            paint
        )
    }

    //从中心点开始绘制文本
    fun drawTextAtCenterLeft(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        paint: Paint
    ) {
        val fontMetrics = paint.fontMetrics
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            text,
            centerX,
            centerY - (fontMetrics.bottom + fontMetrics.ascent) / 2F,
            paint
        )
    }

    fun drawTextAtCenterRight(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        paint: Paint
    ) {
        val fontMetrics = paint.fontMetrics
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            text,
            centerX,
            centerY - (fontMetrics.bottom + fontMetrics.ascent) / 2F,
            paint
        )
    }

    fun drawTextAtCenterBottom(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        paint: Paint
    ) {
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, centerX, centerY, paint)
    }

    fun drawTextAtCenterTop(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        paint: Paint
    ) {
        val fontMetrics = paint.fontMetrics
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, centerX, centerY - (fontMetrics.bottom + fontMetrics.ascent), paint)
    }

    fun drawTextAtLeftBottom(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(text, x, y, paint)
    }

    fun drawTextAtRightBottom(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(text, x, y, paint)
    }

    fun drawBitmapAtCenter(
        canvas: Canvas,
        bitmap: Bitmap,
        centerX: Float,
        centerY: Float,
        paint: Paint
    ) {
        canvas.drawBitmap(bitmap, centerX - bitmap.width / 2F, centerY - bitmap.height / 2F, paint)
    }

    fun drawBitmapAtCenterRight(
        canvas: Canvas,
        bitmap: Bitmap,
        centerX: Float,
        centerY: Float,
        paint: Paint
    ) {
        canvas.drawBitmap(bitmap, centerX - bitmap.width, centerY - bitmap.height / 2F, paint)
    }

    fun drawBitmapAtLeftBottom(
        canvas: Canvas,
        bitmap: Bitmap,
        centerX: Float,
        centerY: Float,
        paint: Paint
    ) {
        canvas.drawBitmap(bitmap, centerX, centerY - bitmap.height, paint)
    }

    fun drawBitmapAtCenterTop(
        canvas: Canvas,
        bitmap: Bitmap,
        centerX: Float,
        centerY: Float,
        paint: Paint
    ) {
        canvas.drawBitmap(bitmap, centerX - bitmap.width / 2F, centerY, paint)
    }

    //测试中心点位置
    fun testCenter(canvas: Canvas, centerX: Float, centerY: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = 1F
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE

        var offset = 10F
        canvas.drawCircle(centerX, centerY, offset, paint)
        canvas.drawLine(centerX - offset, centerY, centerX + offset, centerY, paint)
        canvas.drawLine(centerX, centerY - offset, centerX, centerY + offset, paint)

    }


    /**
     *  绘制曲线 - 线段  (顺时针绘制)
     *
     *  @param start:线段的开始点
     *  @param end :线段的结束点
     *  @param center:线段的中间点 - 影响曲线的弧度
     *  @param path:填充线段
     */
    fun drawCurve(start: PointF, center: PointF, end: PointF, path: Path = Path()): Path {
        path.moveTo(start.x, start.y)
        val cicular = getCurve(start, center, end)   //内切圆弧
        if (cicular.startAngle > cicular.endAngle) {
            path.addArc(
                cicular.rect,
                cicular.startAngle.toFloat(),
                (360 - cicular.startAngle + cicular.endAngle).toFloat()
            )
        } else {
            path.addArc(
                cicular.rect,
                cicular.startAngle.toFloat(),
                (cicular.endAngle - cicular.startAngle).toFloat()
            )
        }
        return path
    }

    /** 获得曲线坐标数据 */
    fun getCurve(start: PointF, center: PointF, end: PointF): CurveResult {
        val rect = getCircleRect(start, center, end)
        val c_center = PointF(rect.centerX(), rect.centerY())
        return CurveResult(
            rect,
            getAngle(c_center, start),
            getAngle(c_center, end),
            getAngle(c_center, center)
        )
    }

    /**给定圆上3个点坐标，求出圆的外切矩形*/
    fun getCircleRect(start: PointF, center: PointF, end: PointF): RectF {
        val x1 = start.x.toDouble()
        val y1 = start.y.toDouble()
        val x2 = center.x.toDouble()
        val y2 = center.y.toDouble()
        val x3 = end.x.toDouble()
        val y3 = end.y.toDouble()

        val e: Double = 2 * (x2 - x1)
        val f: Double = 2 * (y2 - y1)
        val g: Double = x2 * x2 - x1 * x1 + y2 * y2 - y1 * y1
        val a: Double = 2 * (x3 - x2)
        val b: Double = 2 * (y3 - y2)
        val c: Double = x3 * x3 - x2 * x2 + y3 * y3 - y2 * y2

        //圆心与半径
        var X = (g * b - c * f) / (e * b - a * f)
        var Y = (a * g - c * e) / (a * f - b * e)
        var R = Math.sqrt((X - x1) * (X - x1) + (Y - y1) * (Y - y1))

        /*内切圆的范围
                270°
                 |
        180° ----|---->  0°
                 |
                90°
         */
        return RectF((X - R).toFloat(), (Y - R).toFloat(), (X + R).toFloat(), (Y + R).toFloat())
    }

    /**
     * 求点与圆心的角度
     * @param center:圆形所在位置
     * @param pointAtCircular:在圆上的某个点的坐标
     *
     * @return 返回圆上某点的角度
     */
    fun getAngle(center: PointF, pointAtCircular: PointF): Double {
        val angle = asin(
            abs(pointAtCircular.y - center.y).toDouble() / getPointDistance(
                center,
                pointAtCircular
            )
        ) * 180 / PI
        val pointAtQuadrant = getPointAtQuadrant(center, pointAtCircular)
        val d = when (pointAtQuadrant) {
            1 -> 360 - angle
            2 -> 180 + angle
            3 -> 180 - angle
            else -> angle
        }
        return d
    }

    /**求两点间的距离*/
    fun getPointDistance(A: PointF, B: PointF): Double = sqrt(
        (A.x - B.x).pow(2).toDouble() + (A.y - B.y).pow(
            2
        ).toDouble()
    )

    /**求点所在象限 (1 | 2 | 3 | 4)*/
    fun getPointAtQuadrant(center: PointF, point: PointF): Int {
        val x = point.x - center.x
        val y = -(point.y - center.y)

        if (x >= 0 && y >= 0) return 1
        if (x < 0 && y > 0) return 2
        if (x <= 0 && y <= 0) return 3
        return 4
    }

    data class CurveResult(
        val rect: RectF,
        val startAngle: Double,
        val endAngle: Double,
        val centerAngle: Double
    )

    //绘制折线
    fun drawLineChart(canvas: Canvas, points: List<PointF>, paint: Paint): List<PointF> {
        for (index in 1 until points.size) {
            val start = points[index - 1]
            val end = points[index]
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
        return points
    }


    /** 三阶曲线
     * *效果一般
     * *曲线从点中穿过
     */
    fun getLineGraph(path: Path?, points: List<PointF>?): Path {
        val mBesselCurveManager by lazy { BesselCurveManager() }
        var mPath = path ?: Path()
        mBesselCurveManager.reset(points)
        mBesselCurveManager.setPath(mPath)
        return mPath
    }


    /**
     * 控制点计算
     */
    fun getControlPointF(start: PointF, end: PointF, center: PointF): PointF {
        // 求 - PointF:c
        //  0.5 * ( 0.5 * (start.y + c.y) + 0.5 * (end.y + c.y)  ) = center.y
        //  ( 0.5 * (start.y + c.y) + 0.5 * (end.y + c.y)  ) = center.y * 2
        //  0.5 * ( (start.y + c.y) + (end.y + c.y)) = center.y * 2
        //  ( (start.y + c.y) + (end.y + c.y)) = center.y * 4
        //  c.y = (center.y * 4 - (start.y + end.y) ) * 0.5
        return PointF(center.x, (center.y * 4 - (start.y + end.y)) * 0.5F)
    }


}