/**
 * created by 小卷毛, 2018/12/21
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
import com.netease.nim.avchatkit.AVChatKit
import com.netease.nim.avchatkit.common.ICallUtil
import com.netease.nim.avchatkit.model.ITeamDataProvider
import com.netease.nim.avchatkit.model.IUserInfoProvider
import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.RequestCallback
import com.netease.nimlib.sdk.StatusCode
import com.netease.nimlib.sdk.auth.AuthService
import com.netease.nimlib.sdk.auth.AuthServiceObserver
import com.netease.nimlib.sdk.auth.LoginInfo
import com.netease.nimlib.sdk.auth.OnlineClient
import com.netease.nimlib.sdk.auth.constant.LoginSyncStatus
import com.netease.nimlib.sdk.avchat.AVChatCallback
import com.netease.nimlib.sdk.avchat.AVChatManager
import com.netease.nimlib.sdk.avchat.AVChatStateObserver
import com.netease.nimlib.sdk.avchat.constant.AVChatType
import com.netease.nimlib.sdk.avchat.model.*
import com.netease.nimlib.sdk.msg.MsgServiceObserve
import com.netease.nimlib.sdk.msg.model.CustomNotification
import com.netease.nimlib.sdk.uinfo.model.UserInfo

inline fun <reified T : Any> getService(): T = NIMClient.getService(T::class.java)

/**
 * 注册/注销在线状态变化观察者。<br>
 * 注册后，Observer的onEvent方法会被立即调用一次，告知观察者当前状态。
 *
 * @param register true为注册，false为注销
 * @param observer 观察者, 参数为当前状态
 */
fun AuthServiceObserver.observeOnlineStatus(
    register: Boolean = true,
    observer: (StatusCode) -> Unit
) = observeOnlineStatus(observer, register)

/**
 * 注册/注销多端登录状态观察者。当有其他端登录或者注销时，会通过此接口通知到UI。<br>
 * 登录成功后，如果有其他端登录着，也会发出通知。<br>
 *
 * @param register true为注册，false为注销
 * @param observer 观察者，参数为同时登录的其他端信息。<br>
 *                 如果有其他端注销，参数为剩余的在线端。如果没有剩余在线端了，参数为null。
 */
fun AuthServiceObserver.observeOtherClients(
    register: Boolean = true,
    observer: (List<OnlineClient>) -> Unit
) = observeOtherClients(observer, register)

/**
 * 注册/注销登录后同步数据过程通知
 *
 * @param register true为注册，false为注销
 * @param observer 观察者，参数为同步数据的过程状态（开始/结束）
 */
fun AuthServiceObserver.observeLoginSyncDataStatus(
    register: Boolean = true,
    observer: (LoginSyncStatus) -> Unit
) = observeLoginSyncDataStatus(observer, register)

/**
 * 注册/注销自定义通知接收观察者
 *
 * @param register true为注册，false为注销
 * @param observer 观察者，参数为收到的自定义通知
 */
fun MsgServiceObserve.observeCustomNotification(
    register: Boolean = true,
    observer: (CustomNotification) -> Unit
) = observeCustomNotification(observer, register)

/**
 * 设置用户相关资料提供者
 * @param init 用户相关资料提供者
 */
fun setUserInfoProvider(init: _IUserInfoProvider.() -> Unit) =
    AVChatKit.setUserInfoProvider(_IUserInfoProvider().apply(init))

/**
 * 设置群组数据提供者
 * @param init 群组数据提供者
 */
fun setTeamDataProvider(init: _ITeamDataProvider.() -> Unit) =
    AVChatKit.setTeamDataProvider(_ITeamDataProvider().apply(init))

/**
 * 音视频通话呼叫
 * @param init 音视频通话呼叫接口
 */
fun setiCallUtil(init: _ICallUtil.() -> Unit) = AVChatKit.setiCallUtil(_ICallUtil().apply(init))

/**
 * 登录接口。sdk会自动连接服务器，传递用户信息，返回登录结果。<br>
 * 该操作中途可取消。如果因为网络比较差，或其他原因导致服务器迟迟没有返回，用户也没有主动取消，
 * 在45秒后AbortableFuture的onFailed会被调用到。
 */
