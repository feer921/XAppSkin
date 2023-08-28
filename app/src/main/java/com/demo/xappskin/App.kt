package com.demo.xappskin

import android.app.Application
import com.feer.xskin.XSkinManager

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * <P>DESC:
 * App
 * </p>
 * ******************(^_^)***********************
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        XSkinManager.init(this,"")
    }
}