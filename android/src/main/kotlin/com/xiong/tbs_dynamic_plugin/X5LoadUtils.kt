package com.xiong.tbs_dynamic_plugin

import android.content.Context
import android.util.Log
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsDownloader
import com.tencent.smtt.sdk.TbsListener
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author xiong
 * @since  2021/12/6
 * @description X5加载工具类
 **/
object X5LoadUtils {
    private const val TAG = "X5LoadUtils"

    const val ERROR_MEET_FLOW_CONTROL = 110 //命中流控
    const val ERROR_ONLY_DOWNLOAD_WITH_WIFI = 111 //只能使用流量下载
    const val ERROR_UNKNOWN = -1

    private val mInit = AtomicBoolean(false)

    //处理偶现的流控问题
    fun initX5Core(context: Context, callback: IX5CoreCallback) {
        val canLoad = QbSdk.canLoadX5(context)
        mInit.set(canLoad)
        Log.d(TAG,"canLoad = $canLoad")

        val preInitCallback = object : QbSdk.PreInitCallback {
            override fun onCoreInitFinished() {
                Log.d(TAG,"onCoreInitFinished ")
            }

            override fun onViewInitFinished(p0: Boolean) {
                Log.d(TAG,"onViewInitFinished = $p0")
                mInit.set(p0)
                if (p0) {
                    callback.onX5LoadSuccess()
                } else {
                    callback.onX5LoadFailed(ERROR_UNKNOWN)
                }
            }
        }

        if (!canLoad) {
            QbSdk.setTbsListener(object : TbsListener {
                override fun onDownloadFinish(i: Int) {
                    Log.d(TAG,"onDownloadFinish = $i")
                    when(i) {
                        110 -> { //命中流控：onDownloadFinish = 110
                            callback.onX5LoadFailed(ERROR_MEET_FLOW_CONTROL)
                            doX5CoreDownload(context)
                        }
                        111 -> { //非Wifi情况执行下载
                            callback.onX5LoadFailed(ERROR_ONLY_DOWNLOAD_WITH_WIFI)
                            //TODO xiong -- 优化：流量提醒，是否使用流量下载内核
                        }
                    }
                }

                override fun onInstallFinish(i: Int) {
                    Log.d(TAG,"onInstallFinish = $i")
                    if (i == 200) {
                        doRealX5CoreInit(context, preInitCallback)
                    }
                }

                override fun onDownloadProgress(i: Int) {
                    Log.d(TAG,"onDownloadProgress = $i")
                }
            })
            doX5CoreDownload(context)
        } else {
            doRealX5CoreInit(context, preInitCallback)
        }
    }

    //启动下载
    fun doX5CoreDownload(context: Context) {
        if (TbsDownloader.isDownloading()) return

        QbSdk.reset(context)
        QbSdk.setDownloadWithoutWifi(true)
        TbsDownloader.startDownload(context)
    }

    //由于QbSdk.PreInit中做了限制，每次启动APP只能初始化一次，因此要想加载成功必须得等X5内核下载完成才能做初始化
    private fun doRealX5CoreInit(context: Context, preInitCallback: QbSdk.PreInitCallback) {
        QbSdk.initX5Environment(context, preInitCallback)
    }

    fun hasX5CoreLoadComplete(): Boolean = mInit.get()

    interface IX5CoreCallback {

        fun onX5LoadFailed(errorCode: Int)

        fun onX5LoadSuccess()
    }

    open class SimpleX5CoreCallbackHandle : IX5CoreCallback {
        override fun onX5LoadFailed(errorCode: Int) {
            Log.d(TAG,"onX5LoadFailed errorCode: $errorCode ${if (errorCode == ERROR_MEET_FLOW_CONTROL) ", 命中流控, 重新下载" else ""}")
        }

        override fun onX5LoadSuccess() {
            Log.d(TAG,"onX5LoadSuccess")
        }

    }
}