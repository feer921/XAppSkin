package com.feer.xskin

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Method

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * <P>DESC:
 * X(多)皮肤的管理 者
 * </p>
 * ******************(^_^)***********************
 */
class XSkinManager private constructor() {
    /**
     * 当前App的资源包的 Context
     */
    private var mAppContext: Context? = null

    companion object Helper {

        private const val TAG = "XSkinManager"

        private val mManager: XSkinManager by lazy(LazyThreadSafetyMode.NONE) {
            XSkinManager()
        }

        private const val ATTR_NAME_BACKGROUND = "background"
        private const val ATTR_NAME_SRC = "src"
        private const val ATTR_NAME_TEXT_COLOR = "textColor"
        private const val ATTR_NAME_TEXT_COLOR_HINT = "textColorHint"
        private const val ATTR_NAME_TEXT = "text"

        private val mSupportSkinableAttrNames by lazy(LazyThreadSafetyMode.NONE) {
            hashSetOf(
                ATTR_NAME_BACKGROUND,
                ATTR_NAME_TEXT_COLOR,
                ATTR_NAME_TEXT_COLOR_HINT,
                ATTR_NAME_SRC,
                ATTR_NAME_TEXT,
                "drawableLeft",
                "drawableStart",
                "drawableRight",
                "drawableEnd",
                "drawableBottom",
            )
        }

        /**
         * 初始化换肤SDK
         * @param context Context
         * @param lastSkinPkgResPath 用户上次选择/切换的　皮肤资源包文件完整路径
         */
        fun init(context: Context, lastSkinPkgResPath: String) {
            mManager.initSdk(context, lastSkinPkgResPath)
        }

        /**
         * 注册 支持换肤 的精灵,即当前的 Activity 具备了换肤的功能
         * @param context Context
         * @param extraViewDecorator IViewCreaterAndDecorator 外部还可以指定 装饰器
         */
        fun registerSkinSpirit(
            context: Context?,
            extraViewDecorator: IViewCreaterAndDecorator? = null
        ) {
            if (context == null) {
                return
            }
            LayoutInflaterImp().apply {
                val skinOfViewDecorator = SkinOfViewDecorator(extraViewDecorator)
                mManager.addSkinChangedObserver(skinOfViewDecorator)
                this.mViewCreaterAndDecorator = skinOfViewDecorator
            }.injectFactory2(
                context, true
            )
        }

        /**
         * 在本框架不满足默认的可支持换肤属性名称时，可在初始化时增加其他的属性名称
         */
        fun addSupportSkinableAttrName(attrName: String) {
            if (attrName.isNotBlank()) {
                mSupportSkinableAttrNames.add(attrName)
            }
        }

        /**
         * 是否该属性是可支持换肤的属性
         * @param attrName View 的属性
         * @return true: 支持换肤; false：不支持
         */
        fun isSupportedAttrName(attrName: String): Boolean {
            if (attrName.isBlank()) {
                return false
            }
            return mSupportSkinableAttrNames.contains(attrName)
        }


        fun isSkinResLoaded() = mManager.mCurPkgResLoader != null

        @JvmStatic
        fun getManager() = mManager

        /**
         * 统一的 对要换肤的 View进行渲染皮肤资源
         * @param theView 要换肤的 View
         * @param skinableAttrInfos 支持的换肤属性信息
         */
        fun paintSkinOfTheView(theView: View, skinableAttrInfos: SkinableAttrInfos) {
            mManager.paintSkinOfTheView(theView, skinableAttrInfos)
        }

        fun getColor(@ColorRes colorResId: Int): Int? = mManager.getThemeColor(colorResId)

        fun getDrawable(@DrawableRes drawableResId: Int): Drawable? =
            mManager.getThemeDrawable(drawableResId)

        fun getText(@StringRes textResId: Int): CharSequence? = mManager.getThemeText(textResId)

    }

    private var isAddedObserver = false
    private val mSkinChangedObservers by lazy(LazyThreadSafetyMode.NONE) {
        isAddedObserver = true
        mutableListOf<WeakReference<Observer<String>>>()
    }

    private val mSkinResPathMapResLoader by lazy(LazyThreadSafetyMode.NONE){
        mutableMapOf<String,PackageResLoader>()
    }

    /**
     * 初始化SDK，如果有上次记录的 皮肤包资源，则先加载一次
     * @param context Context
     * @param lastSkinPkgResPath 上次换肤的资源包文件完整路径
     */
    fun initSdk(context: Context, lastSkinPkgResPath: String) {
        mAppContext = context.applicationContext
        if (mAppContext is Application){
            val app: Application = mAppContext as Application
            app.registerActivityLifecycleCallbacks(mAppActivityLifecycleCallback)
        }
        mAppThemeRes = mAppContext!!.resources
        if (lastSkinPkgResPath.isNotBlank()) {
            loadSkinRes(context, lastSkinPkgResPath)
        }
    }

