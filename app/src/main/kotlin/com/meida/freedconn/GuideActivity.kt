package com.meida.freedconn

import android.Manifest
import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.Gravity
import android.view.WindowManager
import com.meida.base.getBoolean
import com.meida.utils.ActivityStack
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.PermissionListener
import org.jetbrains.anko.*

/**
 * 不继承BaseActivity，解决打开显示空白的问题
 */
class GuideActivity : AppCompatActivity() {

    private var isReady: Boolean = false

    @SuppressLint("HandlerLeak")
    private var handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (isReady) quitGuide()
            else isReady = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //隐藏状态栏（全屏）
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        frameLayout {
            imageView {
                imageResource = R.mipmap.logo01
            }.lparams(height = dip(92)) {
                gravity = Gravity.CENTER
                bottomMargin = dip(60)
            }
        }

        ActivityStack.screenManager.pushActivity(this@GuideActivity)

        window.decorView.postDelayed({ handler.sendEmptyMessage(0) }, 2000)
        handler.sendEmptyMessage(0)

        AndPermission.with(this@GuideActivity)
            .permission(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE

            )
            .callback(object : PermissionListener {
                override fun onSucceed(requestCode: Int, grantPermissions: MutableList<String>) {
                    // handler.sendEmptyMessage(0)
                }

                override fun onFailed(requestCode: Int, deniedPermissions: MutableList<String>) {
                    toast(getString(R.string.permission_denied))
                    // onBackPressed()
                }
            }).start()
    }

    private fun quitGuide() {
        if (getBoolean("isLogin")) startActivity<MainActivity>()
        else startActivity<LoginActivity>()
        ActivityStack.screenManager.popActivities(this@GuideActivity::class.java)
    }
}
