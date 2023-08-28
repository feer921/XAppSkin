package com.feer.xskin

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.LayoutInflaterCompat
import java.lang.reflect.Field

/**
 * LayoutInflater 的 [LayoutInflater.Factory2] 实现
 * 目的为：收集 Activity 或者说最终被 [LayoutInflater] 解析出来的 xml 中的 各View/控件
 */
open class LayoutInflaterImp : LayoutInflater.Factory2 {

    protected val mTag: String by lazy(LazyThreadSafetyMode.NONE) {
        javaClass.simpleName
    }

    private val mViewPrefixArray by lazy(LazyThreadSafetyMode.NONE){
        arrayOf(
            "android.widget.",
            "android.view.",
            "android.webkit."
        )
    }

    protected var mSrcLayoutInflater: LayoutInflater? = null

    /**
     * 在 [LayoutInflater] 中已经设置的 [LayoutInflater.Factory2]
     */
    protected var mExistFactory2: LayoutInflater.Factory2? = null

    /**
     * 在 [LayoutInflater] 中已经设置的 [LayoutInflater.Factory]
     */
    protected var mExistFactory1: LayoutInflater.Factory? = null

    /**
     * View 创建/装饰者
     */
    var mViewCreaterAndDecorator: AbsViewCreaterAndDecorator? = null

    /**
     * 根据 [LayoutInflater]在解析过程中[View]的 名称 直接构建/创建出 相关的 View
     * 以规避系统原来的 反射构建出 View 从以达到 加快 View的创建
     * 注：本类只尝试构建几种常用的View/控件，子类可以重写该方法，并增加其他类型 View的 构建，或者交由 [mViewCreaterAndDecorator]
     */
    protected open fun createViewByViewName(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        val v: View? = when (name) {
            "View" -> {
                View(context,attrs)
            }
            "FrameLayout" -> {
                FrameLayout(context,attrs)
            }
            "EditText" -> {
                AppCompatEditText(context, attrs)
            }
            "ImageView" -> {
                AppCompatImageView(context, attrs)
            }
            "LinearLayout" -> {
                LinearLayout(context, attrs)
            }
            "ViewStub" -> {
                ViewStub(context, attrs)
            }
            "Button" -> {
                AppCompatButton(context, attrs)
            }
            "androidx.constraintlayout.widget.ConstraintLayout" ->{
                ConstraintLayout(context, attrs)
            }
            "TextView" -> {
                AppCompatTextView(context, attrs)
            }
            else -> mViewCreaterAndDecorator?.createViewByName(viewName = name)
        }
        return v
    }

    /**
     * 给系统原生(当前的 Activity/[Context])的 [LayoutInflater] 注入自定义实现的 [LayoutInflater.Factory2]
     * 即本类实例
     * @param isNeedHookReplace true: 当正常情况下，替换失败时，需要进行反射 hook替换;
     *
     */
    fun injectFactory2(context: Context?, isNeedHookReplace: Boolean = false): Boolean {
        if (context == null) {
            return false
        }
//        if (context is AppCompatActivity) {
//            mDelegate = context.delegate
//        }
        val layoutInflater = LayoutInflater.from(context)
        mExistFactory2 = layoutInflater.factory2
        mSrcLayoutInflater = layoutInflater
        if (mExistFactory2 == null) {//表明当前 [Context] 下的 [LayoutInflater]还没有被设置 factory2
            debugLog("--> injectFactory2() mExistFactory2 is null before...")
            LayoutInflaterCompat.setFactory2(layoutInflater, this)
        } else {//如果当前[Context] 下的 [LayoutInflater]已被设置了[factory2],则只能反射 hook来替换了
            if (mExistFactory2 is LayoutInflaterImp) {//如果 [LayoutInflater]的 [factory2] 就是 本类的实例了，则可以不用处理了
                debugLog("--> injectFactory2() mExistFactory2 is just LayoutInflaterFactoryImp")
                return false
            }
            if (isNeedHookReplace) {
                reflectHookFactoryOf(layoutInflater)
            }
        }
        return true
    }

    protected open fun reflectHookFactoryOf(layoutInflater: LayoutInflater) {
        val classOfInflaterCompat = LayoutInflaterCompat::class.java
        val classOfLayoutInflater = LayoutInflater::class.java
        kotlin.runCatching {
            val sCheckedField = fieldOfClass(classOfInflaterCompat, "sCheckedField")
            sCheckedField?.setBoolean(layoutInflater, false)
            mExistFactory1 = layoutInflater.factory
            val mFactory = fieldOfClass(classOfLayoutInflater, "mFactory")
            mFactory?.set(layoutInflater, this)

            val mFactory2 = fieldOfClass(classOfLayoutInflater, "mFactory2")
            mFactory2?.set(layoutInflater, this)
        }.onFailure {
            Log.e(mTag, "--> reflectHookFactoryOf() occur: $it")
        }
    }

