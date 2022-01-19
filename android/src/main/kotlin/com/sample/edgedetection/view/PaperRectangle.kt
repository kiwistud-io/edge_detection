package com.sample.edgedetection.view

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.sample.edgedetection.processor.Corners
import com.sample.edgedetection.processor.TAG
import org.opencv.core.Point
import org.opencv.core.Size
import kotlin.math.abs
import kotlin.math.roundToInt


class PaperRectangle : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributes: AttributeSet) : super(context, attributes)
    constructor(context: Context, attributes: AttributeSet, defTheme: Int) : super(
            context,
            attributes,
            defTheme
    )

    private val rectPaint = Paint()
    private val circlePaint = Paint()
    private var ratioX: Double = 1.0
    private var ratioY: Double = 1.0
    private var tl: Point = Point()
    private var tr: Point = Point()
    private var br: Point = Point()
    private var bl: Point = Point()
    private val path: Path = Path()
    private var point2Move = Point()
    private var cropMode = false
    private var latestDownX = 0.0F
    private var latestDownY = 0.0F
    //이미지 좌우의 빈 공간 px(white space)
    private var diffWidth = 0.0F
    //이미지 상하의 빈 공간 px(white space)
    private var diffHeight = 0.0F

    init {
        rectPaint.color = Color.parseColor("#1CBF94")
        rectPaint.isAntiAlias = true
        rectPaint.isDither = true
        rectPaint.strokeWidth = 6F
        rectPaint.style = Paint.Style.FILL_AND_STROKE
        rectPaint.strokeJoin = Paint.Join.ROUND    // set the join to round you want
        rectPaint.strokeCap = Paint.Cap.ROUND      // set the paint cap to round too
        rectPaint.pathEffect = CornerPathEffect(10f)

        circlePaint.color = Color.parseColor("#1CBF94")
        circlePaint.isDither = true
        circlePaint.isAntiAlias = true
        circlePaint.strokeWidth = 4F
        circlePaint.style = Paint.Style.STROKE
    }

    fun onCornersDetected(corners: Corners) {
        ratioX = corners.size.width.div(measuredWidth)
        ratioY = corners.size.height.div(measuredHeight)
        tl = corners.corners[0] ?: Point()
        tr = corners.corners[1] ?: Point()
        br = corners.corners[2] ?: Point()
        bl = corners.corners[3] ?: Point()

        Log.i(TAG, "POINTS ------>  $tl corners")

        resize()
        path.reset()
        path.moveTo(tl.x.toFloat(), tl.y.toFloat())
        path.lineTo(tr.x.toFloat(), tr.y.toFloat())
        path.lineTo(br.x.toFloat(), br.y.toFloat())
        path.lineTo(bl.x.toFloat(), bl.y.toFloat())
        path.close()
        invalidate()
    }

    fun onCornersNotDetected() {
        path.reset()
        invalidate()
    }

    fun onCorners2Crop(corners: Corners?, size: Size?) {
        if (size == null) {
            return
        }

        cropMode = true
        tl = corners?.corners?.get(0) ?: Point(size.width * 0.1, size.height * 0.1)
        tr = corners?.corners?.get(1) ?: Point(size.width * 0.9, size.height * 0.1)
        br = corners?.corners?.get(2) ?: Point(size.width * 0.9, size.height * 0.9)
        bl = corners?.corners?.get(3) ?: Point(size.width * 0.1, size.height * 0.9)
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        //exclude status bar height
        val statusBarHeight = getStatusBarHeight(context)
        //exclude navigation bar height
        val navigationBarHeight = getNavigationBarHeight(context)

        //가로가 이미지 영역애 꽉찼을 때의 값
        var extraHeight = 0.0

        //버튼 레이아웃 높이의 실제 픽셀(디바이스 해상도에 맞춘)
        val dp2px = convertDpToPx(context, 65)
        val fullHeight = displayMetrics.heightPixels - statusBarHeight - navigationBarHeight

        var isHeightStandard = false

        //이미지의 높이가 실제 뷰의 높이보다 크고 이미지의 너비가 디스플레이의 너비보다 작거나 같을 떄
        var standard = if (size.height > fullHeight && size.width <= displayMetrics.widthPixels) {
            println("size.height > fullHeight && size.width <= displayMetrics.widthPixels")
            isHeightStandard = true
            fullHeight.toDouble()
        } else {
            size.height
        }

        //높이 기준으로 비율을 정한다면
        val imageWidth: Double
        val imageHeight: Double
        if (isHeightStandard) {
            ratioY = size.height.div(standard)
            imageWidth = size.width.div(ratioY)
            imageHeight = displayMetrics.heightPixels.toDouble()
            ratioX = size.width.div(imageWidth)
        } else {
            extraHeight = dp2px.toDouble()
            standard = if (size.width > displayMetrics.widthPixels) {
                displayMetrics.widthPixels.toDouble()
            } else {
                size.width
            }

            ratioX = size.width.div(standard)
            imageHeight = size.height.div(ratioX)
            ratioY = size.height.div(imageHeight)
            imageWidth = standard
        }

        //디바이스의 너비에서 리사이징 된 이미지의 width 픽셀을 뺀 값
        val whiteSpaceWidth = displayMetrics.widthPixels - imageWidth
        //디바이스의 높이에서 리사이징 된 이미지의 height 픽셀을 뺀 값 (가로가 이미지 영역애 꽉찼을 땐 하단 버턴의 height 만큼 추가로 뺌)
        val whiteSpaceHeight = displayMetrics.heightPixels - imageHeight - extraHeight

        resize()
        diffWidth = (whiteSpaceWidth / 2.0).toFloat();
        diffHeight = (whiteSpaceHeight / 2.0).toFloat()
        movePoints()
    }

    fun getCorners2Crop(): List<Point> {
        reverseSize()
        return listOf(tl, tr, br, bl)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        rectPaint.color = Color.parseColor("#1CBF94")
        rectPaint.strokeWidth = 6F
        rectPaint.style = Paint.Style.STROKE
        canvas?.drawPath(path, rectPaint)

        rectPaint.color = Color.argb(60, 28, 191, 148)
        rectPaint.strokeWidth = 0F
        rectPaint.style = Paint.Style.FILL
        canvas?.drawPath(path, rectPaint)

        if (cropMode) {
            canvas?.drawCircle(tl.x.toFloat() + diffWidth, tl.y.toFloat()  + diffHeight, 20F, circlePaint)
            canvas?.drawCircle(tr.x.toFloat() + diffWidth, tr.y.toFloat() + diffHeight, 20F, circlePaint)
            canvas?.drawCircle(bl.x.toFloat() + diffWidth, bl.y.toFloat() + diffHeight, 20F, circlePaint)
            canvas?.drawCircle(br.x.toFloat() + diffWidth, br.y.toFloat() + diffHeight, 20F, circlePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (!cropMode) {
            return false
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                latestDownX = event.x
                latestDownY = event.y
                calculatePoint2Move(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                point2Move.x = (event.x - latestDownX) + point2Move.x
                point2Move.y = (event.y - latestDownY) + point2Move.y
                movePoints()
                latestDownY = event.y
                latestDownX = event.x
            }
        }
        return true
    }

    private fun calculatePoint2Move(downX: Float, downY: Float) {
        val points = listOf(tl, tr, br, bl)
        point2Move = points.minByOrNull { abs((it.x - downX).times(it.y - downY)) }
                ?: tl
    }

    private fun movePoints() {
        path.reset()
        path.moveTo(tl.x.toFloat() + diffWidth, tl.y.toFloat() + diffHeight)
        path.lineTo(tr.x.toFloat() + diffWidth, tr.y.toFloat() + diffHeight)
        path.lineTo(br.x.toFloat() + diffWidth, br.y.toFloat() + diffHeight)
        path.lineTo(bl.x.toFloat() + diffWidth, bl.y.toFloat() + diffHeight)
        path.close()
        invalidate()
    }

    private fun movePoints(width: Float) {
        println("movePoints - width: " + width)
        path.reset()
        path.moveTo(tl.x.toFloat() + width, tl.y.toFloat())
        path.lineTo(tr.x.toFloat() + width, tr.y.toFloat())
        path.lineTo(br.x.toFloat() + width, br.y.toFloat())
        path.lineTo(bl.x.toFloat() + width, bl.y.toFloat())
        path.close()
        invalidate()
    }


    private fun resize() {
        tl.x = tl.x.div(ratioX)
        tl.y = tl.y.div(ratioY)
        tr.x = tr.x.div(ratioX)
        tr.y = tr.y.div(ratioY)
        br.x = br.x.div(ratioX)
        br.y = br.y.div(ratioY)
        bl.x = bl.x.div(ratioX)
        bl.y = bl.y.div(ratioY)
    }

    private fun reverseSize() {
        tl.x = tl.x.times(ratioX)
        tl.y = tl.y.times(ratioY)
        tr.x = tr.x.times(ratioX)
        tr.y = tr.y.times(ratioY)
        br.x = br.x.times(ratioX)
        br.y = br.y.times(ratioY)
        bl.x = bl.x.times(ratioX)
        bl.y = bl.y.times(ratioY)
    }

    private fun getNavigationBarHeight(pContext: Context): Int {
        val resources = pContext.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    private fun getStatusBarHeight(pContext: Context): Int {
        val resources = pContext.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }


    fun convertDpToPx(context: Context, dp:Int): Int {
        val density= context.resources.displayMetrics.density
        return (dp * density).roundToInt()
    }
}