package com.feer.xskin

import android.util.AttributeSet
import android.view.View

/**
 * 可根据 View/控件 名称 进行 创建对应的[View]，并且可对相应的 [View] 进行相应的装饰的接口
 */
interface IViewCreaterAndDecorator {
    /**
     * 根据 [View]的 名称 创建出相应的 [View]
     * @param viewName 当前的 [View]的名称，可能是 全包名名称，eg.:"com.xx.xx.XView"
     * @return View?
     */
    fun createViewByName(viewName: String): View? = null

    /**
     * 可在此方法的实现中来装饰 [theView]
     * @param theView 要被装饰的 [View]
     * @param attrsOfView [theView] 携带的相关 属性集
     */
    fun decorateView(theView: View, attrsOfView: AttributeSet?)
}