    private fun fieldOfClass(classOfObj: Class<*>, fieldName: String): Field? {
        return kotlin.runCatching {
            val field = classOfObj.getDeclaredField(fieldName)
            field.isAccessible = true
            field
        }.getOrNull()
    }

    /**
     * 是否需要输出 本类创建出一个View/控件的耗时
     * def = false
     */
    var mIsNeedPrintCreateViewTime = false

    /**
     * [LayoutInflater.Factory2] 接口方法
     */
    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        var startTs: Long? = null
        if (mIsNeedPrintCreateViewTime) {
            startTs = SystemClock.elapsedRealtime()
        }
        val v = combineCreateView(parent, name, context, attrs, from = "Factory2")
        onViewCreated(v, attrs)
        if (startTs != null) {
            val endTime = SystemClock.elapsedRealtime()
            val oneWasteTs = endTime - startTs
            debugLog("--> factory2: onCreateView() view name = $name,  waste time = $oneWasteTs")
        }
        return v
    }

    /**
     * [LayoutInflater.Factory] 接口方法
     */
    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        val v = combineCreateView(null, name, context, attrs, from = "Factory")
        onViewCreated(v, attrs)
        return v
    }

    /**
     * 综合的 创建 View
     */
    open fun combineCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet,
        from: String
    ): View? {
        //优先使用直接 new的方式创建对应的View
        var v = createViewByViewName(parent, name, context, attrs)
        debugLog("--> combineCreateView() parent = $parent,name = $name,from = $from,v = $v")
        if (v == null) {
//            v = mDelegate?.createView(parent, name, context, attrs)// 这个大多数情况为 null
//            Log.i(tag, "--> combineCreateView()  mDelegate = $mDelegate, v = $v")
            v = mExistFactory2?.onCreateView(parent, name, context, attrs)
        }
        if (v == null) {
            v = mExistFactory1?.onCreateView(name, context, attrs)
        }
        if (v == null) {
            v = createViewBySrcLayoutInflater(parent, name, context, attrs)
        }
//        if (BuildConfig.DEBUG) {
//            Log.d(tag, "--> combineCreateView() name = $name, v = $v")
//        }
        return v
    }

    /**
     * [android.view.LayoutInflater.Factory] 方案无效后，仍然使用 源 LayoutInflater 进行创建 View
     * 即:走的是 系统的反射创建 View的流程,利用 [LayoutInflater] 的 View createView(String name, String prefix, AttributeSet attrs)方法
     * @param parent
     * @param name 解析出的 View的名称
     * @param context Context
     * @param attrs AttributeSet
     * @return View?
     */
    protected open fun createViewBySrcLayoutInflater(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        var v: View? = null
        val srcLayoutInflater = mSrcLayoutInflater
        if (srcLayoutInflater != null) {
            try {
                //因为系统判断 prefix 是使用 != null来判断：Class.forName(prefix != null ? (prefix + name) : name, false,
                if (!name.contains(".")){
                    //则可能是系统的 View
                    for (s in mViewPrefixArray) {
                        v = srcLayoutInflater.createView(name,s,attrs)
                        if (v != null){
                            break
                        }
                    }
                } else {
                   v =  srcLayoutInflater.createView(name,null,attrs)
                }
//                v = srcLayoutInflater.createView(name, checkOutViewPrefixByName(name), attrs)
            } catch (ex: Exception) {
                debugLog("-> createViewBySrcLayoutInflater() occur: $ex", logLevel = 2)
            }
        }
        return v
    }

    /**
     * 因为系统判断 prefix 是使用 != null来判断：Class.forName(prefix != null ? (prefix + name) : name, false,
     * 所以 默认可以 return null
     */
    protected open fun checkOutViewPrefixByName(viewName: String): String? {
        return if (!viewName.contains(".")) { //没有 "xx.xx.xxView,一般表示 系统的 View
            if ("View" == viewName) {
                "android.view."
            } else "android.widget."
        } else null
    }

    /**
     * View/控件 的创建流程完成后的流程
     * 可交由 [mViewCreaterAndDecorator] 来进一步的 装饰
     */
    open fun onViewCreated(view: View?, attrs: AttributeSet?) {
        view?.let {
            mViewCreaterAndDecorator?.decorateView(theView = it, attrs)
        }
    }

    /**
     * 调试输出 Logcat 信息
     * @param logMsg String
     * @param logLevel 1: Log.i; 2:Log.e; 3:Log.w
     */
    private fun debugLog(logMsg: String,logLevel: Int = 1) {
        if (true) {
            when (logLevel) {
                1 -> {
                    Log.i(mTag, logMsg)
                }
                2 -> {
                    Log.e(mTag, logMsg)
                }
                3 ->{
                    Log.w(mTag,logMsg)
                }
                else -> {
                    Log.i(mTag, logMsg)
                }
            }
        }
    }

}