    /**
     * 加载皮肤资源
     * @param theSkinResFilePath 携带皮肤资源的的文件(一般为Apk(插件Apk))完整路径
     */
    @SuppressLint("PrivateApi")
    fun loadSkinRes(context: Context, theSkinResFilePath: String): Boolean {
        Log.i(TAG, "--> loadSkinRes() theSkinResFilePath =  $theSkinResFilePath")
        if (theSkinResFilePath.isBlank()) {
            return false
        }
        val resFile = File(theSkinResFilePath)
        if (!resFile.exists()) {
            mSkinResPathMapResLoader.remove(theSkinResFilePath)
            return false
        }
        //已经加载过的皮肤资源
        val pkgResLoader = mSkinResPathMapResLoader[theSkinResFilePath]
        if (pkgResLoader != null) {
            mCurPkgResLoader = pkgResLoader
            notifySkinChanged(theSkinResFilePath)
            return true
        }
        val isLoadOk = try {
//            val classOfAsset = AssetManager::class.java
//            val assetManager: AssetManager = classOfAsset.newInstance()
//            val methodNamedAddPath: Method =
//                classOfAsset.getDeclaredMethod("addAssetPath", String::class.java)
//            methodNamedAddPath.isAccessible = true
//            methodNamedAddPath.invoke(assetManager, theSkinResFilePath)

            val resOfHost: Resources = context.resources
            if (mAppThemeRes == null) {
                mAppThemeRes = resOfHost
            }

            // 构建 皮肤资源包的 资源对象 Resources
            val packageManager = context.packageManager

//            val skinRes = Resources(assetManager, resOfHost.displayMetrics, resOfHost.configuration)
            var skinRes: Resources? = null
            //得到 皮肤资源包的 包名
            val skinPackageInfo = packageManager.getPackageArchiveInfo(
                theSkinResFilePath,
                PackageManager.GET_ACTIVITIES
            )?.apply {
                applicationInfo.sourceDir = theSkinResFilePath
                applicationInfo.publicSourceDir = theSkinResFilePath
                skinRes = packageManager.getResourcesForApplication(applicationInfo)
            }
            val skinPackageName = skinPackageInfo?.packageName ?: ""
            skinRes?.let {
                val finalSkinRes =
                    Resources(it.assets, resOfHost.displayMetrics, resOfHost.configuration)
                val skinLoader = PackageResLoader(finalSkinRes, skinPackageName)
                mCurPkgResLoader = skinLoader
                mSkinResPathMapResLoader[theSkinResFilePath] = skinLoader
                true
            } ?: false
        } catch (ex: Exception) {
            Log.e(TAG, "--> loadSkinRes() occur: $ex")
            false
        }
        if (isLoadOk) {
            notifySkinChanged(theSkinResFilePath)
        }
        return isLoadOk
    }

    /**
     * 当前换肤后的资源包加载器
     */
    private var mCurPkgResLoader: PackageResLoader? = null


    /**
     * 宿主App 默认的 主题资源包 对象,也为了当皮肤资源包对象加载不到对应的资源值时，可以兜底加载
     */
    private var mAppThemeRes: Resources? = null

