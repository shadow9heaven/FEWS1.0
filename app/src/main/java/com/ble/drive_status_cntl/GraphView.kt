package com.bluetooth.bth_k2

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View

class GraphView(context: Context, attributeSet: AttributeSet ?=null): View(context, attributeSet) {

    private var dataSet = mutableListOf<DataPoint>()
    private var xMin = 0
    private var xMax = 0
    private var yMin = 0
    private var yMax = 0
    private var max_y = 50.toFloat()


    private val dataPointPaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val dataPointFillPaint = Paint().apply {
        color = Color.WHITE
    }

    private val dataPointLinePaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val axisLinePaint = Paint().apply {
        color = Color.DKGRAY
        strokeWidth = 4f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //if(dataSet.isNotEmpty()) {
            //yMax = dataSet.maxOf { it.yVal }

        //}
        dataSet.forEachIndexed { index, currentDataPoint ->
            //val realX = currentDataPoint.xVal.toRealX()
            var ydif = yMax -yMin
            if(ydif ==0)ydif =1
            else{}
            var tx = xMax - index
            val realX = tx.toFloat()/ xMax * width
            val realY = (((yMax - currentDataPoint.yVal).toFloat()/ydif) *height ).toFloat()

            //val startX = currentDataPoint.xVal.toRealX()
            //val startY = currentDataPoint.yVal.toRealY()

            if (index < dataSet.size - 1) {
                val nextDataPoint = dataSet[index + 1]

                //val endX = nextDataPoint.xVal.toRealX()
                val endX = (tx-1).toFloat()/ xMax * width
                val endY = (((yMax - nextDataPoint.yVal).toFloat()/ydif)*height)

                canvas.drawLine(realX, realY, endX, endY, dataPointLinePaint)
            }

            //canvas.drawCircle(realX, realY, 7f, dataPointFillPaint)
            //canvas.drawCircle(realX, realY, 7f, dataPointPaint)
        }

        canvas.drawLine(0f, 0f, 0f, height.toFloat(), axisLinePaint)
        canvas.drawLine(0f, height.toFloat(), width.toFloat(), height.toFloat(), axisLinePaint)
    }

    fun setData(newDataSet: List<DataPoint>) {
        xMin = newDataSet.minBy { it.xVal }?.xVal ?: 0
        xMax = newDataSet.maxBy { it.xVal }?.xVal ?: 0
        yMin = newDataSet.minBy { it.yVal }?.yVal ?: 0
        yMax = newDataSet.maxBy { it.yVal }?.yVal ?: 0

        //dataSet.clear()

        dataSet = mutableListOf<DataPoint>()
        dataSet.addAll(newDataSet)

        //yMin = dataSet.minOf{it.yVal}
        //yMax = dataSet.maxOf { it.yVal }



        postInvalidate()
    }

    private fun Int.toRealX() = toFloat() / xMax * width
    private fun Int.toRealY() = ((toFloat()-yMin)  / (yMax-yMin) )* height

    //private fun Int.toRealY() = ((toFloat()-yMin)  / (yMax-yMin) )* height

}

data class DataPoint(
        var xVal: Int,
        var yVal: Int
)