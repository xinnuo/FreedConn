/**
 * created by 小卷毛, 2018/12/19
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
package com.meida.utils

import android.bluetooth.BluetoothProfile
import android.content.Context

fun Context.getProfileProxy(init: _ServiceListener.() -> Unit) {
    BluetoothHelper.getAdapter()!!
        .getProfileProxy(
            this,
            _ServiceListener().apply(init),
            BluetoothHelper.getConnectedProfile()
        )
}

class _ServiceListener : BluetoothProfile.ServiceListener {

    private var _onServiceDisconnected: ((Int) -> Unit)? = null
    private var _onServiceConnected: ((Int, BluetoothProfile) -> Unit)? = null

    fun onServiceDisconnected(listener: (Int) -> Unit) {
        _onServiceDisconnected = listener
    }

    fun onServiceConnected(listener: (Int, BluetoothProfile) -> Unit) {
        _onServiceConnected = listener
    }

    override fun onServiceDisconnected(profile: Int) {
        _onServiceDisconnected?.invoke(profile)
    }

    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
        _onServiceConnected?.invoke(profile, proxy)
    }

}
