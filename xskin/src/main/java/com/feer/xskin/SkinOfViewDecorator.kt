package com.feer.xskin

import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.lifecycle.Observer

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * <P>DESC:
 * 对 View装饰 皮肤 的装饰器
 * </p>
 * ******************(^_^)***********************
 */
open class SkinOfViewDecorator(other: IViewCreaterAndDecorator?) :
    AbsViewCreaterAndDecorator(other), Observer<String> {

//    protected val mSupportSkinAttrs by lazy(LazyThreadSafetyMode.NONE) {
//        hashSetOf(
//            "background",
//            "textColor",
//            "src",
//            "drawableLeft",
//            "drawableStart",
//            "drawableRight",
//            "drawableEnd",
//            "drawableBottom",
//        )
//    }

    /**
     * 可支持换肤的 View 的SkinableViewHandler 数据集
     */
    private val mSkinableViewHandlers by lazy(LazyThreadSafetyMode.NONE) {
        mutableListOf<SkinableViewHandler>()
    }

    /**
     * 当前 装饰器下，View的属性名称对应的属性的值所映射的在本App资源下的资源ID
     * eg.: "textColor" = "@color/R.color.white"
     * "@color/R.color.white" ==> @9999999 : 99099909(resIdOfApp)
     */
    private val mAttrValueMapResIdInApp by lazy(LazyThreadSafetyMode.NONE){
        mutableMapOf<String,Int>()
    }

    /**
     * 可在此方法的实现中来装饰 [theView]
     * 本类是用来换肤
     * @param theView 要被装饰的 [View]
     * @param attrsOfView [theView] 携带的相关 属性集
     */
    override fun decorateView(theView: View, attrsOfView: AttributeSet?) {
        //把 View 收集起来
        if (attrsOfView == null) {
            return
        }
        val attr: AttributeSet = attrsOfView
        val attCount = attr.attributeCount
        var attrInfoList: MutableList<SkinableAttrInfos>? = null

        for (index in 0 until attCount) {
            //attributeName = layout_width, attributeValue = "-1"
            //attributeName = layout_height, attributeValue = "-1"
            //textColor
            //当前的属性名称
            val attributeName = attr.getAttributeName(index)

            if (!XSkinManager.isSupportedAttrName(attributeName)) {
                continue
            }
            //获取属性的值
            val attributeValue = attr.getAttributeValue(index)

            Log.i("info", "-------------------------------------" +
                    "\n" +
                    "start: --> attributeName = $attributeName, attributeValue = $attributeValue")
            if (attrInfoList == null) {
                attrInfoList = mutableListOf()
            }
            //比如 color 以 #开头写的颜色，不可以用来换肤
            if (attributeValue.length < 2 || attributeValue.startsWith("#")) {
                continue
            }
            var attrResId = mAttrValueMapResIdInApp[attributeValue]
            if (attrResId == null) {
                //以？开头的表示使用属性
                attrResId = if (attributeValue.startsWith("?")) {
                    //background, attributeValue = ?16842836
                    val attrId = stringToInt(attributeValue.substring(1))
                    val typeValue = TypedValue()
                    theView.context.theme.resolveAttribute(attrId,typeValue,true)
                    typeValue.resourceId
                } else {
                    //正常情况下使用 @开头
                    stringToInt(attributeValue.substring(1))
                }
                mAttrValueMapResIdInApp[attributeValue] = attrResId
            }
            var valueResType = ""
            if (attrResId != 0){
                valueResType = theView.resources.getResourceTypeName(attrResId)
            }
            Log.i("info", "end: attrResId = $attrResId,--> valueResType = $valueResType" +
                    "\n--------------------------------")
            attrInfoList.add(SkinableAttrInfos().apply {
                attrName = attributeName
                attrValueResId = attrResId
                valueResTypeName = valueResType
            })
        }// for loop end

        if (!attrInfoList.isNullOrEmpty()) {
            val skinableViewHandler = SkinableViewHandler(theView, attrInfoList)
            //如果之前选择过皮肤了，则先变换一次
            if (XSkinManager.isSkinResLoaded()){
                skinableViewHandler.applySkin()
            }
            mSkinableViewHandlers.add(skinableViewHandler)
        }
        super.decorateView(theView, attrsOfView)
    }

//
//    /**
//     * 判断 某个属性是否是支持换肤的属性
//     * @param theAttrName String ,eg.: "src"
//     * @return true: 支持换肤;false： 不支持换肤的属性
//     */
//    protected open fun isSupportSkinableAttr(theAttrName: String): Boolean {
//        if (mSupportSkinAttrs.contains(theAttrName)) {
//            return true
//        }
//        return false
//    }
//
//    /**
//     * 添加可被支持换肤的属性名称
//     * @param theAttrName 属性名称，eg.: "color"
//     */
//    fun addSupportedAttr(theAttrName: String) {
//        if (theAttrName.isBlank()) {
//            return
//        }
//        mSupportSkinAttrs.add(theAttrName)
//    }


    /**
     * 当外部更换了皮肤时，让各 View 重新申请皮肤资源
     */
    private fun applySkin() {
        mSkinableViewHandlers.forEach {
            it.applySkin()
        }
    }

    /**
     * 在这里作为更换皮肤时的 观察者回调
     * Called when the data is changed.
     * @param t  The new data
     */
    override fun onChanged(t: String?) {
        Log.i("info","--> onChanged() t = $t")
        applySkin()
    }


    private fun stringToInt(str: String): Int {
        return try {
            str.toInt()
        } catch (ex: Exception) {
            0
        }
    }

}