fun login(account: String, token: String, init: _RequestCallback.() -> Unit) =
    getService<AuthService>().login(LoginInfo(account, token)).setCallback(
        _RequestCallback().apply(
            init
        )
    )

/**
 * 创建多人通话房间
 *
 * @param roomName     房间名
 * @param extraMessage 自定义的扩展消息, 其它加入房间的用户都会收到
 * @param callback     创建房间回调接口
 */
fun createRoom(
    roomName: String,
    extraMessage: String,
    init: _AVChatCallback<AVChatChannelInfo>.() -> Unit
) = AVChatManager.getInstance().createRoom(
    roomName,
    extraMessage,
    _AVChatCallback<AVChatChannelInfo>().apply(init)
)

/**
 * 加入已经预先创建的多人会议房间
 *
 * @param roomName 房间名
 * @param callback 加入房间回调接口
 */
fun joinRoom(roomName: String, init: _AVChatCallback<AVChatData>.() -> Unit) =
    AVChatManager.getInstance().joinRoom2(
        roomName,
        AVChatType.AUDIO,
        _AVChatCallback<AVChatData>().apply(init)
    )

/**
 * 离开加入的多人会议房间
 *
 * @param roomName 房间名
 * @param callback 离开房间的回调接口
 */
fun leaveRoom(roomName: String, init: _AVChatCallback<Void>.() -> Unit) =
    AVChatManager.getInstance().leaveRoom2(roomName, _AVChatCallback<Void>().apply(init))

/**
 * 通用信令消息发送
 *
 * @param chatId         会话ID
 * @param controlCommand 控制命令值
 * @param callback       回调函数
 */
fun sendControlCommand(chatId: Long, controlCommand: Byte, init: _AVChatCallback<Void>.() -> Unit) =
    AVChatManager.getInstance().sendControlCommand(
        chatId,
        controlCommand,
        _AVChatCallback<Void>().apply(init)
    )

/**
 * 注册/注销网络通话状态通知
 * 网络通话开始后，所有的通话状态通过 {@link AVChatStateObserverLite} 进行通知。
 *
 * @param register {@code true} 注册监听，{@code false} 注销监听
 * @param observer 观察者，参数为通话状态回调
 */
fun observeAVChatState(register: Boolean = true, init: _AVChatStateObserver.() -> Unit) =
    AVChatManager.getInstance().observeAVChatState(_AVChatStateObserver().apply(init), register)


class _IUserInfoProvider : IUserInfoProvider() {

    private var _getUserInfo: ((String) -> UserInfo)? = null
    override fun getUserInfo(account: String): UserInfo? {
        return _getUserInfo?.invoke(account)
    }

    fun getUserInfo(listener: (String) -> UserInfo) {
        _getUserInfo = listener
    }

    private var _getUserDisplayName: ((String) -> String)? = null
    override fun getUserDisplayName(account: String): String? {
        return _getUserDisplayName?.invoke(account)
    }

    fun getUserDisplayName(listener: (String) -> String) {
        _getUserDisplayName = listener
    }

}

class _ITeamDataProvider : ITeamDataProvider() {

    private var _getDisplayNameWithoutMe: ((String, String) -> String)? = null
    override fun getDisplayNameWithoutMe(teamId: String, account: String): String? {
        return _getDisplayNameWithoutMe?.invoke(teamId, account)
    }

    fun getDisplayNameWithoutMe(listener: (String, String) -> String) {
        _getDisplayNameWithoutMe = listener
    }

    private var _getTeamMemberDisplayName: ((String, String) -> String)? = null
    override fun getTeamMemberDisplayName(teamId: String, account: String): String? {
        return _getTeamMemberDisplayName?.invoke(teamId, account)
    }

    fun getTeamMemberDisplayName(listener: (String, String) -> String) {
        _getTeamMemberDisplayName = listener
    }

}

class _ICallUtil : ICallUtil {

