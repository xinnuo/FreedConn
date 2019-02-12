package com.meida.freedconn

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.media.AudioManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.flyco.dialog.widget.ActionSheetDialog
import com.lzg.extend.BaseResponse
import com.lzg.extend.StringDialogCallback
import com.lzg.extend.jackson.JacksonDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.lzy.okgo.utils.OkLogger
import com.meida.base.*
import com.meida.chatkit.*
import com.meida.model.ClusterModel
import com.meida.model.CommonData
import com.meida.model.RefreshMessageEvent
import com.meida.share.BaseHttp
import com.meida.utils.BluetoothHelper
import com.meida.utils.FullyGridLayoutManager
import com.meida.utils.setAdapter
import com.netease.nimlib.sdk.Observer
import com.netease.nimlib.sdk.avchat.AVChatManager
import com.netease.nimlib.sdk.avchat.constant.AVChatOSCategory
import com.netease.nimlib.sdk.avchat.constant.AVChatUserRole
import com.netease.nimlib.sdk.avchat.model.AVChatControlEvent
import com.netease.nimlib.sdk.avchat.model.AVChatNetworkStats
import com.netease.nimlib.sdk.avchat.model.AVChatParameters
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_network_chat.*
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.sdk25.listeners.onTouch
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit

class NetworkChatActivity : BaseActivity() {

    //DATA
    private val list = ArrayList<CommonData>()       //房间全部成员
    private val listShow = ArrayList<CommonData>()   //房间展示成员
    private var accounts = ArrayList<String>()       //房间成员accid
    private var accountsOnline = ArrayList<String>() //房间在线成员
    private var chatId: Long = -1              //房间ID
    private var roomName: String = ""          //房间名称ID
    private var clusterName: String = ""       //房间名称
    private var roomMaster: String = ""        //创建者ID

    private val mCompositeDisposable by lazy { CompositeDisposable() }
    private val mPublishSubject by lazy { PublishSubject.create<Int>() }
    private lateinit var notifier: TeamAVChatNotification

    private var chatMode: String = TeamState.CHAT_NONE //群聊模式
    private var isTalkModeOn: Boolean = false          //是否对讲模式
    private var isGroupModeOn: Boolean = false         //是否群聊模式r
    private var isLocalMute: Boolean = false           //是否禁用麦克风
    private var isLocalAudioMute: Boolean = false      //是否禁用外部语音
    private var isLocalAllMute: Boolean = false        //是否禁用所用对讲

    private var isMicHolding: Boolean = false   //是否有人抢麦中
    private var holdingMaster: String = ""      //当前抢麦人
    private val mDisposables by lazy { CompositeDisposable() } //抢麦订阅池

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dismissKeyguard()
        setContentView(R.layout.activity_network_chat)
        init_title(getString(R.string.network_chat))

        EventBus.getDefault().register(this@NetworkChatActivity)

