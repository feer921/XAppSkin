# Android App内换肤框架

## 使用

1.初始化本框架SDK

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        XSkinManager.init(this,"")//初始化换肤SDK
    }
}
```

其中SDK的核心类为　**XSkinManager**, 其初始化方法为：

```kotlin
/**
  * 初始化换肤SDK
  * @param context Context
  * @param lastSkinPkgResPath 用户上次选择/切换的　皮肤资源包文件完整路径
  */
 fun init(context: Context, lastSkinPkgResPath: String) {
   mManager.initSdk(context, lastSkinPkgResPath)
 }
```

2. 当用户选择了一个皮肤资源后，App则通过网络下载的方式把资源包下载下来并存到指定路径，然后调用本框架的加载皮肤资源包的方法，即可完成换肤

   ```kotlin
   /**
    * 加载皮肤资源
    * @param theSkinResFilePath 携带皮肤资源的的文件(一般为Apk(插件Apk))完整路径
    */
    @SuppressLint("PrivateApi")
    fun loadSkinRes(context: Context, theSkinResFilePath: String) {
         Log.i(TAG,"--> loadSkinRes() theSkinResFilePath =  $theSkinResFilePath")
          if (theSkinResFilePath.isBlank()) {
              return
          }
      //.....
    }
   ```

3. 框架本向支持的可换肤的　View 的属性如下：

   ```kotlin
   private const val ATTR_NAME_BACKGROUND = "background"
   private const val ATTR_NAME_SRC = "src"
   private const val ATTR_NAME_TEXT_COLOR = "textColor"
   private const val ATTR_NAME_TEXT_COLOR_HINT = "textColorHint"
   private const val ATTR_NAME_TEXT = "text"
   
   "drawableLeft",
   "drawableStart",
   "drawableRight",
   "drawableEnd",
   "drawableBottom"
   ```

   如果　App有其他的需要支持的属性，则可以使用如下该当来添加：

   ```kotlin
   /**
    * 在本框架不满足默认的可支持换肤属性名称时，可在初始化时增加其他的属性名称
    */
   fun addSupportSkinableAttrName(attrName: String) {
      if (attrName.isNotBlank()) {
          mSupportSkinableAttrNames.add(attrName)
       }
   }
   ```

   默认场景下，使用上面的步骤，App就可以支持换肤功能了。