    private var _incomingCall: ((Context, AVChatData, String, Int) -> Unit)? = null
    override fun incomingCall(
        context: Context,
        config: AVChatData,
        displayName: String,
        source: Int
    ) {
        _incomingCall?.invoke(context, config, displayName, source)
    }

    fun incomingCall(listener: (Context, AVChatData, String, Int) -> Unit) {
        _incomingCall = listener
    }

    private var _outgoingCall: ((Context, String, String, Int, Int) -> Unit)? = null
    override fun outgoingCall(
        context: Context,
        account: String,
        displayName: String,
        callType: Int,
        source: Int
    ) {
        _outgoingCall?.invoke(context, account, displayName, callType, source)
    }

    fun outgoingCall(listener: (Context, String, String, Int, Int) -> Unit) {
        _outgoingCall = listener
    }

    private var _outgoingTeamCall: ((Context, String) -> Unit)? =
        null

    override fun outgoingTeamCall(context: Context, roomName: String) {
        _outgoingTeamCall?.invoke(context, roomName)
    }

    fun outgoingTeamCall(listener: (Context, String) -> Unit) {
        _outgoingTeamCall = listener
    }

    private var _startSettings: ((Context) -> Unit)? = null
    override fun startSettings(context: Context) {
        _startSettings?.invoke(context)
    }

    fun startSettings(listener: (Context) -> Unit) {
        _startSettings = listener
    }

}

open class _AVChatStateObserver : AVChatStateObserver {

    private var _onAudioMixingEvent: ((Int) -> Unit)? = null

    override fun onAudioMixingEvent(event: Int) {
        _onAudioMixingEvent?.invoke(event)
    }

    fun onAudioMixingEvent(listener: (Int) -> Unit) {
        _onAudioMixingEvent = listener
    }

    private var _onLiveEvent: ((Int) -> Unit)? = null

    override fun onLiveEvent(event: Int) {
        _onLiveEvent?.invoke(event)
    }

    fun onLiveEvent(listener: (Int) -> Unit) {
        _onLiveEvent = listener
    }

    private var _onVideoFrameResolutionChanged: ((String, Int, Int, Int) -> Unit)? = null

    override fun onVideoFrameResolutionChanged(
        account: String,
        width: Int,
        height: Int,
        rotate: Int
    ) {
        _onVideoFrameResolutionChanged?.invoke(account, width, height, rotate)
    }

    fun onVideoFrameResolutionChanged(listener: (String, Int, Int, Int) -> Unit) {
        _onVideoFrameResolutionChanged = listener
    }

    private var _onProtocolIncompatible: ((Int) -> Unit)? = null

    override fun onProtocolIncompatible(status: Int) {
        _onProtocolIncompatible?.invoke(status)
    }

    fun onProtocolIncompatible(listener: (Int) -> Unit) {
        _onProtocolIncompatible = listener
    }

    private var _onJoinedChannel: ((Int, String, String, Int) -> Unit)? = null

    override fun onJoinedChannel(code: Int, audioFile: String, videoFile: String, elapsed: Int) {
        _onJoinedChannel?.invoke(code, audioFile, videoFile, elapsed)
    }

    fun onJoinedChannel(listener: (Int, String, String, Int) -> Unit) {
        _onJoinedChannel = listener
    }

    private var _onReportSpeaker: ((MutableMap<String, Int>, Int) -> Unit)? = null

    override fun onReportSpeaker(speakers: MutableMap<String, Int>, mixedEnergy: Int) {
        _onReportSpeaker?.invoke(speakers, mixedEnergy)
    }

    fun onReportSpeaker(listener: (MutableMap<String, Int>, Int) -> Unit) {
        _onReportSpeaker = listener
    }

    private var _onAudioDeviceChanged: ((Int) -> Unit)? = null

    override fun onAudioDeviceChanged(device: Int) {
        _onAudioDeviceChanged?.invoke(device)
    }

    fun onAudioDeviceChanged(listener: (Int) -> Unit) {
        _onAudioDeviceChanged = listener
    }

