package com.xiong.tbs_dynamic_plugin

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.platform.PlatformViewFactory

/** TbsDynamicPlugin */
class TbsDynamicPlugin(): FlutterPlugin, MethodCallHandler, ActivityAware {

  constructor(activity: Activity): this() {
    this.mActivity = activity
  }

  companion object {

    private const val CHANNEL_NAME = "tbs_dynamic"
    private const val CHANNEL_VIEW_NAME = "com.xiong.tbs_dynamic/x5webview"
    private var webViewFactory: WebViewFactory? = null

    //兼容旧版本注册
    @JvmStatic
    fun registerWith(registrar: PluginRegistry.Registrar) {
      registrar.activity()?.let {
        val plugin = TbsDynamicPlugin(it)

        webViewFactory = plugin.setupPluginViewFactory(
          registrar.messenger(),
          registrar.view(),
          it
        ) as? WebViewFactory
        webViewFactory?.let { factory ->
          registrar.addActivityResultListener(factory)
          registrar.platformViewRegistry().registerViewFactory(CHANNEL_VIEW_NAME, factory)
        }

        plugin.setupChannel(registrar.messenger(), registrar.context())
      }
    }
  }

  private var mActivity: Activity? = null
  private var channel : MethodChannel? = null
  private var mX5LoadResult: Result? = null
  private var mHandler: Handler = Handler(Looper.getMainLooper())

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when(call.method) {
      "initX5Core" -> {
        mX5LoadResult = result
        initX5Core()
      }
      "isX5Available" -> {
        result.success(checkX5CoreAvailable())
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  //动态初始化X5内核
  private fun initX5Core() {
    mActivity?.applicationContext?.let {
      X5LoadUtils.initX5Core(it, object : X5LoadUtils.SimpleX5CoreCallbackHandle() {
        override fun onX5LoadFailed(errorCode: Int) {
          super.onX5LoadFailed(errorCode)
          if (errorCode == X5LoadUtils.ERROR_UNKNOWN) { //因为Result只能回复一次，只有为无法正常下载的Error时才去真正返回结果到Flutter层
            mHandler.post { mX5LoadResult?.success(errorCode) }
          }
        }

        override fun onX5LoadSuccess() {
          super.onX5LoadSuccess()
          mHandler.post { mX5LoadResult?.success(0) }
        }
      })
    }
  }

  private fun checkX5CoreAvailable(): Boolean {
    return X5LoadUtils.hasX5CoreLoadComplete()
  }

  override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    webViewFactory = setupPluginViewFactory(binding.binaryMessenger, null, null) as? WebViewFactory
    webViewFactory?.let {
      val registerResult = binding.platformViewRegistry.registerViewFactory(CHANNEL_VIEW_NAME, it)
      Log.d("xiong","register WebViewPlugin Result = $registerResult")
    }

    setupChannel(binding.binaryMessenger, binding.applicationContext)
  }

  private fun setupPluginViewFactory(messenger: BinaryMessenger, containerView: View?, activity: Activity?): PlatformViewFactory {
    return if (activity != null) {
      WebViewFactory(messenger, containerView, activity)
    } else {
      WebViewFactory(messenger, containerView, mActivity)
    }
  }

  private fun setupChannel(messenger: BinaryMessenger, context: Context) {
    channel = MethodChannel(messenger, CHANNEL_NAME)
    channel?.setMethodCallHandler(this)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    mX5LoadResult = null
    channel?.setMethodCallHandler(null)
    channel = null
    mActivity = null
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    mActivity = binding.activity
    webViewFactory?.let {
      it.activity = mActivity
      binding.addActivityResultListener(it)
    }
  }

  override fun onDetachedFromActivityForConfigChanges() {
    mActivity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    mActivity = binding.activity
    webViewFactory?.let {
      it.activity = mActivity
    }
  }

  override fun onDetachedFromActivity() {
    mActivity = null
  }

}
