/**
 * created by 小卷毛, 2018/03/05
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

@file:Suppress("NOTHING_TO_INLINE")

package com.meida.base

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.IdRes
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.meida.freedconn.R
import com.meida.utils.DialogHelper
import com.meida.utils.PreferencesUtils
import java.io.Serializable

/**
 * Activity 扩展类
 */
inline fun <reified T : View> Activity.find(@IdRes id: Int): T = findViewById(id)

inline fun <reified T : Activity> Context.startActivityEx(vararg params: Pair<String, Any?>) =
    startActivity(Intent(this, T::class.java).apply {
        if (params.isNotEmpty()) {
            params.forEach {
                val value = it.second
                when (value) {
                    null -> putExtra(it.first, null as Serializable?)
                    is Int -> putExtra(it.first, value)
                    is Long -> putExtra(it.first, value)
                    is CharSequence -> putExtra(it.first, value)
                    is String -> putExtra(it.first, value)
                    is Float -> putExtra(it.first, value)
                    is Double -> putExtra(it.first, value)
                    is Char -> putExtra(it.first, value)
                    is Short -> putExtra(it.first, value)
                    is Boolean -> putExtra(it.first, value)
                    is Serializable -> putExtra(it.first, value)
                    is Bundle -> putExtra(it.first, value)
                    is Parcelable -> putExtra(it.first, value)
                    is Array<*> -> when {
                        value.isArrayOf<CharSequence>() -> putExtra(it.first, value)
                        value.isArrayOf<String>() -> putExtra(it.first, value)
                        value.isArrayOf<Parcelable>() -> putExtra(it.first, value)
                        else -> throw Exception("Intent extra ${it.first} has wrong type ${value.javaClass.name}")
                    }
                    is IntArray -> putExtra(it.first, value)
                    is LongArray -> putExtra(it.first, value)
                    is FloatArray -> putExtra(it.first, value)
                    is DoubleArray -> putExtra(it.first, value)
                    is CharArray -> putExtra(it.first, value)
                    is ShortArray -> putExtra(it.first, value)
                    is BooleanArray -> putExtra(it.first, value)
                    else -> throw Exception("Intent extra ${it.first} has wrong type ${value.javaClass.name}")
                }
                return@forEach
            }
        }
    })

inline fun Context.getString(key: String, defaultValue: String = ""): String =
    PreferencesUtils.getString(this, key, defaultValue)

inline fun Context.putString(key: String, vaule: String) =
    PreferencesUtils.putString(this, key, vaule)

inline fun Context.getBoolean(key: String, defaultValue: Boolean = false): Boolean =
    PreferencesUtils.getBoolean(this, key, defaultValue)

inline fun Context.putBoolean(key: String, vaule: Boolean) =
    PreferencesUtils.putBoolean(this, key, vaule)

inline fun Context.showToast(message: CharSequence): Toast = Toast
    .makeText(this, message, Toast.LENGTH_SHORT)
    .apply {
        setGravity(Gravity.CENTER, 0, 0)
        show()
    }

inline fun Activity.showLoadingDialog(hint: String = getString(R.string.loading)) =
    DialogHelper.showDialog(this, hint)

inline fun Activity.cancelLoadingDialog() = DialogHelper.dismissDialog()