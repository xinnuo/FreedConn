package com.meida.freedconn

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.View
import com.netease.nim.avchatkit.AVChatKit
import com.netease.nimlib.sdk.auth.AuthServiceObserver
import com.meida.base.BaseActivity
import com.meida.base.getString
import com.meida.chatkit.getService
import com.meida.chatkit.observeOnlineStatus
import com.meida.model.RefreshMessageEvent
import com.meida.share.Const
import com.meida.utils.BluetoothHelper.getAdapter
import com.meida.utils.BluetoothHelper.getConnectedProfile
import com.meida.utils.BluetoothHelper.isBluetoothConnected
import com.meida.utils.BluetoothHelper.isBluetoothEnable
import com.meida.utils.DialogHelper
import com.meida.utils.getProfileProxy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity() {

    private var mDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setToolbarVisibility(false)
        init_title()

        setDeviceEnable(false)

        registerReceiver()
    }

    override fun init_title() {
        checkBluetoothState()

        /* 在线状态观察者 */
        getService<AuthServiceObserver>().observeOnlineStatus {
            if (it.wontAutoLogin()) {
                toast(getString(R.string.notify_offline))
                AVChatKit.getAvChatOptions().logout(baseContext)
            }
        }

        if (isBluetoothConnected()) {
            getAdapter()!!.getProfileProxy(this, getConnectedProfile()) {
                onServiceConnected { profile, proxy ->
                    val mDevices = proxy.connectedDevices
                    if (!mDevices.isNullOrEmpty()) {
                        val device = mDevices.first()
                        val deviceMac = device.address
                        if (deviceMac.startsWith(Const.MAC_HEADER_1)
                            || deviceMac.startsWith(Const.MAC_HEADER_2)
                            || deviceMac.startsWith(Const.MAC_HEADER_3)
                        ) {
                            main_check1.isChecked = false
                            main_check2.isChecked = false
                            setDeviceEnable(false)
                            setMultiEnable(false)
                        } else {
                            main_check3.isChecked = false
                            setDeviceEnable(false)
                            setMultiEnable(false)
                        }
                    }

                    getAdapter()!!.closeProfileProxy(profile, proxy)
                }
            }
        }
    }

    override fun doClick(v: View) {
        when (v.id) {
            R.id.main_device -> startActivity<DeviceActivity>()
            R.id.main_network -> {
                if (getString("pollcode").isEmpty()) startActivity<BindActivity>()
                else startActivity<NetworkActivity>()
            }
            R.id.main_setting -> startActivity<SettingActivity>()
        }
    }

    /** 判断蓝牙是否开启和连接 **/
    @SuppressLint("CheckResult")
    private fun checkBluetoothState() {
        mDisposable = Observable.timer(1, TimeUnit.SECONDS)
            .map { return@map isBluetoothEnable() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                when {
                    !it -> DialogHelper.showBluetoothDialog(baseContext) { hint ->
                        if (hint == "yes") startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }
                    !isBluetoothConnected() -> DialogHelper.showBluetoothDialog(
                        baseContext,
                        getString(R.string.bluetooth_disconnect)
                    ) { hint ->
                        if (hint == "yes") startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }
                }
            }
    }

    /** 多功能设备是否可用 **/
    private fun setDeviceEnable(isEnable: Boolean) {
        main_device.isClickable = isEnable
        main_device.setBackgroundResource(
            if (isEnable) R.mipmap.btn02_b
            else R.mipmap.btn02
        )
        main_device_img.setImageResource(
            if (isEnable) R.mipmap.icon08_b
            else R.mipmap.icon08
        )
        @Suppress("DEPRECATION")
        main_device_hint.setTextColor(
            resources.getColor(
                if (isEnable) R.color.blue_light
                else R.color.black_light
            )
        )
    }

    /** 多功能蓝牙是否可用 **/
    private fun setMultiEnable(isEnable: Boolean) {
        main_bluetooth.isClickable = isEnable
        main_bluetooth.setBackgroundResource(
            if (isEnable) R.mipmap.btn03_b
            else R.mipmap.btn03
        )
        main_bluetooth_img.setImageResource(
            if (isEnable) R.mipmap.icon09_b
            else R.mipmap.icon09
        )
        @Suppress("DEPRECATION")
        main_bluetooth_hint.setTextColor(
            resources.getColor(
                if (isEnable) R.color.blue_light
                else R.color.black_light
            )
        )
    }

    /** 注册电话状态监听(不可用) **/
    private fun registerPhoneStateListener() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        tm.listen(object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                when (state) {
                    //挂断
                    TelephonyManager.CALL_STATE_IDLE ->
                        EventBus.getDefault().post(RefreshMessageEvent("电话挂断"))
                    //接听(来电或去电)
                    TelephonyManager.CALL_STATE_OFFHOOK ->
                        EventBus.getDefault().post(RefreshMessageEvent("电话接听"))
                    //响铃
                    TelephonyManager.CALL_STATE_RINGING -> {
                    }
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
    }

    /** 注册广播 **/
    private fun registerReceiver() {
        val filter = IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        registerReceiver(mReceiver, filter)
    }

    /** 退出 **/
    private var exitTime: Long = 0

    override fun onBackPressed() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            toast(getString(R.string.quit))
            exitTime = System.currentTimeMillis()
        } else super.onBackPressed()
    }

    /** 注销广播 **/
    override fun finish() {
        super.finish()
        mDisposable?.dispose()
        try {
            unregisterReceiver(mReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 广播 BroadcastReceiver **/
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1)) {
                BluetoothA2dp.STATE_CONNECTED -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val deviceMac = device.address
                    if (deviceMac.startsWith(Const.MAC_HEADER_1)
                        || deviceMac.startsWith(Const.MAC_HEADER_2)
                        || deviceMac.startsWith(Const.MAC_HEADER_3)
                    ) {
                        main_check1.isChecked = false
                        main_check2.isChecked = false
                        setDeviceEnable(false)
                        setMultiEnable(false)
                    } else {
                        main_check3.isChecked = false
                        setDeviceEnable(false)
                        setMultiEnable(false)
                    }

                    EventBus.getDefault().post(RefreshMessageEvent("蓝牙连接"))
                }
                BluetoothA2dp.STATE_DISCONNECTING -> {
                    main_check1.isChecked = false
                    main_check2.isChecked = false
                    main_check3.isChecked = false
                    setDeviceEnable(false)
                    setMultiEnable(false)

                    EventBus.getDefault().post(RefreshMessageEvent("蓝牙断开"))
                }
            }
        }
    }

}