        getInfoData {
            if (!TeamAVChatProfile.sharedInstance().isTeamAVChatting) startRtc()
        }
    }

    override fun init_title() {
        super.init_title()
        chat_admin.gone()
        chat_level.gone()
        chat_user.gone()
        chat_curve.gone()
        chat_line.visible()

        roomName = intent.getStringExtra(KEY_ROOM) ?: ""
        initNotification()
        initListeners()

        chat_nine.setAdapter {
            onDisplayImage { _, imageView, url ->
                url as String
                imageView.setImageURL(url)
            }
        }

        chat_member.apply {
            layoutManager = FullyGridLayoutManager(baseContext, 6)
            mAdapter = SlimAdapter.create()
                .register<CommonData>(R.layout.item_chat_grid) { data, injector ->
                    injector.text(R.id.item_chat_name, data.userName)
                        .visibility(
                            R.id.item_chat_master,
                            if (data.master == "0") View.VISIBLE else View.INVISIBLE
                        )
                        .visibility(
                            R.id.item_chat_level,
                            if (data.priority == "0") View.VISIBLE else View.INVISIBLE
                        )
                        .with<ImageView>(R.id.item_chat_img) {
                            when (data.imgFlag) {
                                "0" -> it.setImageResource(R.mipmap.icon26)
                                "1" -> it.setImageResource(R.mipmap.icon27)
                                else -> it.setImageURL(BaseHttp.baseImg + data.userHead)
                            }
                        }
                        .clicked(R.id.item_chat) {
                            when (data.imgFlag) {
                                "0" -> startActivity<NetworkHandleActivity>(
                                    "type" to "1",
                                    "roomId" to roomName,
                                    "list" to list
                                )
                                "1" -> startActivity<NetworkHandleActivity>(
                                    "type" to "2",
                                    "roomId" to roomName,
                                    "list" to list
                                )
                            }
                        }
                }
                .attachTo(this)
            adapter = mAdapter
        }

        mCompositeDisposable.add(
            mPublishSubject.buffer(6, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    OkLogger.i("网络质量回调：${it.size}")
                    if (it.size > 2) showToast(getString(R.string.network_worse))
                }
        )
    }

    override fun onResume() {
        super.onResume()
        //取消通知栏
        activeCallingNotifier(false)
        //禁止自动锁屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        if (TeamAVChatProfile.sharedInstance().isTeamAVChatting) switchVoiceAfterPhone()
    }

    override fun onStop() {
        super.onStop()
        if (isMicHolding && holdingMaster == getString("accid")) {
            setGrabAnimation(false)
            sendCancelCommand {
                if (chatMode == TeamState.CHAT_TALK) setPttVoice(false)
                else chat_ptt.setImageResource(R.mipmap.icon35)
            }
        }

        //显示通知栏
        activeCallingNotifier(true)
    }

    //设置窗口flag，亮屏并且解锁/覆盖在锁屏界面上
    private fun dismissKeyguard() {
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    private fun initNotification() {
        notifier = TeamAVChatNotification(this)
        notifier.init(roomName)
    }

    @Suppress("DEPRECATION")
    private fun initListeners() {
        chat_ptt.onTouch { _, event ->
            if (!isLocalMute
                && chatMode != TeamState.CHAT_NONE
            ) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mDisposables.add(
                            Completable.timer(500, TimeUnit.MILLISECONDS)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    if (chatMode == TeamState.CHAT_TALK) { //对讲抢麦
                                        setGrabAnimation(true)
                                        startTalkToGrab()
                                    } else { //群聊抢麦
                                        val accidMine = getString("accid")
                                        val priorityMine =
                                            list.first { it.mobile == accidMine }.priority

                                        val authorMine = when {
                                            roomMaster == accidMine -> TeamState.MASTER
                                            priorityMine == "0" -> TeamState.PRIORITY
                                            else -> TeamState.COMMON
                                        }

                                        if (authorMine > TeamState.COMMON) {
                                            setGrabAnimation(true)
                                            startGroupToGrab()
                                        }
                                    }
                                }
                        )
                    }
                    MotionEvent.ACTION_UP -> {
                        mDisposables.clear() //取消订阅

                        Completable.timer(500, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                if (holdingMaster == getString("accid")) { //抢麦者松开
                                    setGrabAnimation(false)
                                    sendCancelCommand {
                                        if (chatMode == TeamState.CHAT_TALK) setPttVoice(false)
                                        else chat_ptt.setImageResource(R.mipmap.icon35)
                                    }
                                } else { //非抢麦者松开
                                    setGrabAnimation(false)
                                    if (chatMode == TeamState.CHAT_TALK) setPttVoice(false)
                                    else chat_ptt.setImageResource(R.mipmap.icon35)
                                }
                            }
                    }
                }
            }

            return@onTouch true
        }

        chat_single.oneClick { _ ->
            if (chat_dialog.isVisble()) return@oneClick

            if (isGroupModeOn) {
                showToast(getString(R.string.network_chat_off_group))
                return@oneClick
            }

            getStatusData(if (isTalkModeOn) 0 else 1) {

                sendControlCommand(
                    chatId,
                    if (it == 1) TeamState.NOTIFY_CUSTOM_TALK else TeamState.NOTIFY_CUSTOM_NONE
                ) {

                    onSuccess {
                        setTalkMode(!isTalkModeOn)
                        setVoiceLine(false)
                        if (isTalkModeOn) {
                            chatMode = TeamState.CHAT_TALK
                            chat_ptt.setImageResource(R.mipmap.icon35)
                            if (!isLocalMute) chat_mic.setImageResource(R.mipmap.icon30)
                            if (!isLocalAudioMute) chat_voice.setImageResource(R.mipmap.icon31)
                        } else {
                            chatMode = TeamState.CHAT_NONE
                            chat_ptt.setImageResource(R.mipmap.icon34)
                            chat_mic.setImageResource(R.mipmap.icon28)
                            chat_voice.setImageResource(R.mipmap.icon29)
                        }

                        AVChatManager.getInstance().muteLocalAudio(true)
                    }

                }
            }
        }

        chat_all.oneClick { _ ->
            if (chat_dialog.isVisble()) return@oneClick

            if (isTalkModeOn) {
                showToast(getString(R.string.network_chat_off_talk))
                return@oneClick
            }

            getStatusData(if (isGroupModeOn) 0 else 2) {

                sendControlCommand(
                    chatId,
                    if (it == 2) TeamState.NOTIFY_CUSTOM_GROUP else TeamState.NOTIFY_CUSTOM_NONE
                ) {

                    onSuccess {
                        setGroupMode(!isGroupModeOn)
                        setVoiceLine(isGroupModeOn)

                        if (isGroupModeOn) {
                            chatMode = TeamState.CHAT_GROUP
                            chat_ptt.setImageResource(R.mipmap.icon35)
                            if (!isLocalMute) chat_mic.setImageResource(R.mipmap.icon30)
                            if (!isLocalAudioMute) chat_voice.setImageResource(R.mipmap.icon31)

                            AVChatManager.getInstance().muteLocalAudio(isLocalMute)
                        } else {
                            chatMode = TeamState.CHAT_NONE
                            chat_ptt.setImageResource(R.mipmap.icon34)
                            chat_mic.setImageResource(R.mipmap.icon28)
                            chat_voice.setImageResource(R.mipmap.icon29)

                            AVChatManager.getInstance().muteLocalAudio(true)
                        }
                    }
                }
            }
        }

        chat_mic.oneClick {
            if (chat_dialog.isVisble()) return@oneClick

            if (!isLocalAllMute
                && chatMode != TeamState.CHAT_NONE
                && TeamAVChatProfile.sharedInstance().isTeamAVChatting
            ) {
                setLocalMicMute(!isLocalMute)
                if (isLocalMute) setVoiceLine(false)
                else setVoiceLine(isGroupModeOn)
            }
        }

        chat_voice.oneClick {
            if (chat_dialog.isVisble()) return@oneClick

            if (!isLocalAllMute
                && chatMode != TeamState.CHAT_NONE
                && TeamAVChatProfile.sharedInstance().isTeamAVChatting
            ) {
                setLocalAudioMute(!isLocalAudioMute)
            }
        }

        chat_talk.oneClick {
            if (chat_dialog.isVisble()) return@oneClick

            if (isLocalAllMute) {
                setMuteAll(false)
                setLocalMicMute(false)
                setLocalAudioMute(false)
                setVoiceLine(isGroupModeOn)
                if (chatMode != TeamState.CHAT_NONE) {
                    chat_ptt.setImageResource(R.mipmap.icon35)
                } else {
                    chat_mic.setImageResource(R.mipmap.icon28)
                    chat_voice.setImageResource(R.mipmap.icon29)
                    chat_ptt.setImageResource(R.mipmap.icon34)
                }
            } else {
                setMuteAll(true)
                setLocalMicMute(true)
                setLocalAudioMute(true)
                setVoiceLine(false)
                chat_ptt.setImageResource(R.mipmap.icon34)
            }
        }

        chat_level.oneClick {
            if (chat_dialog.isVisble()) return@oneClick

            startActivity<NetworkHandleActivity>(
                "type" to "3",
                "roomId" to roomName,
                "list" to list
            )
        }

        chat_share.oneClick {
            if (chat_dialog.isVisble()) return@oneClick

            ActionSheetDialog(
                baseContext,
                arrayOf(getString(R.string.network_chat_share)),
                null
            ).apply {
                isTitleShow(false)
                lvBgColor(resources.getColor(R.color.white))
                cornerRadius(15f)
                itemTextColor(resources.getColor(R.color.black))
                itemHeight(50f)
                itemTextSize(18f)
                cancelText(resources.getColor(R.color.blue_light))
                cancelTextSize(18f)
                layoutAnimation(null)
                show()

                setOnOperItemClickL { _, _, _, _ ->
                    dismiss()
                }
            }
        }

        chat_name.oneClick {
            if (chat_dialog.isVisble()
                || roomName.isEmpty()
                || roomMaster != getString("accid")
            ) return@oneClick

            startActivity<NetworkNameActivity>(
                "clusterId" to roomName,
                "name" to clusterName
            )
        }
    }

    private fun getInfoData(event: (() -> Unit)? = null) {
        OkGo.post<BaseResponse<ClusterModel>>(BaseHttp.cluster_member)
            .tag(this@NetworkChatActivity)
            .params("clusterId", roomName)
            .execute(object : JacksonDialogCallback<BaseResponse<ClusterModel>>(baseContext, true) {

                @SuppressLint("SetTextI18n")
                override fun onSuccess(response: Response<BaseResponse<ClusterModel>>) {

                    list.apply {
                        clear()
                        addItems(response.body().`object`.members)
                    }
                    roomMaster = list.firstOrNull { it.master == "0" }?.mobile ?: ""
                    accounts.clear()
                    list.mapTo(accounts) { it.mobile }

                    val data = response.body().`object`.clusters
                    clusterName = data.clusterName
                    chat_name.text = "$clusterName(${list.size}人)"
                    chat_code.text = getString(R.string.network_chat_code) + data.command
                    when (data.clusterStatus) {
                        "0" -> chatMode = TeamState.CHAT_NONE
                        "1" -> chatMode = TeamState.CHAT_TALK
                        "2" -> chatMode = TeamState.CHAT_GROUP
                    }
                    chat_level.visibility =
                        if (getString("accid") == roomMaster) View.VISIBLE else View.GONE

                    val imgs = list.map { BaseHttp.baseImg + it.userHead }
                    chat_nine.setImagesData(imgs)

                    listShow.clear()
                    listShow.addAll(list.filter { it.master == "0" })
                    listShow.addAll(list.filter { it.priority == "0" })

                    val accidMine = getString("accid")
                    val priorityMine = list.first { it.mobile == accidMine }.priority
                    val listNoun = ArrayList<CommonData>()
                    val itemCount =
                        if (roomMaster == accidMine || priorityMine == "0") (10 - listShow.size) else (11 - listShow.size)

                    list.filter { it.master != "0" && it.priority != "0" }
                        .forEachWithIndex { index, item ->
                            if (index < itemCount) listNoun.add(item)
                        }
                    listShow.addAll(listNoun)

                    if (roomMaster == accidMine || priorityMine == "0")
                        listShow.add(CommonData().apply { imgFlag = "0" })
                    listShow.add(CommonData().apply { imgFlag = "1" })

                    mAdapter.updateData(listShow)

                    if (event != null) event()
                }

            })
    }

    private fun getStatusData(status: Int, event: (Int) -> Unit) {
        OkGo.post<String>(BaseHttp.update_status_cluster)
            .tag(this@NetworkChatActivity)
            .headers("token", getString("token"))
            .params("clusterId", roomName)
            .params("clusterStatus", status)
            .execute(object : StringDialogCallback(baseContext) {

                override fun onSuccessResponse(
                    response: Response<String>,
                    msg: String,
                    msgCode: String
                ) {
                    event(status)
                }

            })
    }

    @SuppressLint("CheckResult")
    private fun updateTiming() {
        mCompositeDisposable.add(
            Observable.interval(60, 60, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    OkGo.post<String>(BaseHttp.update_residueTime)
                        .tag(this@NetworkChatActivity)
                        .headers("token", getString("token"))
                        .execute(object : StringDialogCallback(baseContext, false) {

                            override fun onSuccessResponse(
                                response: Response<String>,
                                msg: String,
                                msgCode: String
                            ) {
                                OkLogger.i(msg)
                            }

                        })
                }
        )
    }

    /* 声音强制切换到耳机或外放 */
    private fun forceVoiceToBluetooth() {
        mCompositeDisposable.add(
            Observable.interval(2, 2, TimeUnit.SECONDS)
                .map { return@map BluetoothHelper.isBluetoothConnected() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager

                    if (it) {
                        am.isBluetoothScoOn = true
                        am.isSpeakerphoneOn = false
                        am.startBluetoothSco()
                    } else {
                        am.isBluetoothScoOn = false
                        am.isSpeakerphoneOn = true
                        am.stopBluetoothSco()
                    }
                }
        )
    }

    /* 声音切换到耳机或外放 */
    @SuppressLint("CheckResult")
    private fun switchVoiceAfterPhone() {
        Observable.timer(1000, TimeUnit.MILLISECONDS)
            .map { return@map BluetoothHelper.isBluetoothConnected() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager

                if (it) {
                    am.isBluetoothScoOn = true
                    am.isSpeakerphoneOn = false
                    am.startBluetoothSco()
                } else {
                    am.isBluetoothScoOn = false
                    am.isSpeakerphoneOn = true
                    am.stopBluetoothSco()
                }
            }
    }

    /* 开始对讲抢麦 */
    private fun startTalkToGrab() {
        val accid = getString("accid")
        val priority = list.first { it.mobile == accid }.priority
        when {
            accid == roomMaster -> sendSuccessCommand()
            priority == "0" -> {
                mDisposables.add(
                    Completable.timer(500, TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            if (!isMicHolding) sendSuccessCommand()
                            else checkMineAuthor()
                        }
                )
            }
            else -> {
                mDisposables.add(
                    Completable.timer(1000, TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            if (!isMicHolding) sendSuccessCommand()
                            else checkMineAuthor()
                        }
                )
            }
        }
    }

    /* 开始群聊抢麦 */
    @SuppressLint("CheckResult")
    private fun startGroupToGrab() {
        if (roomMaster == getString("accid")) sendSuccessCommand()
        else {
            mDisposables.add(
                Completable.timer(500, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (!isMicHolding) sendSuccessCommand()
                        else checkMineAuthor()
                    }
            )
        }
    }

    /* 抢麦失败 */
    private fun failedToGrab() {
        setPttVoice(false)
        setGrabAnimation(false)
        TeamSoundPlayer.instance().play()
    }

    /* 判断我的抢麦权限 */
    private fun checkMineAuthor() {
        //我的accid和priority
        val accidMine = getString("accid")
        val priorityMine = list.first { it.mobile == accidMine }.priority

        //抢麦者的accid和priority
        val accidNow = holdingMaster
        val priorityNow = list.first { it.mobile == accidNow }.priority

        //我的权限
        val authorMine = when {
            roomMaster == accidMine -> TeamState.MASTER
            priorityMine == "0" -> TeamState.PRIORITY
            else -> TeamState.COMMON
        }

        //抢麦者的权限
        val authorNow = when {
            roomMaster == accidNow -> TeamState.MASTER
            priorityNow == "0" -> TeamState.PRIORITY
            else -> TeamState.COMMON
        }

        if (authorNow < authorMine) sendSuccessCommand()
        else failedToGrab()
    }

    /* 发送对讲抢麦成功控制指令 */
    private fun sendSuccessCommand() {
        OkLogger.i("成功")
        sendControlCommand(chatId, TeamState.NOTIFY_GRAB_SUCCESSS) {
            onSuccess {
                isMicHolding = true
                holdingMaster = getString("accid")
                chat_dialog_hint.text = getString(R.string.network_chat_mic_done)
                setPttVoice(true)
            }
            onFailed { failedToGrab() }
            onException { failedToGrab() }
        }
    }

    /* 发送取消抢麦控制指令 */
    private fun sendCancelCommand(event: (() -> Unit)) {
        OkLogger.i("取消")
        sendControlCommand(chatId, TeamState.NOTIFY_GRAB_CANCEL) {
            onSuccess {
                isMicHolding = false
                holdingMaster = ""
                event()
            }
        }
    }

    /* 设置抢麦动画 */
    private fun setGrabAnimation(isBegan: Boolean) {
        chat_dialog.visibility = if (isBegan) View.VISIBLE else View.GONE
        chat_dialog_hint.text = getString(R.string.network_chat_mic_holding)
        val animationDrawable = chat_dialog_img.drawable as AnimationDrawable
        if (isBegan) animationDrawable.start()
        else {
            if (animationDrawable.isRunning)
                animationDrawable.stop()
        }
    }

    /* 设置是否禁用对讲功能 */
    private fun setMuteAll(isEnable: Boolean) {
        isLocalAllMute = isEnable
        chat_talk.setBackgroundResource(if (isEnable) R.mipmap.btn07 else R.mipmap.btn08)
        @Suppress("DEPRECATION")
        chat_talk.setTextColor(resources.getColor(if (isEnable) R.color.light else R.color.blue_light))
        chat_talk.text =
            getString(if (isEnable) R.string.network_chat_on else R.string.network_chat_off)
    }

    /* 设置是否开启对讲模式 */
    private fun setTalkMode(isEnable: Boolean) {
        isTalkModeOn = isEnable
        chat_single.setBackgroundResource(if (isEnable) R.mipmap.btn08 else R.mipmap.btn07)
        @Suppress("DEPRECATION")
        chat_single.setTextColor(resources.getColor(if (isEnable) R.color.blue_light else R.color.light))
    }

    /* 设置是否开启群聊模式 */
    private fun setGroupMode(isEnable: Boolean) {
        isGroupModeOn = isEnable
        chat_all.setBackgroundResource(if (isEnable) R.mipmap.btn08 else R.mipmap.btn07)
        @Suppress("DEPRECATION")
        chat_all.setTextColor(resources.getColor(if (isEnable) R.color.blue_light else R.color.light))
    }

    /* 设置是否开启PTT语音 */
    private fun setPttVoice(isDown: Boolean) {
        chat_ptt.setImageResource(if (isDown) R.mipmap.icon34 else R.mipmap.icon35)
        setVoiceLine(isDown)

        AVChatManager.getInstance().muteLocalAudio(!isDown)
    }

    /* 设置是否开启波浪线 */
    private fun setVoiceLine(isShown: Boolean) {
        chat_curve.visibility = if (isShown) View.VISIBLE else View.GONE
        chat_line.visibility = if (isShown) View.GONE else View.VISIBLE
    }

    /* 设置是否禁用本地麦克风 */
    private fun setLocalMicMute(isEnable: Boolean) {
        chat_mic.setImageResource(if (isEnable) R.mipmap.icon28 else R.mipmap.icon30)

        isLocalMute = isEnable
        if (chatMode == TeamState.CHAT_GROUP)
            AVChatManager.getInstance().muteLocalAudio(isLocalMute)
    }

    /* 设置是否禁用本地语音播报 */
    private fun setLocalAudioMute(isEnable: Boolean) {
        chat_voice.setImageResource(if (isEnable) R.mipmap.icon29 else R.mipmap.icon31)

        isLocalAudioMute = isEnable
        AVChatManager.getInstance().muteAllRemoteAudio(isEnable)
    }

    /* 设置通话状态 */
    private fun setChatting(isChatting: Boolean) {
        TeamAVChatProfile.sharedInstance().isTeamAVChatting = isChatting
    }

    /* 通知栏显示状态 */
    private fun activeCallingNotifier(active: Boolean) {
        notifier.activeCallingNotification(if (TeamAVChatProfile.sharedInstance().isTeamAVChatting) active else false)
    }

    /* 音视频配置 */
    @SuppressLint("SetTextI18n")
    private fun startRtc() {
        AVChatManager.getInstance().enableRtc()      //激活音视频通话底层引擎
        AVChatManager.getInstance().disableVideo()   //关闭视频模块

        val isConnected = BluetoothHelper.isBluetoothConnected()
        AVChatManager.getInstance().setSpeaker(!isConnected) //是否打开扬声器

        AVChatManager.getInstance().observeAVChatState(mStateObserver, false) //注销网络通话状态
        AVChatManager.getInstance().observeAVChatState(mStateObserver, true)  //注册网络通话状态

        AVChatManager.getInstance().observeControlNotification(mControlEventObserver, false) //注销网络通话控制消息
        AVChatManager.getInstance().observeControlNotification(mControlEventObserver, true)  //注册网络通话控制消息

        AVChatManager.getInstance().setParameter(AVChatParameters.KEY_SESSION_MULTI_MODE_USER_ROLE, AVChatUserRole.NORMAL) //角色模式
        AVChatManager.getInstance().setParameter(AVChatParameters.KEY_AUDIO_REPORT_SPEAKER, true)                          //声音强度汇报
        AVChatManager.getInstance().setParameter(AVChatParameters.KEY_OS_CATEGORY, AVChatOSCategory.DEFAULT)               //设备类型
        AVChatManager.getInstance().setParameter(AVChatParameters.KEY_AUDIO_DTX_ENABLE, true)                              //语音DTX
        joinRoom(roomName) {
            //加入房间
            onSuccess { data ->
                chatId = data!!.chatId
                setChatting(true)
                AVChatManager.getInstance().muteAllRemoteAudio(false) //是否允许播放远端用户语音

                val accid = getString("accid")
                val priority = list.first { it.mobile == accid }.priority
                val isFirst = accid == roomMaster || priority == "0"
                chat_admin.visibility = if (isFirst) View.VISIBLE else View.GONE
                chat_user.visibility = if (isFirst) View.GONE else View.VISIBLE
                chat_level.visibility = if (accid == roomMaster) View.VISIBLE else View.GONE

                accountsOnline.add(getString("accid"))
                chat_number.text = getString(R.string.network_chat_num) + "${accountsOnline.size}人"

                when (chatMode) {
                    TeamState.CHAT_NONE -> {
                        setTalkMode(false)  //关闭对讲模式
                        setGroupMode(false) //关闭群聊模式
                        setVoiceLine(false) //关闭波浪线
                        chat_hint.text = " "
                        chat_ptt.setImageResource(R.mipmap.icon34)
                        chat_mic.setImageResource(R.mipmap.icon28)
                        chat_voice.setImageResource(R.mipmap.icon29)

                        AVChatManager.getInstance().muteLocalAudio(true)
                    }
                    TeamState.CHAT_TALK -> {
                        setTalkMode(true)   //开启对讲模式
                        setGroupMode(false) //关闭群聊模式
                        setVoiceLine(false) //关闭波浪线
                        chat_hint.text = getString(R.string.network_chat_hint1)
                        chat_ptt.setImageResource(R.mipmap.icon35)
                        chat_mic.setImageResource(R.mipmap.icon30)
                        chat_voice.setImageResource(R.mipmap.icon31)

                        AVChatManager.getInstance().muteLocalAudio(true)
                    }
                    TeamState.CHAT_GROUP -> {
                        setTalkMode(false) //关闭对讲模式
                        setGroupMode(true) //开启群聊模式
                        setVoiceLine(true) //开启波浪线
                        chat_hint.text = getString(R.string.network_chat_hint2)
                        chat_ptt.setImageResource(R.mipmap.icon35)
                        chat_mic.setImageResource(R.mipmap.icon30)
                        chat_voice.setImageResource(R.mipmap.icon31)

                        AVChatManager.getInstance().muteLocalAudio(false)
                    }
                }

                updateTiming()
                forceVoiceToBluetooth()
            }
            onFailed {
                showToast(getString(R.string.network_chat_error_join))
                onBackPressed()
            }
        }
    }

    /* 通话控制指令观察者 */
    private val mControlEventObserver = Observer<AVChatControlEvent> { event ->
        when (event.controlCommand) {
            TeamState.NOTIFY_CUSTOM_NONE -> {
                //关闭抢麦
                isMicHolding = false
                holdingMaster = ""
                if (chat_dialog.isVisble()) {
                    setGrabAnimation(false)
                    TeamSoundPlayer.instance().play()
                }

                setTalkMode(false)  //关闭对讲模式
                setGroupMode(false) //关闭群聊模式
                setVoiceLine(false) //关闭波浪线
                chatMode = TeamState.CHAT_NONE
                chat_hint.text = " "
                chat_ptt.setImageResource(R.mipmap.icon34)
                chat_mic.setImageResource(R.mipmap.icon28)
                chat_voice.setImageResource(R.mipmap.icon29)

                AVChatManager.getInstance().muteLocalAudio(true)
            }
            TeamState.NOTIFY_CUSTOM_TALK -> {
                setTalkMode(true)   //开启对讲模式
                setGroupMode(false) //关闭群聊模式
                setVoiceLine(false) //关闭波浪线
                chatMode = TeamState.CHAT_TALK
                chat_hint.text = getString(R.string.network_chat_hint1)
                if (!isLocalAllMute) chat_ptt.setImageResource(R.mipmap.icon35)
                if (!isLocalMute) chat_mic.setImageResource(R.mipmap.icon30)
                if (!isLocalAudioMute) chat_voice.setImageResource(R.mipmap.icon31)

                AVChatManager.getInstance().muteLocalAudio(true)
            }
            TeamState.NOTIFY_CUSTOM_GROUP -> {
                setTalkMode(false)         //关闭对讲模式
                setGroupMode(true)         //开启群聊模式
                setVoiceLine(!isLocalMute) //开启波浪线
                chatMode = TeamState.CHAT_GROUP
                chat_hint.text = getString(R.string.network_chat_hint2)
                if (!isLocalAllMute) chat_ptt.setImageResource(R.mipmap.icon35)
                if (!isLocalMute) chat_mic.setImageResource(R.mipmap.icon30)
                if (!isLocalAudioMute) chat_voice.setImageResource(R.mipmap.icon31)

                AVChatManager.getInstance().muteLocalAudio(isLocalMute)
            }
            TeamState.NOTIFY_GRAB_SUCCESSS -> {
                if (!isMicHolding) {
                    //没有抢麦者
                    isMicHolding = true
                    holdingMaster = event.account

                    if (!isLocalMute && chatMode == TeamState.CHAT_GROUP) {
                        setVoiceLine(false)
                        AVChatManager.getInstance().muteLocalAudio(true)
                    }
                } else {
                    //指令者的accid和priority
                    val accidRevevie = event.account
                    val priorityReceive = list.first { it.mobile == accidRevevie }.priority

                    //我的accid和priority
                    val accidMine = getString("accid")
                    val priorityMine = list.first { it.mobile == accidMine }.priority

                    //抢麦者的accid和priority
                    val accidNow = holdingMaster
                    val priorityNow = list.first { it.mobile == accidNow }.priority

                    //我的权限
                    val authorMine = when {
                        roomMaster == accidMine -> TeamState.MASTER
                        priorityMine == "0" -> TeamState.PRIORITY
                        else -> TeamState.COMMON
                    }

                    //指令者的权限
                    val authorReceive = when {
                        roomMaster == accidRevevie -> TeamState.MASTER
                        priorityReceive == "0" -> TeamState.PRIORITY
                        else -> TeamState.COMMON
                    }

                    //抢麦者的权限
                    val authorNow = when {
                        roomMaster == accidNow -> TeamState.MASTER
                        priorityNow == "0" -> TeamState.PRIORITY
                        else -> TeamState.COMMON
                    }

                    if (accidNow == accidMine) {
                        if (authorMine < authorReceive) {
                            holdingMaster = accidRevevie
                            failedToGrab()
                        }
                    } else {
                        if (authorNow < authorReceive)
                            holdingMaster = accidRevevie
                    }
                }

            }
            TeamState.NOTIFY_GRAB_CANCEL -> {
                if (event.account == holdingMaster) {
                    isMicHolding = false
                    holdingMaster = ""

                    if (!isLocalMute && chatMode == TeamState.CHAT_GROUP) {
                        setVoiceLine(true)
                        AVChatManager.getInstance().muteLocalAudio(false)
                    }
                }
            }
            TeamState.NOTIFY_GRAB_HOLDING -> {
                isMicHolding = true
                holdingMaster = event.account

                if (chatMode == TeamState.CHAT_GROUP) {
                    AVChatManager.getInstance().muteLocalAudio(true)
                }
            }
        }
    }

    /* 音视频状态观察者 */
    private val mStateObserver = object : _AVChatStateObserver() {

        @SuppressLint("SetTextI18n")
        override fun onUserJoined(account: String) {
            OkLogger.i("用户：${account}加入房间")
            if (accountsOnline.none { it == account }) accountsOnline.add(account)
            chat_number.text = getString(R.string.network_chat_num) + "${accountsOnline.size}人"

            if (isMicHolding && holdingMaster == getString("accid")) {
                AVChatManager.getInstance().sendControlCommand(
                    chatId,
                    TeamState.NOTIFY_GRAB_HOLDING,
                    null
                )
            }

            AVChatManager.getInstance().muteAllRemoteAudio(isLocalAudioMute) //是否允许播放远端用户语音
        }

        @SuppressLint("SetTextI18n")
        override fun onUserLeave(account: String, event: Int) {
            OkLogger.i("用户：${account}离开房间")
            if (accountsOnline.any { it == account }) accountsOnline.remove(account)
            chat_number.text = getString(R.string.network_chat_num) + "${accountsOnline.size}人"
        }

        @SuppressLint("CheckResult")
        override fun onNetworkQuality(account: String, quality: Int, stats: AVChatNetworkStats) {
            if (account == getString("accid") && quality > 2) {
                OkLogger.i("用户：$account， 网络质量：$quality")
                mPublishSubject.onNext(quality)
            }
        }

        override fun onConnectionTypeChanged(netType: Int) {
            when (netType) {
                20 -> {
                    showToast(getString(R.string.network_wifi))
                    cancelLoadingDialog()
                }
                30, 40, 50 -> {
                    showToast(getString(R.string.network_wap))
                    cancelLoadingDialog()
                }
                70 -> {
                    showToast(getString(R.string.network_none))
                    showLoadingDialog(getString(R.string.connecting))
                }
                90 -> {
                    showToast(getString(R.string.network_vpn))
                    cancelLoadingDialog()
                }
            }
        }

        override fun onReportSpeaker(speakers: MutableMap<String, Int>, mixedEnergy: Int) {
            // speakers.forEach { OkLogger.i("onReportSpeaker:${it.key}, ${it.value}") }
        }
    }

    /* 结束通话 */
    private fun hangUp() {
        AVChatManager.getInstance().leaveRoom2(roomName, null) //离开多人房间
        AVChatManager.getInstance().disableRtc()                        //关闭音视频通话底层引擎

        AVChatManager.getInstance().muteAllRemoteAudio(false)
        AVChatManager.getInstance().observeAVChatState(mStateObserver, false)
        AVChatManager.getInstance().observeControlNotification(mControlEventObserver, false)
    }

    /* 退出 */
    override fun finish() {
        super.finish()
        hangUp()
        setChatting(false)
        activeCallingNotifier(false)

        mCompositeDisposable.clear()
        TeamSoundPlayer.instance().stop()
        EventBus.getDefault().unregister(this@NetworkChatActivity)
    }

    @SuppressLint("SetTextI18n")
    @Subscribe
    fun onMessageEvent(event: RefreshMessageEvent) {
        when (event.type) {
            "修改群名" -> {
                if (event.id == roomName) {
                    clusterName = event.name
                    chat_name.text = "$clusterName(${list.size}人)"
                }

                TeamAVChatEx.onModifyRoomSuccess(
                    baseContext,
                    roomName,
                    clusterName,
                    accountsOnline.filter { it != getString("accid") })
            }
            "优先权" -> {
                TeamAVChatEx.onSetLevelSuccess(
                    baseContext,
                    roomName,
                    accounts.filter { it != getString("accid") })
                getInfoData()
            }
            "拉入群组" -> { //先更新后通知
                getInfoData {
                    TeamAVChatEx.onPullRoomSuccess(
                        baseContext,
                        roomName,
                        accounts.filter { it != getString("accid") })
                }
            }
            "踢出群组" -> { //先通知后更新
                TeamAVChatEx.onKickRoomSuccess(
                    baseContext,
                    roomName,
                    accounts.filter { it != getString("accid") })
                getInfoData()
            }
            "拉入群组通知", "踢出群组通知" -> if (event.id == roomName) {
                getInfoData {
                    if (accounts.none { it == getString("accid") })
                        onBackPressed()
                }
            }
            "加入群组通知", "退出群组通知" -> if (event.id == roomName) {
                getInfoData()
            }
            "修改群名通知" -> if (event.id == roomName) {
                clusterName = event.name
                chat_name.text = "$clusterName(${list.size}人)"
            }
            "优先权通知" -> if (event.id == roomName) {
                getInfoData {
                    val accid = getString("accid")
                    val priority = list.first { it.mobile == accid }.priority
                    if (priority == "0") {
                        if (!chat_admin.isVisble()) {
                            chat_admin.visible()
                            chat_user.gone()

                            setMuteAll(false)
                            setLocalMicMute(isLocalMute)
                            setLocalAudioMute(isLocalAudioMute)
                            if (chatMode != TeamState.CHAT_NONE)
                                chat_ptt.setImageResource(R.mipmap.icon35)
                            else chat_ptt.setImageResource(R.mipmap.icon34)
                        }
                    } else {
                        if (chat_admin.isVisble()) {
                            chat_admin.gone()
                            chat_user.visible()
                        }
                    }
                }
            }
            "蓝牙连接" -> AVChatManager.getInstance().setSpeaker(false)
            "蓝牙断开" -> AVChatManager.getInstance().setSpeaker(true)
            "电话接听" -> {
                AVChatManager.getInstance().muteAllRemoteAudio(true)
                AVChatManager.getInstance().muteLocalAudio(true)
                if (AVChatManager.getInstance().speakerEnabled())
                    AVChatManager.getInstance().setSpeaker(false)
            }
            "电话挂断" -> {
                if (!isLocalAudioMute) AVChatManager.getInstance().muteAllRemoteAudio(false)
                if (!isLocalMute) AVChatManager.getInstance().muteLocalAudio(false)
                if (!BluetoothHelper.isBluetoothConnected())
                    AVChatManager.getInstance().setSpeaker(true)

                switchVoiceAfterPhone()
            }
        }
    }

    companion object {
        private const val KEY_ROOM = "roomName"

        fun startActivity(context: Context, roomName: String) {
            context.startActivity(Intent().apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                setClass(context, NetworkChatActivity::class.java)
                putExtra(KEY_ROOM, roomName)
            })
        }
    }
}