    /**
     * 获取当前皮肤主题下的 颜色资源
     */
    fun getThemeColor(@ColorRes colorResId: Int): Int? {
        if (colorResId == 0){
            return null
        }
        return mCurPkgResLoader?.getColor(colorResId) ?: mAppThemeRes?.getColor(colorResId)
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    fun getThemeDrawable(@DrawableRes drawableResId: Int): Drawable? {
        if (drawableResId == 0){
            return null
        }
        return mCurPkgResLoader?.getDrawable(drawableResId) ?: mAppThemeRes?.getDrawable(
            drawableResId
        )
    }

    fun getThemeText(@StringRes textResId: Int): CharSequence? {
        if (textResId == 0){
            return ""
        }
        return mCurPkgResLoader?.getText(textResId) ?: mAppThemeRes?.getText(textResId) ?: ""
    }

    /**
     * 当换肤时，如果在皮肤资源包里找不到对应的资源值，是否需要置空替换
     * def = false: 当在皮肤资源包里找不到时，不替换
     */
    var isReplacedAtNotFoundSkinResValue = false

    /**
     * 根据 可支持皮肤的属性/资源 信息，
     * @param theView 要换肤的 View
     * @param skinableAttrInfos 支持的换肤属性信息
     */
    fun paintSkinOfTheView(theView: View, skinableAttrInfos: SkinableAttrInfos) {
        Log.i(TAG, "--> paintSkinOfTheView() skinableAttrInfos = $skinableAttrInfos")
        val isPainted = mSkinViewPainer?.paintSkinOfTheView(theView, skinableAttrInfos) ?: false
        if (isPainted) {
            return
        }
        val resId = skinableAttrInfos.attrValueResId
        if (resId == 0){//资源Id，没有获取到的情况，则不去更新皮肤资源了
            return
        }
        when (skinableAttrInfos.attrName) {
            ATTR_NAME_BACKGROUND -> {//要设置背景
                when (skinableAttrInfos.valueResTypeName) {
                    SkinableAttrInfos.RES_TYPE_COLOR -> {
                        var themeColor = getThemeColor(resId)
                        if (isReplacedAtNotFoundSkinResValue) {
                            themeColor = themeColor ?: 0
                        }
                        themeColor?.let {
                            theView.setBackgroundColor(it)
                        }
                    }

                    SkinableAttrInfos.RES_TYPE_DRAWABLE -> {
                        val themeDrawable = getThemeDrawable(resId)
                        if (isReplacedAtNotFoundSkinResValue) {
                            theView.setBackgroundDrawable(themeDrawable)
                        } else {//不替换的情况下，只有找到皮肤下的资源时才设置
                            if (themeDrawable != null) {
                                theView.setBackgroundDrawable(themeDrawable)
                            }
                        }
                    }
                    else -> {}
                }
            }
            ATTR_NAME_SRC -> {//前景
                if (theView is ImageView) {
                    val themeDrawable = getThemeDrawable(resId)
                    if (isReplacedAtNotFoundSkinResValue){
                        theView.setImageDrawable(themeDrawable)
                    } else {
                        themeDrawable?.let {
                            theView.setImageDrawable(it)
                        }
                    }
                }
            }
            ATTR_NAME_TEXT_COLOR -> {
                if (theView is TextView) {
                    val themeColor = getThemeColor(resId)
                    if (isReplacedAtNotFoundSkinResValue){
                        theView.setTextColor(themeColor ?: 0)
                    } else {
                        themeColor?.let {
                            theView.setTextColor(it)
                        }
                    }
                }
            }

            ATTR_NAME_TEXT -> {
                if (theView is TextView) {
                    val themeText = getThemeText(resId)
                    if (isReplacedAtNotFoundSkinResValue){
                        theView.text = themeText
                    } else {
                        themeText?.let {
                            theView.text = it
                        }
                    }
                }
            }

            ATTR_NAME_TEXT_COLOR_HINT -> {
                if (theView is TextView) {
                    val themeColor = getThemeColor(resId)
                    if (isReplacedAtNotFoundSkinResValue){
                        theView.setHintTextColor(themeColor ?: 0)
                    } else {
                        themeColor?.let {
                            theView.setHintTextColor(it)
                        }
                    }
                }
            }

            else -> {
            }
        }
    }

    /**
     * 添加 换肤变更的观察者
     */
    fun addSkinChangedObserver(observer: Observer<String>) {
        mSkinChangedObservers.add(WeakReference(observer))
    }

    /**
     * 重置皮肤
     */
    fun resetSkin() {
        mCurPkgResLoader = null
        notifySkinChanged()
    }

    /**
     * 通知皮肤切换了
     */
    private fun notifySkinChanged(data: String? = "") {
        if (isAddedObserver) {
            mSkinChangedObservers.forEach {
                it.get()?.onChanged(data)
            }
        }
    }

    /**
     * 根据宿主App里的 资源Id，检出该资源Id对应的资源名称(eg.:"white")以及资源类型(eg.: color/drawable)
     */
    fun checkOutResNameAndTypeOfResId(resIdInApp: Int): Pair<String, String>? {
        if (resIdInApp == 0){
            return null
        }
        return mAppThemeRes?.let {
            val resName = it.getResourceEntryName(resIdInApp)
            val resType = it.getResourceTypeName(resIdInApp)
            resName to resType
        }
    }

    /**
     * 外部来处理对 要换肤的 View/控件 进行渲染皮肤
     */
    var mSkinViewPainer: ISkinViewPainter? = null


    interface ISkinViewPainter {

        /**
         * 统一的 对要换肤的 View进行渲染皮肤资源
         * @param theView 要换肤的 View
         * @param skinableAttrInfos 支持的换肤属性信息
         * @return true: 外部处理了; false: 未处理，框架来进行默认的处理
         */
        fun paintSkinOfTheView(theView: View, skinableAttrInfos: SkinableAttrInfos): Boolean

    }

    /**
     * App中Activiyt 的生命周期监听者
     */
    private val mAppActivityLifecycleCallback by lazy(LazyThreadSafetyMode.NONE){
        object : Application.ActivityLifecycleCallbacks {
            /**
             * Called when the Activity calls [super.onCreate()][Activity.onCreate].
             */
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                registerSkinSpirit(activity)
            }

            /**
             * Called when the Activity calls [super.onStart()][Activity.onStart].
             */
            override fun onActivityStarted(activity: Activity) {
            }

            /**
             * Called when the Activity calls [super.onResume()][Activity.onResume].
             */
            override fun onActivityResumed(activity: Activity) {
            }

            /**
             * Called when the Activity calls [super.onPause()][Activity.onPause].
             */
            override fun onActivityPaused(activity: Activity) {
            }

            /**
             * Called when the Activity calls [super.onStop()][Activity.onStop].
             */
            override fun onActivityStopped(activity: Activity) {
            }

            /**
             * Called when the Activity calls
             * [super.onSaveInstanceState()][Activity.onSaveInstanceState].
             */
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            /**
             * Called when the Activity calls [super.onDestroy()][Activity.onDestroy].
             */
            override fun onActivityDestroyed(activity: Activity) {
            }

        }
    }
}