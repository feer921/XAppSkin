package com.feer.xskin

import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * <P>DESC:
 * 资源包加载器
 * 目的为：一个资源包(皮肤包) 对应的资源 [Resources]
 * </p>
 * ******************(^_^)***********************
 */
class PackageResLoader(val pkgRes: Resources, val packageName: String) {

    /**
     * 资源Id 映射 对应的 值
     */
    private val mResMapValue by lazy(LazyThreadSafetyMode.NONE) {
        mutableMapOf<Int, Any>()
    }

    private val mAppResIdMapPkgResId by lazy(LazyThreadSafetyMode.NONE){
        mutableMapOf<Int, Int>()
    }

    fun getColor(@ColorRes colorRes: Int): Int? {
        var pkgResId = mAppResIdMapPkgResId[colorRes]
        if (pkgResId == null){
            pkgResId = checkOutPkgResId(colorRes)
            if (pkgResId != 0){
                mAppResIdMapPkgResId[colorRes] = pkgResId
            }
        }
        if (pkgResId != 0){
            return getCachedValue<Int>(pkgResId) ?: cacheValue(pkgResId, pkgRes.getColor(pkgResId))
        }
        return null
    }

    fun getDrawable(@DrawableRes drawableResId: Int): Drawable? {
        var pkgResId = mAppResIdMapPkgResId[drawableResId]
        if (pkgResId == null) {
            pkgResId = checkOutPkgResId(drawableResId)
            if (pkgResId != 0){
                mAppResIdMapPkgResId[drawableResId] = pkgResId
            }
        }
        if (pkgResId != 0){
            return getCachedValue(pkgResId) ?: cacheValue(pkgResId, pkgRes.getDrawable(pkgResId))
        }
        return null
    }

    /**
     * 注意：这里的 [textResId]是宿主 App下的资源Id，要转换成皮肤包下的资源Id
     */
    fun getText(@StringRes textResId: Int): CharSequence? {
        var pkgResId = mAppResIdMapPkgResId[textResId]
        if (pkgResId == null){
            pkgResId = checkOutPkgResId(textResId)
            if (pkgResId != 0){
                mAppResIdMapPkgResId[textResId] = pkgResId
            }
        }
        if (pkgResId != 0){
            return getCachedValue<String>(pkgResId)?: cacheValue(pkgResId,pkgRes.getText(pkgResId))
        }
        return null
    }


    private fun <V> getCachedValue(theKey: Int): V? {
        if (mResMapValue.containsKey(theKey)) {
            return mResMapValue[theKey] as? V
        }
        return null
    }

    private fun <V : Any> cacheValue(theKey: Int, v: V): V {
        mResMapValue[theKey] = v
        return v
    }

    private fun checkOutPkgResId(appResId: Int, appResName: String = "", appResType: String = ""): Int {
        if (appResId == 0) {
            return 0
        }
        var finalAppResName = appResName
        var finalAppResType = appResType
        if (finalAppResName.isBlank() || finalAppResType.isBlank()){
            checkOutResNameAndTypeInApp(appResId)?.let {
                finalAppResName = it.first
                finalAppResType = it.second
            }
        }
        return kotlin.runCatching {
            pkgRes.getIdentifier(finalAppResName, finalAppResType, packageName)
        }.getOrNull() ?: 0
    }

    private fun checkOutResNameAndTypeInApp(appResId: Int): Pair<String, String>? =
        XSkinManager.getManager()
            .checkOutResNameAndTypeOfResId(appResId)
}