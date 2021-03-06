package com.meida.freedconn

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.meida.base.*
import com.meida.ble.BleConnectUtil
import com.meida.ble.BleConnectUtil.getInstance
import com.meida.ble.BleConnectUtil.mBluetoothGattCharacteristic
import com.meida.ble.CheckUtils
import com.meida.ble.Constants
import com.meida.ble.EventMsg
import com.meida.share.Const
import com.meida.utils.DialogHelper
import com.meida.utils.toNotDouble
import kotlinx.android.synthetic.main.activity_device_remote.*
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.sdk25.listeners.onClick
import org.jetbrains.anko.toast
import kotlin.math.roundToInt

class DeviceRemoteActivity : BaseActivity() {

    private lateinit var bleConnectUtil: BleConnectUtil
    private val listDevice = ArrayList<BluetoothDevice>()
    private val listSS = ArrayList<Int>()

    /**
     * 跟ble通信的标志位,检测数据是否在指定时间内返回
     */
    private var bleFlag = false
    private var regainBleDataCount = 0
    private var currentSendOrder = ""
    private var sData: ByteArray? = null

    @SuppressLint("HandlerLeak")
    private val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                10 -> {
                    cancelLoadingDialog()
                    remote_progress.setProgress((msg.obj).toString().toNotDouble(), 100.0)
                }
                1000 -> {
                    regainBleDataCount = 0
                    bleFlag = false
                    this.removeCallbacks(checkConnetRunnable)
                    toast("请求数据超时")
                }
                1111 -> bleConnectUtil.disConnect()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_remote)
        init_title("遥控器")

        EventBus.getDefault().register(this@DeviceRemoteActivity)

        if (bleConnectUtil.isConnected) {
            remote_result.visible()
            remote_power.visible()
            remote_list.gone()
            remote_search.gone()
            remote_load.gone()
            showLoadingDialog("获取数据中...")
            remote_name.text = Const.BleName
            sendDataByBle("FF01050700")
        } else {
            remote_search.text = "点击搜索"
            remote_search.onClick {
                remote_search.text = "搜索中..."
                remote_load.visible()

                listDevice.clear()
                listSS.clear()
                mAdapter.updateData(listDevice)
                searchBleDevice()

                handler.postDelayed({
                    bleConnectUtil.stopScan()
                    remote_search.text = "点击搜索"
                    remote_load.gone()
                }, 10 * 1000)
            }
        }

        remote_name.onClick {
            DialogHelper.showHintDialog(
                this,
                "提示",
                "是否断开链接当前BLE设备",
                "取消",
                "确定",
                false
            ) { hint ->
                if (hint == "yes") {
                    remote_search.text="点击搜索"
                    remote_load.visible()

                    remote_search.visible()
                    remote_result.gone()
                    remote_power.gone()
                    remote_list.visible()

                    bleConnectUtil.disConnect()


                    remote_search.onClick {
                        remote_search.text = "搜索中..."
                        remote_load.visible()

                        listDevice.clear()
                        listSS.clear()
                        mAdapter.updateData(listDevice)
                        searchBleDevice()

                        handler.postDelayed({
                            bleConnectUtil.stopScan()
                            remote_search.text = "点击搜索"
                            remote_load.gone()
                        }, 10 * 1000)
                    }


                }
            }
        }
    }

    override fun init_title() {
        super.init_title()
        remote_search.visible()
        remote_result.gone()
        remote_power.gone()
        remote_list.visible()
        bleConnectUtil = getInstance(baseContext)
        bleConnectUtil.setCallback(this)

        remote_progress.setProgressTextAdapter {
            return@setProgressTextAdapter "${it.roundToInt()}%"
        }
        remote_progress.setProgress(0.0, 100.0)

        remote_list.load_Linear(baseContext)
        mAdapter = SlimAdapter.create()
            .register<BluetoothDevice>(R.layout.item_device_list) { data, injector ->

                injector.text(R.id.item_device_name, data.name)
                    .text(R.id.item_device_power, listSS[listDevice.indexOf(data)].toString())
                    .gone(R.id.item_device_check)
                    .clicked(R.id.item_device) {
                        bleConnectUtil.stopScan()
                        mPosition = listDevice.indexOf(data)
                        showLoadingDialog("正在连接...")
                        bleConnectUtil.connectBle(data)
                    }
            }
            .attachTo(remote_list)
    }

    private fun searchBleDevice() {
        bleConnectUtil.bluetoothIsAble { device, ss ->
            if (listDevice.none { it.address == device.address }) {
                if (device.name == "Freedconn") {
                    listDevice.add(device)
                    listSS.add(ss)
                    mAdapter.updateData(listDevice)
                }
            }
        }
    }

    /**
     * 蓝牙连接检测线程
     */
    private val checkConnetRunnable: Runnable = object : Runnable {
        override fun run() {


            if (!bleFlag) {
                //没有在指定时间收到回复
                if (regainBleDataCount > 2) {
                    handler.sendEmptyMessage(1000)
                } else {
                    regainBleDataCount++

                    sendDataByBle(currentSendOrder)
                    handler.postDelayed(this, 3000)
                }
            }
        }
    }

    /**
     * android ble 发送
     * 每条数据长度应保证在20个字节以内
     * 2条数据至少要空15ms
     */
    private fun sendDataByBle(currentSendAllOrder: String) {
        if (currentSendAllOrder.isNotEmpty()) {
            currentSendOrder = currentSendAllOrder
            val isSuccess = BooleanArray(1)

            if (currentSendAllOrder.length <= 40) {
                sData = CheckUtils.hex2byte(currentSendOrder)
                mBluetoothGattCharacteristic.value = sData
                isSuccess[0] = bleConnectUtil.sendData(mBluetoothGattCharacteristic)
            } else {
                var i = 0
                while (i < currentSendAllOrder.length) {
                    val shortOrder = arrayOf("")
                    val finalI = i

                    if (currentSendAllOrder.length - i >= 40) {
                        shortOrder[0] = currentSendAllOrder.substring(finalI, finalI + 40)
                    } else {
                        shortOrder[0] =
                            currentSendAllOrder.substring(finalI, currentSendAllOrder.length)
                    }

                    sData = CheckUtils.hex2byte(shortOrder[0])
                    mBluetoothGattCharacteristic.value = sData
                    isSuccess[0] = bleConnectUtil.sendData(mBluetoothGattCharacteristic)
                    i += 40
                }
            }
            handler.postDelayed({
                if (!isSuccess[0]) handler.sendEmptyMessage(1111)
            }, ((currentSendAllOrder.length / 40 + 1) * 15).toLong())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this@DeviceRemoteActivity)
    }

    @SuppressLint("SetTextI18n")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventMsg) {
        when (event.msg) {
            Constants.BLE_CONNECTION_FINISH_MSG -> {
                cancelLoadingDialog()

                if (bleConnectUtil.isConnected) {
                    remote_search.gone()
                    remote_load.gone()

                    remote_result.visible()
                    remote_power.visible()
                    remote_list.gone()
                    remote_search.text = "点击搜索"
                    remote_load.gone()

                    bleConnectUtil.stopScan()

                    try {
                        remote_name.text = listDevice[mPosition].name
                        Const.BleAddress = listDevice[mPosition].address
                        Const.BleName = listDevice[mPosition].name
                        listDevice.clear()
                        listSS.clear()

                        mAdapter.updateData(listDevice)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    handler.postDelayed({ sendDataByBle("FF01050700") }, 200)
                } else toast("遥控设备连接失败")
            }
        }
    }

    override fun onRecive(data_char: BluetoothGattCharacteristic) {
        bleFlag = true

        //收到的数据
        val receiverData = CheckUtils.byte2hex(data_char.value).toString()
        if ("07" in receiverData) {
            val data = (receiverData.substring(8, 10).toInt(16)).toString()

            handler.sendMessage(Message().apply {
                obj = data
                what = 10
            })
        }
    }

    override fun onDisconnect() {
        //设备断开连接
        handler.sendMessage(Message().apply { what = 1111 })
    }

}