    private var _onSessionStats: ((AVChatSessionStats) -> Unit)? = null

    override fun onSessionStats(sessionStats: AVChatSessionStats) {
        _onSessionStats?.invoke(sessionStats)
    }

    fun onSessionStats(listener: (AVChatSessionStats) -> Unit) {
        _onSessionStats = listener
    }

    private var _onVideoFpsReported: ((String, Int) -> Unit)? = null

    override fun onVideoFpsReported(account: String, fps: Int) {
        _onVideoFpsReported?.invoke(account, fps)
    }

    fun onVideoFpsReported(listener: (String, Int) -> Unit) {
        _onVideoFpsReported = listener
    }

    private var _onAVRecordingCompletion: ((String, String) -> Unit)? = null

    override fun onAVRecordingCompletion(account: String, filePath: String) {
        _onAVRecordingCompletion?.invoke(account, filePath)
    }

    fun onAVRecordingCompletion(listener: (String, String) -> Unit) {
        _onAVRecordingCompletion = listener
    }

    private var _onUserLeave: ((String, Int) -> Unit)? = null

    override fun onUserLeave(account: String, event: Int) {
        _onUserLeave?.invoke(account, event)
    }

    fun onUserLeave(listener: (String, Int) -> Unit) {
        _onUserLeave = listener
    }

    private var _onCallEstablished: (() -> Unit)? = null

    override fun onCallEstablished() {
        _onCallEstablished?.invoke()
    }

    fun onCallEstablished(listener: () -> Unit) {
        _onCallEstablished = listener
    }

    private var _onAudioMixingProgressUpdated: ((Long, Long) -> Unit)? = null

    override fun onAudioMixingProgressUpdated(progressMs: Long, durationMs: Long) {
        _onAudioMixingProgressUpdated?.invoke(progressMs, durationMs)
    }

    fun onAudioMixingProgressUpdated(listener: (Long, Long) -> Unit) {
        _onAudioMixingProgressUpdated = listener
    }

    private var _onUserJoined: ((String) -> Unit)? = null

    override fun onUserJoined(account: String) {
        _onUserJoined?.invoke(account)
    }

    fun onUserJoined(listener: (String) -> Unit) {
        _onUserJoined = listener
    }

    private var _onAudioFrameFilter: ((AVChatAudioFrame) -> Boolean)? = null

    override fun onAudioFrameFilter(frame: AVChatAudioFrame): Boolean {
        return _onAudioFrameFilter?.invoke(frame) ?: false
    }

    fun onAudioFrameFilter(listener: (AVChatAudioFrame) -> Boolean) {
        _onAudioFrameFilter = listener
    }

    private var _onTakeSnapshotResult: ((String, Boolean, String) -> Unit)? = null

    override fun onTakeSnapshotResult(account: String, success: Boolean, file: String) {
        _onTakeSnapshotResult?.invoke(account, success, file)
    }

    fun onTakeSnapshotResult(listener: (String, Boolean, String) -> Unit) {
        _onTakeSnapshotResult = listener
    }

    private var _onNetworkQuality: ((String, Int, AVChatNetworkStats) -> Unit)? = null

    override fun onNetworkQuality(account: String, quality: Int, stats: AVChatNetworkStats) {
        _onNetworkQuality?.invoke(account, quality, stats)
    }

    fun onNetworkQuality(listener: (String, Int, AVChatNetworkStats) -> Unit) {
        _onNetworkQuality = listener
    }

    private var _onVideoFrameFilter: ((AVChatVideoFrame, Boolean) -> Boolean)? = null

    override fun onVideoFrameFilter(frame: AVChatVideoFrame, maybeDualInput: Boolean): Boolean {
        return _onVideoFrameFilter?.invoke(frame, maybeDualInput) ?: false
    }

    fun onVideoFrameFilter(listener: (AVChatVideoFrame, Boolean) -> Boolean) {
        _onVideoFrameFilter = listener
    }

    private var _onDisconnectServer: ((Int) -> Unit)? = null

    override fun onDisconnectServer(code: Int) {
        _onDisconnectServer?.invoke(code)
    }

