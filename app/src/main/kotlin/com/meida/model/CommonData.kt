/**
 * created by 小卷毛, 2019/1/3
 * Copyright (c) 2019, 416143467@qq.com All Rights Reserved.
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
package com.meida.model

import java.io.Serializable

/**
 * 项目名称：FreedConn
 * 创建人：小卷毛
 * 创建时间：2019-01-03 11:43
 */
data class CommonData(
    //通讯录好友
    var userName: String = "",
    var userHead: String = "",
    var telephone: String = "",
    var accountInfoId: String = "",

    //通讯录群组
    var clusterId: String = "",
    var clusterName: String = "",
    var clusterStatus: String = "",
    var whetherCluster: String = "",
    var command: String = "",
    var commandLocal: String = "",
    var master: String = "",
    var priority: String = "",
    var imgFlag: String = "",
    var mobile: String = "",
    var clusterMembers: List<CommonData>? = ArrayList(),

    //搜索好友
    var friend: String = "",

    //验证消息数量
    var requtsetCount: String = "",

    //验证消息
    var friendRequestId: String = "",
    var friendId: String = "",
    var status: String = "",

    var isExpanded: Boolean = false,
    var isChecked: Boolean = false
) : Serializable