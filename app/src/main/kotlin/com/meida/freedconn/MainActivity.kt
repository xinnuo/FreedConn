package com.meida.freedconn

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.view.View
import com.meida.base.BaseActivity
import com.meida.base.getString
import com.meida.chatkit.TeamAVChatProfile
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
import com.netease.nim.avchatkit.AVChatKit
import com.netease.nimlib.sdk.auth.AuthServiceObserver
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

        registerReceiver()
    }

    override fun onResume() {
        super.onResume()

        TeamAVChatProfile.sharedInstance().apply {
            isTeamAVChatting = false
            teamAVChatId = ""
            teamAVChatName = ""
            isTeamAVEnable = false
        }
    }

    override fun init_title() {
        checkBluetoothState()
        setDeviceEnable(true)
        setMultiEnable(true)

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
                            main_check3.isChecked = true
                            setDeviceEnable(true)
                            setMultiEnable(true)
                        } else {
                            main_check1.isChecked = false
                            main_check2.isChecked = false
                            main_check3.isChecked = false
                            setDeviceEnable(true)
                            setMultiEnable(true)
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
            R.id.main_bluetooth -> toast("暂不对外开放")
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

    /** 注册广播 **/
    private fun registerReceiver() {
        registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        registerReceiver(mReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED))
        registerReceiver(mReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
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
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val deviceMac = device.address
                    if (deviceMac.startsWith(Const.MAC_HEADER_1)
                        || deviceMac.startsWith(Const.MAC_HEADER_2)
                        || deviceMac.startsWith(Const.MAC_HEADER_3)
                    ) {
                        main_check1.isChecked = false
                        main_check2.isChecked = false
                        main_check3.isChecked = true
                        setDeviceEnable(true)
                        setMultiEnable(true)
                    } else if (deviceMac.startsWith(Const.MACBLE_HEADER_1)) {
                        main_check2.isChecked = true
                    } else {
                        main_check1.isChecked = false
                        main_check2.isChecked = false
                        main_check3.isChecked = false
                        setDeviceEnable(true)
                        setMultiEnable(true)
                    }

                    EventBus.getDefault().post(RefreshMessageEvent("蓝牙连接"))
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val deviceMac = device.address
                    if (deviceMac.startsWith(Const.MAC_HEADER_1)
                        || deviceMac.startsWith(Const.MAC_HEADER_2)
                        || deviceMac.startsWith(Const.MAC_HEADER_3)
                    ) {
                        main_check3.isChecked = false
                    } else if (deviceMac.startsWith(Const.MACBLE_HEADER_1)) {
                        main_check2.isChecked = false
                    }
                    setDeviceEnable(true)
                    setMultiEnable(true)

                    EventBus.getDefault().post(RefreshMessageEvent("蓝牙断开"))
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                    when (blueState) {
                        BluetoothAdapter.STATE_OFF -> {
                            main_check1.isChecked = false
                            main_check2.isChecked = false
                            main_check3.isChecked = false
                            setDeviceEnable(true)
                            setMultiEnable(true)

                            EventBus.getDefault().post(RefreshMessageEvent("蓝牙断开"))
                        }
                        BluetoothAdapter.STATE_ON -> {
                        }
                    }
                }
            }
        }
    }

}
