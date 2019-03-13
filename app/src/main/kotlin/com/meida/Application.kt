/**
 * created by 小卷毛, 2018/12/17
 * Copyright (c) 2018, 416143467@qq.com All Rights Reserved.
 * #                   *********                            #
 * #                  ************                          #
 * #                  *************                         #
 * #                 **  ***********                        #
 * #                ***  ****** *****                       #
 * #                *** *******   ****                      #
 * #               ***  ********** ****                     #
 * #              ****  *********** ****                    #
 * #            *****   ***********  *****                  #
 * #           ******   *** ********   *****                #
 * #           *****   ***   ********   ******              #
 * #          ******   ***  ***********   ******            #
 * #         ******   **** **************  ******           #
 * #        *******  ********************* *******          #
 * #        *******  ******************************         #
 * #       *******  ****** ***************** *******        #
 * #       *******  ****** ****** *********   ******        #
 * #       *******    **  ******   ******     ******        #
 * #       *******        ******    *****     *****         #
 * #        ******        *****     *****     ****          #
 * #         *****        ****      *****     ***           #
 * #          *****       ***        ***      *             #
 * #            **       ****        ****                   #
 */
package com.meida

import android.content.Context
import android.content.Intent
import android.support.multidex.MultiDexApplication
import com.clj.fastble.BleManager
import com.lzy.okgo.OkGo
import com.lzy.okgo.cache.CacheEntity
import com.lzy.okgo.cache.CacheMode
import com.lzy.okgo.cookie.CookieJarImpl
import com.lzy.okgo.cookie.store.DBCookieStore
import com.lzy.okgo.https.HttpsUtils
import com.lzy.okgo.interceptor.HttpLoggingInterceptor
import com.lzy.okgo.utils.OkLogger
import com.meida.base.getBoolean
import com.meida.base.getString
import com.meida.base.putBoolean
import com.meida.chatkit.createRoom
import com.meida.chatkit.setTeamDataProvider
import com.meida.chatkit.setUserInfoProvider
import com.meida.chatkit.setiCallUtil
import com.meida.freedconn.BuildConfig
import com.meida.freedconn.LoginActivity
import com.meida.freedconn.NetworkChatActivity
import com.meida.freedconn.R
import com.netease.nim.avchatkit.AVChatKit
import com.netease.nim.avchatkit.common.log.LogHelper
import com.netease.nim.avchatkit.config.AVChatOptions
import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.auth.LoginInfo
import com.netease.nimlib.sdk.avchat.constant.AVChatResCode
import com.netease.nimlib.sdk.util.NIMUtil
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.util.logging.Level

/**
 * 项目名称：FreedConn
 * 创建人：小卷毛
 * 创建时间：2018-12-17 10:57
 */
class Application : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        initOkGo()

        BleManager.getInstance().init(this@Application)
        BleManager.getInstance()
            .enableLog(BuildConfig.LOG_DEBUG)
            .setReConnectCount(1, 5000)
            .operateTimeout = 5000

        NIMClient.init(this@Application, loginInfo(), null)
        if (NIMUtil.isMainProcess(this@Application)) {
            initAVChatKit()
            if (getBoolean("isLogin")) AVChatKit.setAccount(getString("accid"))
            com.meida.chatkit.TeamAVChatProfile.sharedInstance().registerObserver(true)
        }
    }

    private fun initOkGo() {
        val builder = OkHttpClient.Builder()

        OkLogger.debug("FreedConn", BuildConfig.LOG_DEBUG)

        //log相关
        val loggingInterceptor = HttpLoggingInterceptor("OkGo")
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY) //log打印级别，决定了log显示的详细程度
        loggingInterceptor.setColorLevel(Level.INFO)                        //log颜色级别，决定了log在控制台显示的颜色
        builder.addInterceptor(loggingInterceptor)                          //添加OkGo默认debug日志

        //超时时间设置，默认60秒
        builder.readTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)    //全局的读取超时时间
        builder.writeTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)   //全局的写入超时时间
        builder.connectTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS) //全局的连接超时时间

        //自动管理cookie（或者叫session的保持），以下几种任选其一就行
        builder.cookieJar(CookieJarImpl(DBCookieStore(this))) //使用数据库保持cookie，如果cookie不过期，则一直有效
        // builder.cookieJar(CookieJarImpl(SPCookieStore(this)))        //使用sp保持cookie，如果cookie不过期，则一直有效
        // builder.cookieJar(CookieJarImpl(MemoryCookieStore()))        //使用内存保持cookie，app退出后，cookie消失

        //https相关设置，信任所有证书，不安全有风险
        val sslParams1 = HttpsUtils.getSslSocketFactory()
        builder.sslSocketFactory(sslParams1.sSLSocketFactory, sslParams1.trustManager)
        builder.hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier) //配置https的域名匹配规则

        // 其他统一的配置
        OkGo.getInstance().init(this@Application)     //必须调用初始化
            .setOkHttpClient(builder.build())              //建议设置OkHttpClient，不设置会使用默认的
            .setCacheMode(CacheMode.NO_CACHE)              //全局统一缓存模式，默认不使用缓存，可以不传
            .setCacheTime(CacheEntity.CACHE_NEVER_EXPIRE)  //全局统一缓存时间，默认永不过期，可以不传
            .retryCount = 3                                //全局统一超时重连次数，默认为三次，那么最差的情况会请求4次(一次原始请求，三次重连请求)，不需要可以设置为0
    }

    private fun loginInfo(): LoginInfo? {
        val isLogin = getBoolean("isLogin")
        return if (isLogin) LoginInfo(
            getString("accid"),
            getString("token")
        )
        else null
    }

    private fun initAVChatKit() {
        val avChatOptions = object : AVChatOptions() {
            override fun logout(context: Context) {
                if (getBoolean("isLogin")) {
                    startActivity(
                        Intent(context, LoginActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("offLine", true)
                        }
                    )

                    putBoolean("isLogin", false)
                }
            }
        }
        avChatOptions.entranceActivity = NetworkChatActivity::class.java
        avChatOptions.notificationIconRes = R.mipmap.ic_launcher_round
        AVChatKit.init(avChatOptions)
        AVChatKit.setContext(this@Application)

        //初始化日志系统
        LogHelper.init()

        //设置用户相关资料提供者
        setUserInfoProvider { }
        //设置群组数据提供者
        setTeamDataProvider { }
        //设置音视频操作接口
        setiCallUtil {
            outgoingTeamCall { context, roomName ->
                if (roomName.isNotEmpty()) {
                    createRoom(roomName, "") {
                        onSuccess {
                            NetworkChatActivity.startActivity(context, roomName)
                        }
                        onFailed { code ->
                            if (code == AVChatResCode.ERROR_CREATE_ROOM_ALREADY_EXIST) {
                                NetworkChatActivity.startActivity(context, roomName)
                            }
                        }
                    }
                }
            }
        }
    }

}