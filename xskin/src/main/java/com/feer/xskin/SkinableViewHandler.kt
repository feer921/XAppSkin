package com.feer.xskin

import android.view.View

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * <P>DESC:
 * 可换肤的 [View] Handler
 * </p>
 * ******************(^_^)***********************
 */
class SkinableViewHandler(private val skinableView: View, private val skinableAttrList: List<SkinableAttrInfos>) {


    /**
     * 申请换肤
     * 注：外部每变换一次皮肤，都需要调用本方法,来重新加载皮肤资源
     */
    fun applySkin(){
        skinableAttrList.forEach {attrInfos ->
            XSkinManager.paintSkinOfTheView(skinableView, attrInfos)
        }
    }
}