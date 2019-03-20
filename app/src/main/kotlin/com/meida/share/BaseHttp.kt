/**
 * created by 小卷毛, 2018/12/27
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
package com.meida.share

import com.meida.freedconn.BuildConfig

/**
 * 项目名称：FreedConn
 * 创建人：小卷毛
 * 创建时间：2018-12-27 15:22
 */
object BaseHttp {

    @Suppress("MayBeConstant")
    private val baseUrl = BuildConfig.API_HOST
    private val baseIp = "$baseUrl/api"
    val baseImg = "$baseUrl/"

    val account_reg = "$baseIp/account_reg.rm"     //注册
    val account_login = "$baseIp/account_login.rm" //登陆
    val update_pwd = "$baseIp/update_pwd.rm"       //忘记密码
    val update_pwd2 = "$baseIp/update_pwd2.rm"     //修改密码

    val cluster_list = "$baseIp/cluster_list.rm"               //搜索
    val phoneBook_list = "$baseIp/phoneBook_list.rm"           //通讯录
    val add_friend_request = "$baseIp/add_friend_request.rm"   //添加好友
    val friend_request_list = "$baseIp/friend_request_list.rm" //验证列表
    val add_friend = "$baseIp/add_friend.rm"                   //同意
    val lose_request = "$baseIp/lose_request.rm"               //忽略
    val del_friend = "$baseIp/del_friend.rm"                   //删除好友
    val friend_list = "$baseIp/friend_list1.rm"                //好友列表

    val create_cluster = "$baseIp/create_cluster.rm"                 //创建群组
    val cluster_member = "$baseIp/cluster_member.rm"                 //群组信息
    val quit_cluster = "$baseIp/quit_cluster.rm"                     //退出群组
    val out_cluster = "$baseIp/out_cluster.rm"                       //踢出群组
    val add_cluster = "$baseIp/add_cluster.rm"                       //拉入群组
    val jion_cluster = "$baseIp/jion_cluster.rm"                     //拉入群组
    val set_priority = "$baseIp/set_priority.rm"                     //设置优先权
    val update_status_cluster = "$baseIp/update_status_cluster.rm"   //群组更新
    val update_residueTime = "$baseIp/update_residueTime.rm"         //通话计时
    val update_name_cluster = "$baseIp/update_name_cluster.rm"       //修改名称
    val update_talkback_status = "$baseIp/update_talkback_status.rm" //修改对讲状态（1关闭  0开启）
    val off_talkback_status = "$baseIp/off_talkback_status.rm"       //清空对讲状态

    val userhead_edit = "$baseIp/userhead_edit.rm" //头像上传
    val pollcode_bind = "$baseIp/pollcode_bind.rm" //注册码绑定
    val system_set = "$baseIp/system_set.rm"       //设置
    val zcxy_center = "$baseIp/zcxy_center.rm"     //注册协议
}