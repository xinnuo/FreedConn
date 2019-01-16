/**
 * created by 小卷毛, 2018/12/29
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
package com.meida.chatkit

import android.content.Context
import com.netease.nimlib.sdk.msg.MsgService
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum
import com.netease.nimlib.sdk.msg.model.CustomNotification
import com.netease.nimlib.sdk.msg.model.CustomNotificationConfig
import com.meida.base.getString
import com.meida.freedconn.R

object TeamAVChatEx {

    /**
     * 发送点对点创建房间成功的自定义通知
     */
    fun onCreateRoomSuccess(context: Context, roomName: String, accounts: List<String>) {
        val mContent = com.meida.chatkit.TeamAVChatProfile.sharedInstance()
            .buildContent(roomName, TeamState.CHAT_CREATE)
        val mConfig = CustomNotificationConfig().apply {
            enablePush = true
            enablePushNick = false
            enableUnreadCount = true
        }

        accounts.forEach {
            getService<MsgService>().sendCustomNotification(CustomNotification().apply {
                sessionId = it
                sessionType = SessionTypeEnum.P2P
                config = mConfig
                content = mContent
                apnsText = context.getString("userName") +
                        context.getString(R.string.network_chat_push_join)
                isSendToOnlineUserOnly = false
            })
        }
    }

    /**
     * 发送点对点加入房间成功的自定义通知
     */
    fun onJoinRoomSuccess(context: Context, roomName: String, accounts: List<String>) {
        val mContent = com.meida.chatkit.TeamAVChatProfile.sharedInstance()
            .buildContent(roomName, TeamState.CHAT_JOIN)
        val mConfig = CustomNotificationConfig().apply {
            enablePush = true
            enablePushNick = false
            enableUnreadCount = true
        }

        accounts.forEach {
            getService<MsgService>().sendCustomNotification(CustomNotification().apply {
                sessionId = it
                sessionType = SessionTypeEnum.P2P
                config = mConfig
                content = mContent
                apnsText = context.getString("userName") +
                        context.getString(R.string.network_chat_push_jionin)
                isSendToOnlineUserOnly = false
            })
        }
    }

    /**
     * 发送点对点退出房间成功的自定义通知
     */
    fun onQuitRoomSuccess(context: Context, roomName: String, accounts: List<String>) {
        val mContent = com.meida.chatkit.TeamAVChatProfile.sharedInstance()
            .buildContent(roomName, TeamState.CHAT_QUIT)
        val mConfig = CustomNotificationConfig().apply {
            enablePush = true
            enablePushNick = false
            enableUnreadCount = true
        }

        accounts.forEach {
            getService<MsgService>().sendCustomNotification(CustomNotification().apply {
                sessionId = it
                sessionType = SessionTypeEnum.P2P
                config = mConfig
                content = mContent
                apnsText = context.getString("userName") +
                        context.getString(R.string.network_chat_push_quit)
                isSendToOnlineUserOnly = false
            })
        }
    }

    /**
     * 发送点对点拉人入房间的自定义通知
     */
    fun onPullRoomSuccess(context: Context, roomName: String, accounts: List<String>) {
        val mContent = com.meida.chatkit.TeamAVChatProfile.sharedInstance()
            .buildContent(roomName, TeamState.CHAT_PULL)
        val mConfig = CustomNotificationConfig().apply {
            enablePush = true
            enablePushNick = false
            enableUnreadCount = true
        }

        accounts.forEach {
            getService<MsgService>().sendCustomNotification(CustomNotification().apply {
                sessionId = it
                sessionType = SessionTypeEnum.P2P
                config = mConfig
                content = mContent
                apnsText = context.getString("userName") +
                        context.getString(R.string.network_chat_push_pull)
                isSendToOnlineUserOnly = false
            })
        }
    }

    /**
     * 发送点对点设置房间优先权的自定义通知
     */
    fun onSetLevelSuccess(context: Context, roomName: String, accounts: List<String>) {
        val mContent = com.meida.chatkit.TeamAVChatProfile.sharedInstance()
            .buildContent(roomName, TeamState.CHAT_LEVEL)
        val mConfig = CustomNotificationConfig().apply {
            enablePush = true
            enablePushNick = false
            enableUnreadCount = true
        }

        accounts.forEach {
            getService<MsgService>().sendCustomNotification(CustomNotification().apply {
                sessionId = it
                sessionType = SessionTypeEnum.P2P
                config = mConfig
                content = mContent
                apnsText = context.getString("userName") +
                        context.getString(R.string.network_chat_push_level)
                isSendToOnlineUserOnly = false
            })
        }
    }

    /**
     * 发送点对点踢出房间的自定义通知
     */
    fun onKickRoomSuccess(context: Context, roomName: String, accounts: List<String>) {
        val mContent = com.meida.chatkit.TeamAVChatProfile.sharedInstance()
            .buildContent(roomName, TeamState.CHAT_KICK)
        val mConfig = CustomNotificationConfig().apply {
            enablePush = true
            enablePushNick = false
            enableUnreadCount = true
        }

        accounts.forEach {
            getService<MsgService>().sendCustomNotification(CustomNotification().apply {
                sessionId = it
                sessionType = SessionTypeEnum.P2P
                config = mConfig
                content = mContent
                apnsText = context.getString("userName") +
                        context.getString(R.string.network_chat_push_kick)
                isSendToOnlineUserOnly = false
            })
        }
    }

    /**
     * 发送点对点加好友的自定义通知
     */
    fun onAddFriendSuccess(context: Context, account: String) {
        val mContent = com.meida.chatkit.TeamAVChatProfile.sharedInstance()
            .buildContent(TeamState.CHAT_FRIEND_ADD)
        val mConfig = CustomNotificationConfig().apply {
            enablePush = true
            enablePushNick = false
            enableUnreadCount = true
        }

        getService<MsgService>().sendCustomNotification(CustomNotification().apply {
            sessionId = account
            sessionType = SessionTypeEnum.P2P
            config = mConfig
            content = mContent
            apnsText = context.getString("userName") +
                    context.getString(R.string.network_chat_push_friend_add)
            isSendToOnlineUserOnly = false
        })
    }

    /**
     * 发送点对点删好友的自定义通知
     */
    fun onDelFriendSuccess(context: Context, account: String) {
        val mContent = com.meida.chatkit.TeamAVChatProfile.sharedInstance()
            .buildContent(TeamState.CHAT_FRIEND_DEL)
        val mConfig = CustomNotificationConfig().apply {
            enablePush = true
            enablePushNick = false
            enableUnreadCount = true
        }

        getService<MsgService>().sendCustomNotification(CustomNotification().apply {
            sessionId = account
            sessionType = SessionTypeEnum.P2P
            config = mConfig
            content = mContent
            apnsText = context.getString("userName") +
                    context.getString(R.string.network_chat_push_friend_del)
            isSendToOnlineUserOnly = false
        })
    }

    /**
     * 发送点对点同意加好友的自定义通知
     */
    fun onAgreeFriendSuccess(context: Context, account: String) {
        val mContent = com.meida.chatkit.TeamAVChatProfile.sharedInstance()
            .buildContent(TeamState.CHAT_FRIEND_AGREE)
        val mConfig = CustomNotificationConfig().apply {
            enablePush = true
            enablePushNick = false
            enableUnreadCount = true
        }

        getService<MsgService>().sendCustomNotification(CustomNotification().apply {
            sessionId = account
            sessionType = SessionTypeEnum.P2P
            config = mConfig
            content = mContent
            apnsText = context.getString("userName") +
                    context.getString(R.string.network_chat_push_friend_agree)
            isSendToOnlineUserOnly = false
        })
    }

}
