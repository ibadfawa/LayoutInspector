package com.mi.layoutinspector.inspect

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import com.mi.layoutinspector.ActivityInspector
import com.mi.layoutinspector.LayoutInspector
import com.mi.layoutinspector.R
import com.mi.layoutinspector.utils.screenIsPortrait
import com.mi.layoutinspector.viewinfos.ViewInfosPopupWindow

/**
 * create by niuxiaowei
 * date : 2021/7/30
 * 它与DeorView中android.R.id.content的子view是一一对应关系。它包含多个{@see ViewInspector}
 **/

@SuppressLint("ViewConstructor")
class ViewInspectors constructor(
    context: Context,
    private var childOfContentView: View,
    private val decorView: View?,
    private val activityInspector: ActivityInspector
) : FrameLayout(context) {

    private var viewInfos = mutableListOf<InspectViewInfo>()
    private val viewInfosPopupWindow =
        ViewInfosPopupWindow(decorView, PopupWindow.OnDismissListener {
            curInspectedView = null
            curViewInspector?.setSelecte(false)
            curViewInspector = null
        },activityInspector)
    private var curInspectedView: View? = null
    private var curViewInspector: ViewInspector? = null
    private var offsetY = -1
    private var offsetX = -1
    private var mPaint: Paint = Paint()

    private var preScreenOrientation = screenIsPortrait(context)

    init {
        mPaint.color = ContextCompat.getColor(context, R.color.li_color_34b1f3)
        mPaint.strokeWidth = 2.0f
        //InspectPage的点击事件拦截掉
        setOnClickListener {
            Log.i("InspectPage", "InspectPage  click")
        }
        visibility = View.GONE
    }

    private fun add(
        viewInfo: InspectViewInfo,
        parent: ViewInspector?
    ): ViewInspector {
        viewInfos.add(viewInfo)
        val isSetClick4View =
            !(viewInfo.view is ViewGroup && !LayoutInspector.isViewGroupShowViewAttributes)
        val view = ViewInspector(
            context,
            viewInfo.view,
            this,
            isSetClick4View,
            parent
        )
        val lp = LayoutParams(viewInfo.view.width, viewInfo.view.height)
        addView(view, lp)
        return view
    }

    private fun calOffset() {
        if (preScreenOrientation != screenIsPortrait(context)) {
            offsetX = -1
            offsetY = -1
        }
        preScreenOrientation = screenIsPortrait(context)
        if (offsetY == -1) {
            val location = IntArray(2)
            childOfContentView.getLocationOnScreen(location)
            offsetY = location[1]
            offsetX = location[0]
        }
    }

    fun showInspectorViews() {
        visibility = View.VISIBLE
        calOffset()
        removeAllViews()
        collectInspectItemViews()
    }

    fun hideInspectorViews() {
        visibility = View.GONE
        removeAllViews()
        viewInfos.clear()
    }

    private fun collectInspectItemViews() {
        if (childOfContentView is ViewGroup) {
            val parent = add(InspectViewInfo(childOfContentView), null)
            collectInspectItemViewsForViewGroup(childOfContentView as ViewGroup, parent)
        } else {
            add(InspectViewInfo(childOfContentView), null)
        }
    }


    private fun collectInspectItemViewsForViewGroup(
        view: ViewGroup,
        parent: ViewInspector? = null
    ) {
        view.let {
            var childCount = 0
            childCount = it.childCount
            for (index in 0 until childCount) {
                val child = it.getChildAt(index)
                if (child != null && child.visibility == View.VISIBLE) {
                    if (child is ViewGroup) {
                        val innerParent = add(InspectViewInfo(child), parent)
                        collectInspectItemViewsForViewGroup(child, innerParent)
                    } else {
                        add(InspectViewInfo(child), parent)
                    }
                }
            }
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val inspectItemView = getChildAt(i) as ViewInspector
            val viewInfo = viewInfos[i]
            val location = IntArray(2)
            viewInfo.view.getLocationOnScreen(location)
            val x = location[0] - offsetX// view距离 屏幕左边的距离（即x轴方向）
            val y = location[1] - offsetY // view距离 屏幕顶边的距离（即y轴方向）
            inspectItemView.apply {
                layout(x, y, x + viewInfo.view.width, y + viewInfo.view.height)
                isOutOfScreen =
                    x * y < 0 || x >= this@ViewInspectors.measuredWidth || y >= this@ViewInspectors.measuredHeight
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        for (i in 0 until childCount) {
            val inspectItemView = getChildAt(i)
            if (inspectItemView is ViewInspector) {
                inspectItemView.drawMargin(canvas, mPaint)
            }
        }
        super.dispatchDraw(canvas)
    }


    fun showViewInfosPopupWindow(view: View, viewInspector: ViewInspector) {
        curInspectedView = view
        curViewInspector = viewInspector
        viewInfosPopupWindow.showViewInfos(context, view, viewInspector)
    }

    fun hideViewInfosPopupWindow() {
        curInspectedView = null
        viewInfosPopupWindow.hideViewInfos()
    }

    fun curInspectedView(): View? {
        return curInspectedView
    }

    data class InspectViewInfo(val view: View)

}