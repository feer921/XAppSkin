package com.feer.xskin

import android.util.AttributeSet
import android.view.View

/**
 * 抽象的 [View] 的创建和 装饰器，
 * 注：本抽象类是直接交给了 [otherViewCreaterAndDecorator] 来处理
 */
abstract class AbsViewCreaterAndDecorator(protected val otherViewCreaterAndDecorator: IViewCreaterAndDecorator?) :
    IViewCreaterAndDecorator {

    /**
     * 根据 [View]的 名称 创建出相应的 [View]
     * @param viewName 当前的 [View]的名称，可能是 全包名名称，eg.:"com.xx.xx.XView"
     * @return View?
     */
    override fun createViewByName(viewName: String): View? {
        return otherViewCreaterAndDecorator?.createViewByName(viewName)
    }

    /**
     * 可在此方法的实现中来装饰 [theView]
     * @param theView 要被装饰的 [View]
     * @param attrsOfView [theView] 携带的相关 属性集
     */
    override fun decorateView(theView: View, attrsOfView: AttributeSet?) {
        otherViewCreaterAndDecorator?.decorateView(theView,attrsOfView)
    }
}