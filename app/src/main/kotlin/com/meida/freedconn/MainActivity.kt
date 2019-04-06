package com.meida.freedconn

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Bundle
import android.provider.Settings
import android.view.View
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
import android.os.Build
import com.lzy.okgo.utils.OkLogger
import com.meida.ble.BleConnectUtil
import com.meida.chatkit.TeamAVChatProfile
import com.meida.chatkit.TeamSoundPlayer
import com.meida.utils.ActivityStack
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.PermissionListener
import io.reactivex.disposables.CompositeDisposable


class MainActivity : BaseActivity() {

    private var mDisposable: Disposable? = null
    private val mDisposableNet by lazy { CompositeDisposable() }
    private lateinit var bleConnectUtil: BleConnectUtil
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setToolbarVisibility(false)
        init_title()

        registerReceiver()
        registerNetReceiver()
    }

    override fun onResume() {
        super.onResume()
        main_check2.isChecked = bleConnectUtil.isConnected==true
    }

    override fun init_title() {
        checkBluetoothState()
        setDeviceEnable(true)
        setMultiEnable(true)

        bleConnectUtil = BleConnectUtil.getInstance(baseContext)

        AndPermission.with(this@MainActivity)
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
                override fun onSucceed(requestCode: Int, grantPermissions: MutableList<String>) {}

                override fun onFailed(requestCode: Int, deniedPermissions: MutableList<String>) {
                    toast(getString(R.string.permission_denied))
                    onBackPressed()
                }
            }).start()

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
                        mDevices.forEach {
                            val deviceMac = it.address
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

    /** 注册蓝牙状态监听广播 **/
    private fun registerReceiver() {
        registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        registerReceiver(mReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED))
        registerReceiver(mReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
    }

    /** 注册网络状态监听广播 **/
    private fun registerNetReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?)?.registerNetworkCallback(
                NetworkRequest.Builder().build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network?) {
                        OkLogger.i("网络可用")
                        mDisposableNet.clear()
                    }

                    override fun onLost(network: Network?) {
                        OkLogger.i("网络丢失")
                        updateNetWork()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network?,
                        networkCapabilities: NetworkCapabilities?
                    ) {
                        OkLogger.i("网络功能更改")
                        mDisposableNet.clear()
                    }

                    override fun onLinkPropertiesChanged(
                        network: Network?,
                        linkProperties: LinkProperties?
                    ) {
                        OkLogger.i("网络连接属性修改")
                        mDisposableNet.clear()
                    }
                }
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(mNetReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }
    }

    private fun updateNetWork() {
        if (mDisposableNet.size() > 0) mDisposableNet.clear()

        if (TeamAVChatProfile.sharedInstance().isTeamAVEnable) {
            mDisposableNet.add(
                Observable.interval(10, 10, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        TeamSoundPlayer.instance().play(R.raw.half_second_low_tones)
                    }
            )
        }
    }

    /** 退出 **/
    private var exitTime: Long = 0

    override fun onBackPressed() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            toast(getString(R.string.quit))
            exitTime = System.currentTimeMillis()
        } else {
            ActivityStack.screenManager.popAllActivityExceptOne(MainActivity::class.java)
            super.onBackPressed()
        }
    }

    /** 注销广播 **/
    override fun finish() {
        super.finish()
        mDisposable?.dispose()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) mDisposableNet.clear()
        else unregisterReceiver(mNetReceiver)

        unregisterReceiver(mReceiver)
    }

    /** 网络广播 BroadcastReceiver **/
    @Suppress("DEPRECATION")
    private val mNetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action === ConnectivityManager.CONNECTIVITY_ACTION) {
                //获取联网状态的NetworkInfo对象
                val info =
                    intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)
                if (info.isAvailable
                    && info.state == NetworkInfo.State.CONNECTED
                ) {
                    OkLogger.i("网络连接")
                    mDisposableNet.clear()
                } else {
                    OkLogger.i("网络断开")
                    updateNetWork()
                }
            }
        }
    }

    /** 蓝牙广播 BroadcastReceiver **/
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
                        EventBus.getDefault().post(RefreshMessageEvent("遥控器连接"))
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
                        EventBus.getDefault().post(RefreshMessageEvent("遥控器断开"))
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

                            EventBus.getDefault().post(RefreshMessageEvent("遥控器断开"))
                            EventBus.getDefault().post(RefreshMessageEvent("蓝牙断开"))
                        }
                        BluetoothAdapter.STATE_ON -> { }
                    }
                }
            }
        }
    }



}
