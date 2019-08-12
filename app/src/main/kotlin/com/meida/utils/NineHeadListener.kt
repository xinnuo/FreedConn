/**
 * created by 小卷毛, 2018/12/24
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

import android.content.Context
import android.widget.ImageView
import com.lqr.ninegridimageview.LQRNineGridImageView
import com.lqr.ninegridimageview.LQRNineGridImageViewAdapter

fun <T : Any> LQRNineGridImageView<T>.setAdapter(init: _LQRNineGridImageViewAdapter<T>.() -> Unit) {
    val adapter = _LQRNineGridImageViewAdapter<T>()
    adapter.init()
    setAdapter(adapter)
}

class _LQRNineGridImageViewAdapter<T : Any> : LQRNineGridImageViewAdapter<T>() {

    private var _onDisplayImage: ((Context, ImageView, T) -> Unit)? = null
    private var _generateImageView: ((ImageView) -> Unit)? = null

    fun onDisplayImage(listener: (Context, ImageView, T) -> Unit) {
        _onDisplayImage = listener
    }

    fun generateImageView(listener: (ImageView) -> Unit) {
        _generateImageView = listener
    }

    override fun onDisplayImage(context: Context, imageView: ImageView, t: T) {
        _onDisplayImage?.invoke(context, imageView, t)
    }

    /**
     * 重写该方法自定义生成ImageView方式，用于九宫格头像中的一个个图片控件，可以设置ScaleType等属性
     */
    override fun generateImageView(context: Context): ImageView {
        return super.generateImageView(context).apply { _generateImageView?.invoke(this) }
    }

}
