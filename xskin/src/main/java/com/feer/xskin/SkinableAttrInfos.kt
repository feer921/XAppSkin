package com.feer.xskin

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * <P>DESC:
 * 可支持皮肤的属性信息
 * 作用：对于 给 View 设置的 一个属性时，该属性对应的 字段信息
 * </p>
 * ******************(^_^)***********************
 */
class SkinableAttrInfos {

    companion object Infos {
         const val RES_TYPE_STRING = "string"
         const val RES_TYPE_DRAWABLE = "drawable"
//         const val RES_TYPE_STYLE = "style"
         const val RES_TYPE_COLOR = "color"
//         const val RES_TYPE_DIMEN = "dimen"
//         const val RES_TYPE_ANIM = "anim"
//         const val RES_TYPE_MENU = "menu"
    }
    /**
     * 当前的属性名称,eg.: "textColor"
     */
    var attrName = ""

    /**
     * 当前的属性的值的 资源 ID,eg.: R.String.app_name 对应的 id值
     */
    var attrValueResId = 0

    /**
     * [attrValueResId] 的值所对应的 资源类型，eg."string"/"drawable"/"color"
     */
    var valueResTypeName = ""




}