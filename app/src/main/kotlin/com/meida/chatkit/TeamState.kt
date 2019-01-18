package com.meida.chatkit

import com.netease.nimlib.sdk.avchat.constant.AVChatControlCommand.NOTIFY_CUSTOM_BASE

object TeamState {

    const val CHAT_CREATE = "create" //创建房间
    const val CHAT_JOIN = "jion"     //加入房间
    const val CHAT_QUIT = "quit"     //退出房间
    const val CHAT_PULL = "pull"     //拉入房间
    const val CHAT_KICK = "kick"     //踢出房间
    const val CHAT_LEVEL = "level"   //房间权限
    const val CHAT_TALK = "Intercom" //对讲模式
    const val CHAT_GROUP = "group"   //群聊模式
    const val CHAT_NONE = "none"     //静音模式
    const val CHAT_FRIEND_ADD = "friend_add"     //加好友
    const val CHAT_FRIEND_DEL = "friend_del"     //删好友
    const val CHAT_FRIEND_AGREE = "friend_agree" //同意好友

    const val MASTER = 102   //群主
    const val PRIORITY = 101 //优先者
    const val COMMON = 100   //普通成员

    //控制指令
    const val NOTIFY_CUSTOM_NONE = (NOTIFY_CUSTOM_BASE + 1).toByte()   //静音
    const val NOTIFY_CUSTOM_TALK = (NOTIFY_CUSTOM_BASE + 2).toByte()   //对讲
    const val NOTIFY_CUSTOM_GROUP = (NOTIFY_CUSTOM_BASE + 3).toByte()  //群聊
    const val NOTIFY_GRAB_SUCCESSS = (NOTIFY_CUSTOM_BASE + 4).toByte() //抢麦成功
    const val NOTIFY_GRAB_CANCEL = (NOTIFY_CUSTOM_BASE + 5).toByte()   //抢麦取消
    const val NOTIFY_GRAB_HOLDING = (NOTIFY_CUSTOM_BASE + 6).toByte()  //抢麦中

}