    fun onDisconnectServer(listener: (Int) -> Unit) {
        _onDisconnectServer = listener
    }

    private var _onAudioRecordingCompletion: ((String) -> Unit)? = null

    override fun onAudioRecordingCompletion(filePath: String) {
        _onAudioRecordingCompletion?.invoke(filePath)
    }

    fun onAudioRecordingCompletion(listener: (String) -> Unit) {
        _onAudioRecordingCompletion = listener
    }

    private var _onDeviceEvent: ((Int, String) -> Unit)? = null

    override fun onDeviceEvent(code: Int, desc: String) {
        _onDeviceEvent?.invoke(code, desc)
    }

    fun onDeviceEvent(listener: (Int, String) -> Unit) {
        _onDeviceEvent = listener
    }

    private var _onConnectionTypeChanged: ((Int) -> Unit)? = null

    override fun onConnectionTypeChanged(netType: Int) {
        _onConnectionTypeChanged?.invoke(netType)
    }

    fun onConnectionTypeChanged(listener: (Int) -> Unit) {
        _onConnectionTypeChanged = listener
    }

    private var _onLeaveChannel: (() -> Unit)? = null

    override fun onLeaveChannel() {
        _onLeaveChannel?.invoke()
    }

    fun onLeaveChannel(listener: () -> Unit) {
        _onLeaveChannel = listener
    }

    private var _onFirstVideoFrameAvailable: ((String) -> Unit)? = null

    override fun onFirstVideoFrameAvailable(account: String) {
        _onFirstVideoFrameAvailable?.invoke(account)
    }

    fun onFirstVideoFrameAvailable(listener: (String) -> Unit) {
        _onFirstVideoFrameAvailable = listener
    }

    private var _onLowStorageSpaceWarning: ((Long) -> Unit)? = null

    override fun onLowStorageSpaceWarning(availableSize: Long) {
        _onLowStorageSpaceWarning?.invoke(availableSize)
    }

    fun onLowStorageSpaceWarning(listener: (Long) -> Unit) {
        _onLowStorageSpaceWarning = listener
    }

    private var _onFirstVideoFrameRendered: ((String) -> Unit)? = null

    override fun onFirstVideoFrameRendered(account: String) {
        _onFirstVideoFrameRendered?.invoke(account)
    }

    fun onFirstVideoFrameRendered(listener: (String) -> Unit) {
        _onFirstVideoFrameRendered = listener
    }

}

class _RequestCallback : RequestCallback<LoginInfo> {

    private var _onSuccess: ((LoginInfo) -> Unit)? = null
    override fun onSuccess(param: LoginInfo) {
        _onSuccess?.invoke(param)
    }

    fun onSuccess(listener: (LoginInfo) -> Unit) {
        _onSuccess = listener
    }

    private var _onFailed: ((Int) -> Unit)? = null
    override fun onFailed(code: Int) {
        _onFailed?.invoke(code)
    }

    fun onFailed(listener: (Int) -> Unit) {
        _onFailed = listener
    }

    private var _onException: ((Throwable) -> Unit)? = null
    override fun onException(exception: Throwable) {
        _onException?.invoke(exception)
    }

    fun onException(listener: (Throwable) -> Unit) {
        _onException = listener
    }

}

class _AVChatCallback<T : Any> : AVChatCallback<T> {

    private var _onSuccess: ((T?) -> Unit)? = null
    override fun onSuccess(t: T?) {
        _onSuccess?.invoke(t)
    }

    fun onSuccess(listener: (T?) -> Unit) {
        _onSuccess = listener
    }

    private var _onFailed: ((Int) -> Unit)? = null
    override fun onFailed(code: Int) {
        _onFailed?.invoke(code)
    }

    fun onFailed(listener: (Int) -> Unit) {
        _onFailed = listener
    }

    private var _onException: ((Throwable) -> Unit)? = null
    override fun onException(exception: Throwable) {
        _onException?.invoke(exception)
    }

    fun onException(listener: (Throwable) -> Unit) {
        _onException = listener
    }

}