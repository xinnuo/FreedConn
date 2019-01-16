package com.meida.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.support.annotation.RequiresPermission
import android.bluetooth.BluetoothProfile

/**
 * <pre>
 * 蓝牙相关工具类
 * </pre>
 */
object BluetoothHelper {

    /**
     * 获取默认蓝牙对象
     */
    fun getAdapter() = BluetoothAdapter.getDefaultAdapter() ?: null

    /**
     * 设备是否支持蓝牙
     */
    fun isSupportBluetooth() = getAdapter() != null

    /**
     * 蓝牙是否已经启动
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    fun isBluetoothEnable() = getAdapter()?.isEnabled ?: false

    /**
     * 蓝牙是否已连接
     */
    fun isBluetoothConnected() = getConnectedProfile() != -1

    /**
     * 获取已连接的配置类型
     */
    fun getConnectedProfile(): Int {
        return if (isBluetoothEnable()) {
            val a2dp = getAdapter()!!.getProfileConnectionState(BluetoothProfile.A2DP)
            val headset = getAdapter()!!.getProfileConnectionState(BluetoothProfile.HEADSET)
            val health = getAdapter()!!.getProfileConnectionState(BluetoothProfile.HEALTH)

            when {
                a2dp == BluetoothHeadset.STATE_CONNECTED -> BluetoothProfile.A2DP
                headset == BluetoothHeadset.STATE_CONNECTED -> BluetoothProfile.HEADSET
                health == BluetoothHeadset.STATE_CONNECTED -> BluetoothProfile.HEALTH
                else -> -1
            }
        } else -1
    }

}
