package com.meida.freedconn

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
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
import com.meida.ble.BleConnectUtil
import com.meida.chatkit.*
import com.meida.model.ClusterModel
import com.meida.model.CommonData
import com.meida.model.RefreshMessageEvent
import com.meida.share.BaseHttp
import com.meida.share.Const
import com.meida.utils.*
import com.meida.utils.BluetoothHelper.getAdapter
import com.meida.utils.BluetoothHelper.getConnectedProfile
import com.netease.nimlib.sdk.Observer
import com.netease.nimlib.sdk.avchat.AVChatManager
import com.netease.nimlib.sdk.avchat.constant.AVChatUserRole
import com.netease.nimlib.sdk.avchat.model.AVChatControlEvent
import com.netease.nimlib.sdk.avchat.model.AVChatNetworkStats
import com.netease.nimlib.sdk.avchat.model.AVChatParameters
import com.umeng.socialize.ShareAction
import com.umeng.socialize.UMShareAPI
import com.umeng.socialize.bean.SHARE_MEDIA
import com.umeng.socialize.media.UMImage
import com.umeng.socialize.media.UMWeb
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
import org.jetbrains.anko.longToast
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
    private var modeMaster: String = ""        //管理员ID

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

        ActivityStack.screenManager.popActivities(NetworkActivity::class.java)
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
        TeamAVChatProfile.sharedInstance().isTeamAVEnable = false

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
                                else -> {
                                    it.setImageURL(BaseHttp.baseImg + data.userHead)

                                    it.colorFilter = ColorMatrixColorFilter(
                                        ColorMatrix().apply {
                                            setSaturation(if (data.isOnline) 1f else 0f)
                                        }
                                    )
                                }
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

        if (BleConnectUtil.getInstance(baseContext).isConnected) {
            BleConnectUtil.getInstance(baseContext).setCallback(this)
        }
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
    }

    override fun onStop() {
        super.onStop()
        if (isMicHolding && holdingMaster == getString("accid")) {
            setGrabAnimation(false)
            sendCancelCommand {
                if (chatMode == TeamState.CHAT_TALK) setPttVoice(false)
                else {
                    chat_ptt.setImageResource(R.mipmap.icon35)
                    setVoiceLine(true)
                }
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
        /* 抢麦 */
        chat_ptt.onTouch { _, event ->
            if (!isLocalMute
                && chatMode != TeamState.CHAT_NONE
            ) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> onPTTDown()
                    MotionEvent.ACTION_UP -> onPTTUp()
                }
            }

            return@onTouch true
        }

        /* 对讲模式切换 */
        chat_single.oneClick { _ ->
            if (chat_dialog.isVisble()) return@oneClick

            if (isGroupModeOn) {
                showToast(getString(R.string.network_chat_off_group))
                return@oneClick
            }

            Completable.timer(
                RandomLength.createRandomNumber(100, 3000).toLong(),
                TimeUnit.MILLISECONDS
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { showLoadingDialog() }
                .doFinally { cancelLoadingDialog() }
                .subscribe {
                    if (modeMaster.isNotEmpty()
                        && modeMaster != getString("accid")) return@subscribe

                    getStatusData(if (isTalkModeOn) 0 else 1) {

                        sendControlCommand(
                            chatId,
                            if (it == 1) TeamState.NOTIFY_CUSTOM_TALK else TeamState.NOTIFY_CUSTOM_NONE
                        ) {

                            onSuccess {
                                setTalkMode(!isTalkModeOn)
                                setVoiceLine(false)
                                modeMaster = if (isTalkModeOn) getString("accid") else ""

                                if (isTalkModeOn) {
                                    chatMode = TeamState.CHAT_TALK
                                    chat_ptt.setImageResource(R.mipmap.icon35)
                                    if (accountsOnline.size > 1) chat_ptt.visible()
                                    if (!isLocalMute) chat_mic.setImageResource(R.mipmap.icon30)
                                    if (!isLocalAudioMute) chat_voice.setImageResource(R.mipmap.icon31)
                                    checkFreedconn()

                                    TeamAVChatProfile.sharedInstance().isTeamAVEnable = true
                                } else {
                                    chatMode = TeamState.CHAT_NONE
                                    chat_ptt.setImageResource(R.mipmap.icon34)
                                    chat_mic.setImageResource(R.mipmap.icon28)
                                    chat_voice.setImageResource(R.mipmap.icon29)
                                    chat_ptt.invisible()

                                    TeamAVChatProfile.sharedInstance().isTeamAVEnable = false
                                }

                                AVChatManager.getInstance().muteLocalAudio(true)

                                getAllStatusData(if (isTalkModeOn) "0" else "1") //对讲模式状态
                            }
                        }
                    }
                }
        }

        /* 群聊模式切换 */
        chat_all.oneClick { _ ->
            if (chat_dialog.isVisble()) return@oneClick

            if (isTalkModeOn) {
                showToast(getString(R.string.network_chat_off_talk))
                return@oneClick
            }

            Completable.timer(
                RandomLength.createRandomNumber(100, 3000).toLong(),
                TimeUnit.MILLISECONDS
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { showLoadingDialog() }
                .doFinally { cancelLoadingDialog() }
                .subscribe {
                    if (modeMaster.isNotEmpty()
                        && modeMaster != getString("accid")) return@subscribe

                    getStatusData(if (isGroupModeOn) 0 else 2) {

                        sendControlCommand(
                            chatId,
                            if (it == 2) TeamState.NOTIFY_CUSTOM_GROUP else TeamState.NOTIFY_CUSTOM_NONE
                        ) {

                            onSuccess {
                                setGroupMode(!isGroupModeOn)
                                setVoiceLine(isGroupModeOn)
                                modeMaster = if (isGroupModeOn) getString("accid") else ""

                                if (isGroupModeOn) {
                                    chatMode = TeamState.CHAT_GROUP
                                    chat_ptt.setImageResource(R.mipmap.icon35)
                                    if (accountsOnline.size > 1) chat_ptt.visible()
                                    if (!isLocalMute) chat_mic.setImageResource(R.mipmap.icon30)
                                    if (!isLocalAudioMute) chat_voice.setImageResource(R.mipmap.icon31)
                                    checkFreedconn()

                                    TeamAVChatProfile.sharedInstance().isTeamAVEnable = true
                                    AVChatManager.getInstance().muteLocalAudio(isLocalMute)
                                } else {
                                    chatMode = TeamState.CHAT_NONE
                                    chat_ptt.setImageResource(R.mipmap.icon34)
                                    chat_mic.setImageResource(R.mipmap.icon28)
                                    chat_voice.setImageResource(R.mipmap.icon29)
                                    chat_ptt.invisible()

                                    TeamAVChatProfile.sharedInstance().isTeamAVEnable = false
                                    AVChatManager.getInstance().muteLocalAudio(true)
                                }

                                getAllStatusData(if (isGroupModeOn) "0" else "1") //对讲模式状态
                            }
                        }
                    }
                }
        }

        /* 麦克风切换 */
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

        /* 声音切换 */
        chat_voice.oneClick {
            if (chat_dialog.isVisble()) return@oneClick

            if (!isLocalAllMute
                && chatMode != TeamState.CHAT_NONE
                && TeamAVChatProfile.sharedInstance().isTeamAVChatting
            ) {
                setLocalAudioMute(!isLocalAudioMute)
            }
        }

        /* 对讲切换 */
        chat_talk.oneClick {
            if (chat_dialog.isVisble()
                || chatMode == TeamState.CHAT_NONE
            ) return@oneClick

            getAllStatusData(if (isLocalAllMute) "0" else "1") {
                if (isLocalAllMute) {
                    setMuteAll(false)
                    setLocalMicMute(false)
                    setLocalAudioMute(false)
                    setVoiceLine(isGroupModeOn)
                    TeamAVChatProfile.sharedInstance().isTeamAVEnable = true
                    checkFreedconn()
                    if (chatMode != TeamState.CHAT_NONE) {
                        chat_ptt.setImageResource(R.mipmap.icon35)

                        if (chatMode == TeamState.CHAT_GROUP) {
                            val accid = getString("accid")
                            val priority = list.firstOrNull { it.mobile == accid }?.priority ?: ""
                            val isFirst = accid == roomMaster || priority == "0"

                            chat_ptt.visibility =
                                if (isFirst && accountsOnline.size > 1) View.VISIBLE
                                else View.INVISIBLE
                        } else {
                            if (accountsOnline.size > 1) chat_ptt.visible()
                        }
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
                    chat_ptt.invisible()
                    TeamAVChatProfile.sharedInstance().isTeamAVEnable = false
                }
            }

        }

        /* 优先权 */
        chat_level.oneClick {
            if (chat_dialog.isVisble()) return@oneClick

            startActivity<NetworkHandleActivity>(
                "type" to "3",
                "roomId" to roomName,
                "list" to list
            )
        }

        /* 分享 */
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
                    ShareAction(baseContext)
                        .setPlatform(SHARE_MEDIA.WEIXIN)
                        .withText(getString(R.string.app_name))
                        .withMedia(UMWeb("https://www.pgyer.com/D11B").apply {
                            title = getString(R.string.app_name)
                            description = "为你分享群聊 $clusterName"
                            setThumb(UMImage(baseContext, R.mipmap.ic_logo))
                        })
                        .share()
                }
            }
        }

        /* 修改名称 */
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

    /* PTT按下 */
    private fun onPTTDown() {
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

    /* PTT松开 */
    @SuppressLint("CheckResult")
    private fun onPTTUp() {
        mDisposables.clear() //取消订阅

        Completable.timer(500, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (holdingMaster == getString("accid")) { //抢麦者松开
                    setGrabAnimation(false)
                    sendCancelCommand {
                        if (chatMode == TeamState.CHAT_TALK) setPttVoice(false)
                        else {
                            chat_ptt.setImageResource(R.mipmap.icon35)
                            setVoiceLine(true)
                        }
                    }
                } else { //非抢麦者松开
                    setGrabAnimation(false)
                    if (chatMode == TeamState.CHAT_TALK) setPttVoice(false)
                    else {
                        chat_ptt.setImageResource(R.mipmap.icon35)
                        setVoiceLine(true)
                    }
                }
            }
    }

    /* 群聊信息 */
    private fun getInfoData(isLoading: Boolean = true, event: (() -> Unit)? = null) {
        OkGo.post<BaseResponse<ClusterModel>>(BaseHttp.cluster_member)
            .tag(this@NetworkChatActivity)
            .headers("token", getString("token"))
            .params("clusterId", roomName)
            .execute(object :
                JacksonDialogCallback<BaseResponse<ClusterModel>>(baseContext, isLoading) {

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
                    val priorityMine = list.firstOrNull { it.mobile == accidMine }?.priority ?: ""
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

                    listShow.forEach { inner ->
                        inner.isOnline =
                            accountsOnline.any { it == inner.mobile } && inner.talkbackStatus == "0"
                    }

                    chat_number.text =
                        getString(R.string.network_chat_num) + "${listShow.filter { it.isOnline }.size}人"
                    mAdapter.updateData(listShow)

                    if (event != null) event()
                }

            })
    }

    /* 更新模式 */
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

    /* 在线状态使能 */
    private fun getAllStatusData(status: String, event: ((String) -> Unit)? = null) {
        OkGo.post<String>(BaseHttp.update_talkback_status)
            .tag(this@NetworkChatActivity)
            .headers("token", getString("token"))
            .params("clusterId", roomName)
            .params("talkbackStatus", status)
            .execute(object : StringDialogCallback(baseContext) {

                override fun onSuccessResponse(
                    response: Response<String>,
                    msg: String,
                    msgCode: String
                ) {
                    if (event != null) event(status)
                }

            })
    }

    /* 管理员在线指令 */
    private fun sendAdminCommand() {
        mCompositeDisposable.add(
            Observable.interval(10, 10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (modeMaster == getString("accid"))
                        sendControlCommand(chatId, TeamState.NOTIFY_GRAB_ADMIN) { }
                }
        )
    }

    private var mAdminCount = 0L
    private var mMemberCount = 0L

    /* 成员在线指令判断 */
    private fun checkAdminCommand() {
        mCompositeDisposable.add(
            Observable.interval(10, 10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if ((modeMaster.isNotEmpty() && modeMaster != getString("accid"))
                        || (modeMaster.isEmpty() && chatMode != TeamState.CHAT_NONE)
                    ) {
                        mMemberCount++

                        OkLogger.e("memeber$mMemberCount")

                        if (mMemberCount > mAdminCount + 2) {
                            mAdminCount = 0L
                            mMemberCount = 0L

                            //关闭抢麦
                            isMicHolding = false
                            holdingMaster = ""
                            if (chat_dialog.isVisble()) {
                                setGrabAnimation(false)
                                TeamSoundPlayer.instance().play()
                            }

                            modeMaster = ""
                            chatMode = TeamState.CHAT_NONE
                            setTalkMode(false)  //关闭对讲模式
                            setGroupMode(false) //关闭群聊模式
                            setVoiceLine(false) //关闭波浪线

                            chat_ptt.setImageResource(R.mipmap.icon34)
                            chat_mic.setImageResource(R.mipmap.icon28)
                            chat_voice.setImageResource(R.mipmap.icon29)
                            chat_ptt.invisible()

                            chat_hint.text = " "
                            chat_talk.setBackgroundResource(R.mipmap.btn07)
                            @Suppress("DEPRECATION")
                            chat_talk.setTextColor(resources.getColor(R.color.light))

                            AVChatManager.getInstance().muteLocalAudio(true)

                            /* 普通用户关闭对讲 */
                            setMuteAll(true)
                            setLocalMicMute(true)
                            setLocalAudioMute(true)
                            setVoiceLine(false)
                            getAllStatusData("1")

                            /* 群主、优先者开启管理员模式 */
                            val accid = getString("accid")
                            val priority = list.firstOrNull { it.mobile == accid }?.priority ?: ""
                            val isFirst = accid == roomMaster || priority == "0"
                            if (isFirst) {
                                setAdminEnable(true)
                                setMuteAll(false)
                                setLocalMicMute(false)
                                setLocalAudioMute(false)
                                setVoiceLine(false)
                            }

                            if (accountsOnline.filterNot { it == modeMaster }.isNotEmpty()) {
                                val inner = accountsOnline.filterNot { it == modeMaster }.first()
                                if (getString("accid") == inner) {
                                    OkGo.post<String>(BaseHttp.off_talkback_status)
                                        .tag(this@NetworkChatActivity)
                                        .params("clusterId", roomName)
                                        .execute(object : StringDialogCallback(baseContext) {

                                            override fun onSuccessResponse(
                                                response: Response<String>,
                                                msg: String,
                                                msgCode: String
                                            ) {
                                            }

                                        })

                                    getStatusData(0) {
                                        sendControlCommand(chatId, TeamState.NOTIFY_CUSTOM_NONE) { }
                                    }
                                }
                            }
                        }
                    }
                }
        )
    }

    /* 更新时长 */
    @SuppressLint("CheckResult")
    private fun updateTiming() {
        mCompositeDisposable.add(
            Observable.interval(60, 60, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (accountsOnline.size > 1) {
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
                }
        )
    }

    /* 更新用户在线状态 */
    @SuppressLint("CheckResult", "SetTextI18n")
    private fun updateOnlineData() {
        mCompositeDisposable.add(
            Observable.interval(5, 5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe { getInfoData(false) }
        )
    }

    /* 声音强制切换到耳机或外放 */
    private fun forceVoiceToBluetooth() {
        mCompositeDisposable.add(
            Observable.interval(2, 2, TimeUnit.SECONDS)
                .map { return@map BluetoothHelper.isBluetoothConnected() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { isBlue ->
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

                        when {
                            devices.any {
                                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                                        || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                            } -> {
                                getAdapter()!!.getProfileProxy(baseContext, getConnectedProfile()) {
                                    onServiceConnected { profile, proxy ->
                                        val mDevices = proxy.connectedDevices
                                        if (!mDevices.isNullOrEmpty()) {
                                            mDevices.forEach {
                                                val deviceMac = it.address
                                                if (deviceMac.startsWith(Const.MAC_HEADER_1)
                                                    || deviceMac.startsWith(Const.MAC_HEADER_2)
                                                    || deviceMac.startsWith(Const.MAC_HEADER_3)
                                                ) {
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

                                        getAdapter()!!.closeProfileProxy(profile, proxy)
                                    }
                                }
                            }
                            devices.any {
                                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                                        || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                            } -> {
                                am.isBluetoothScoOn = false
                                am.isSpeakerphoneOn = false
                                am.stopBluetoothSco()
                            }
                            else -> {
                                am.isBluetoothScoOn = false
                                am.isSpeakerphoneOn = true
                                am.stopBluetoothSco()
                            }
                        }
                    } else {
                        when {
                            isBlue -> {
                                getAdapter()!!.getProfileProxy(baseContext, getConnectedProfile()) {
                                    onServiceConnected { profile, proxy ->
                                        val mDevices = proxy.connectedDevices
                                        if (!mDevices.isNullOrEmpty()) {
                                            mDevices.forEach {
                                                val deviceMac = it.address
                                                if (deviceMac.startsWith(Const.MAC_HEADER_1)
                                                    || deviceMac.startsWith(Const.MAC_HEADER_2)
                                                    || deviceMac.startsWith(Const.MAC_HEADER_3)
                                                ) {
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

                                        getAdapter()!!.closeProfileProxy(profile, proxy)
                                    }
                                }
                            }
                            @Suppress("DEPRECATION")
                            am.isWiredHeadsetOn -> {
                                am.isBluetoothScoOn = false
                                am.isSpeakerphoneOn = false
                                am.stopBluetoothSco()
                            }
                            else -> {
                                am.isBluetoothScoOn = false
                                am.isSpeakerphoneOn = true
                                am.stopBluetoothSco()
                            }
                        }
                    }

                    am.mode = AudioManager.MODE_NORMAL
                }
        )
    }

    /* 是否使用FreedConn产品 */
    private fun checkFreedconn() {
        if (BluetoothHelper.isBluetoothConnected()) {
            getAdapter()!!.getProfileProxy(baseContext, getConnectedProfile()) {
                onServiceConnected { profile, proxy ->
                    val mDevices = proxy.connectedDevices
                    if (!mDevices.isNullOrEmpty()) {
                        mDevices.forEach {
                            val deviceMac = it.address
                            if (Const.MAC_HEADER_1 !in deviceMac
                                && Const.MAC_HEADER_2 !in deviceMac
                                && Const.MAC_HEADER_3 !in deviceMac) {
                                longToast("请使用 Freedconn 产品")
                            }
                        }
                    }

                    getAdapter()!!.closeProfileProxy(profile, proxy)
                }
            }
        }
    }

    /* 开始对讲抢麦 */
    private fun startTalkToGrab() {
        val accid = getString("accid")
        val priority = list.firstOrNull { it.mobile == accid }?.priority ?: ""
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
        OkLogger.i("抢麦成功")
        sendControlCommand(chatId, TeamState.NOTIFY_GRAB_SUCCESSS) {
            onSuccess {
                isMicHolding = true
                holdingMaster = getString("accid")
                chat_dialog_hint.text = getString(R.string.network_chat_mic_done)
                setPttVoice(true)
                setVoiceLine(false)
            }
            onFailed { failedToGrab() }
            onException { failedToGrab() }
        }
    }

    /* 发送取消抢麦控制指令 */
    private fun sendCancelCommand(event: (() -> Unit)) {
        OkLogger.i("取消抢麦")
        sendControlCommand(chatId, TeamState.NOTIFY_GRAB_CANCEL) {
            onSuccess {
                isMicHolding = false
                holdingMaster = ""
                event()
            }
        }
    }

    /* 设置抢麦弹框是否显示 */
    private fun setGrabAnimation(isBegan: Boolean) {
        chat_dialog.visibility = if (isBegan) View.VISIBLE else View.GONE
        chat_dialog_hint.text = getString(R.string.network_chat_mic_holding)
        chat_dialog_img.setImageResource(R.mipmap.icon36)
    }

    /* 设置是否显示对讲、群聊按钮 */
    private fun setAdminEnable(isEnable: Boolean) {
        chat_admin.visibility = if (isEnable) View.VISIBLE else View.GONE
        chat_user.visibility = if (isEnable) View.GONE else View.VISIBLE
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

    /* 设置通话状态和信息 */
    private fun setChatting(isChatting: Boolean) {
        TeamAVChatProfile.sharedInstance().isTeamAVChatting = isChatting
        TeamAVChatProfile.sharedInstance().teamAVChatId = if (isChatting) roomName else ""
        TeamAVChatProfile.sharedInstance().teamAVChatName = if (isChatting) clusterName else ""
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

        AVChatManager.getInstance()
            .observeControlNotification(mControlEventObserver, false) //注销网络通话控制消息
        AVChatManager.getInstance()
            .observeControlNotification(mControlEventObserver, true)  //注册网络通话控制消息

        AVChatManager.getInstance().setParameter(
            AVChatParameters.KEY_SESSION_MULTI_MODE_USER_ROLE,
            AVChatUserRole.NORMAL
        ) //角色模式
        AVChatManager.getInstance().setParameter(
            AVChatParameters.KEY_AUDIO_REPORT_SPEAKER,
            true
        )                          //声音强度汇报
        joinRoom(roomName) {
            //加入房间
            onSuccess { data ->
                chatId = data!!.chatId
                setChatting(true)
                accountsOnline.add(getString("accid"))
                AVChatManager.getInstance().muteAllRemoteAudio(false) //是否允许播放远端用户语音

                val accid = getString("accid")
                val priority = list.firstOrNull { it.mobile == accid }?.priority ?: ""
                val isFirst = accid == roomMaster || priority == "0"
                chat_level.visibility = if (accid == roomMaster) View.VISIBLE else View.GONE

                if (isFirst) setAdminEnable(chatMode == TeamState.CHAT_NONE)
                else setAdminEnable(false)

                when (chatMode) {
                    TeamState.CHAT_TALK -> {
                        setTalkMode(true)   //开启对讲模式
                        setGroupMode(false) //关闭群聊模式
                        chat_hint.text = getString(R.string.network_chat_hint1)
                    }
                    TeamState.CHAT_GROUP -> {
                        setTalkMode(false) //关闭对讲模式
                        setGroupMode(true) //开启群聊模式
                        chat_hint.text = getString(R.string.network_chat_hint2)
                    }
                    TeamState.CHAT_NONE -> {
                        setTalkMode(false)  //关闭对讲模式
                        setGroupMode(false) //关闭群聊模式
                        chat_hint.text = " "
                    }
                }

                setVoiceLine(false) //关闭波浪线
                chat_ptt.setImageResource(R.mipmap.icon34)
                chat_mic.setImageResource(R.mipmap.icon28)
                chat_voice.setImageResource(R.mipmap.icon29)
                chat_hint.text = when (chatMode) {
                    TeamState.CHAT_TALK -> getString(R.string.network_chat_hint1)
                    TeamState.CHAT_GROUP -> getString(R.string.network_chat_hint2)
                    else -> " "
                }

                AVChatManager.getInstance().muteLocalAudio(true)

                //非管理员不开启对讲
                if (chat_user.isVisble()) {
                    setMuteAll(true)
                    setLocalMicMute(true)
                    setLocalAudioMute(true)
                    setVoiceLine(false)
                    chat_ptt.setImageResource(R.mipmap.icon34)
                }

                getAllStatusData("1") //对讲模式关闭
                chat_ptt.invisible()

                updateTiming()
                forceVoiceToBluetooth()
                updateOnlineData()

                sendAdminCommand()
                checkAdminCommand()
            }
            onFailed {
                showToast(getString(R.string.network_chat_error_join))
                finish()
            }
        }
    }

    /* 通话控制指令观察者 */
    @Suppress("DEPRECATION")
    private val mControlEventObserver = Observer<AVChatControlEvent> { event ->
        if (event.chatId != chatId) return@Observer

        when (event.controlCommand) {
            TeamState.NOTIFY_CUSTOM_NONE -> {
                //关闭抢麦
                isMicHolding = false
                holdingMaster = ""
                if (chat_dialog.isVisble()) {
                    setGrabAnimation(false)
                    TeamSoundPlayer.instance().play()
                }

                modeMaster = ""
                chatMode = TeamState.CHAT_NONE
                setTalkMode(false)  //关闭对讲模式
                setGroupMode(false) //关闭群聊模式
                setVoiceLine(false) //关闭波浪线

                chat_ptt.setImageResource(R.mipmap.icon34)
                chat_mic.setImageResource(R.mipmap.icon28)
                chat_voice.setImageResource(R.mipmap.icon29)
                chat_ptt.invisible()

                chat_hint.text = " "
                chat_talk.setBackgroundResource(R.mipmap.btn07)
                chat_talk.setTextColor(resources.getColor(R.color.light))

                AVChatManager.getInstance().muteLocalAudio(true)

                /* 普通用户关闭对讲 */
                setMuteAll(true)
                setLocalMicMute(true)
                setLocalAudioMute(true)
                setVoiceLine(false)
                TeamAVChatProfile.sharedInstance().isTeamAVEnable = false
                getAllStatusData("1")

                /* 群主、优先者开启管理员模式 */
                val accid = getString("accid")
                val priority = list.firstOrNull { it.mobile == accid }?.priority ?: ""
                val isFirst = accid == roomMaster || priority == "0"
                if (isFirst) {
                    setAdminEnable(true)
                    setMuteAll(false)
                    setLocalMicMute(false)
                    setLocalAudioMute(false)
                    setVoiceLine(false)
                }
            }
            TeamState.NOTIFY_CUSTOM_TALK -> {
                modeMaster = event.account
                chatMode = TeamState.CHAT_TALK
                chat_hint.text = getString(R.string.network_chat_hint1)

                setTalkMode(true)   //开启对讲模式
                setGroupMode(false) //关闭群聊模式

                setAdminEnable(false)
                setMuteAll(true)
                setLocalMicMute(true)
                setLocalAudioMute(true)
                setVoiceLine(false)
                chat_ptt.setImageResource(R.mipmap.icon34)
                getAllStatusData("1")
            }
            TeamState.NOTIFY_CUSTOM_GROUP -> {
                modeMaster = event.account
                chatMode = TeamState.CHAT_GROUP
                chat_hint.text = getString(R.string.network_chat_hint2)

                setTalkMode(false) //关闭对讲模式
                setGroupMode(true) //开启群聊模式

                setAdminEnable(false)
                setMuteAll(true)
                setLocalMicMute(true)
                setLocalAudioMute(true)
                setVoiceLine(false)
                chat_ptt.setImageResource(R.mipmap.icon34)
                getAllStatusData("1")
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
            TeamState.NOTIFY_GRAB_ADMIN -> {
                modeMaster = event.account
                mAdminCount++

                OkLogger.e("admin$mMemberCount")
            }
        }
    }

    /* 音视频状态观察者 */
    private val mStateObserver = object : _AVChatStateObserver() {

        @SuppressLint("SetTextI18n")
        override fun onUserJoined(account: String) {
            OkLogger.i("用户：${account}加入房间")
            if (accountsOnline.none { it == account }) accountsOnline.add(account)

            if (chatMode != TeamState.CHAT_NONE) {
                if (chatMode == TeamState.CHAT_GROUP) {
                    val accid = getString("accid")
                    val priority = list.firstOrNull { it.mobile == accid }?.priority ?: ""
                    val isFirst = accid == roomMaster || priority == "0"

                    chat_ptt.visibility =
                        if (isFirst && !isLocalAllMute && accountsOnline.size > 1) View.VISIBLE
                        else View.INVISIBLE
                } else {
                    chat_ptt.visibility =
                        if (!isLocalAllMute && accountsOnline.size > 1) View.VISIBLE
                        else View.INVISIBLE
                }
            }

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

            if (chatMode != TeamState.CHAT_NONE) {
                if (chatMode == TeamState.CHAT_GROUP) {
                    val accid = getString("accid")
                    val priority = list.firstOrNull { it.mobile == accid }?.priority ?: ""
                    val isFirst = accid == roomMaster || priority == "0"

                    chat_ptt.visibility =
                        if (isFirst && !isLocalAllMute && accountsOnline.size > 1) View.VISIBLE
                        else View.INVISIBLE
                } else {
                    chat_ptt.visibility =
                        if (!isLocalAllMute && accountsOnline.size > 1) View.VISIBLE
                        else View.INVISIBLE
                }
            }
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
            speakers.forEach {
                OkLogger.i("onReportSpeaker：用户：${it.key}， 声音强度：${it.value}")

                if (it.key == getString("accid")) {
                    val value = (it.value / 150f + 0.5).toInt()

                    chat_curve.setVolume(value)

                    if (isMicHolding) {
                        when (value) {
                            in 0..5 -> chat_dialog_img.setImageResource(R.mipmap.icon36)
                            in 6..10 -> chat_dialog_img.setImageResource(R.mipmap.icon37)
                            in 11..15 -> chat_dialog_img.setImageResource(R.mipmap.icon38)
                            in 16..20 -> chat_dialog_img.setImageResource(R.mipmap.icon42)
                        }
                    }
                }
            }
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

    override fun onBackPressed() {
        /*if (getString("accid") == modeMaster) {
            getStatusData(0) {
                sendControlCommand(
                    chatId,
                    TeamState.NOTIFY_CUSTOM_NONE
                ) { super.onBackPressed() }
            }
        } else super.onBackPressed()*/

        startActivity(
            Intent(baseContext, NetworkActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    /* 退出 */
    override fun finish() {
        if (modeMaster.isNotEmpty()
            && getString("accid") == modeMaster
        ) {
            getStatusData(0) {
                sendControlCommand(
                    chatId,
                    TeamState.NOTIFY_CUSTOM_NONE
                ) {
                    onSuccess {
                        hangUp()
                        setChatting(false)
                        activeCallingNotifier(false)

                        mCompositeDisposable.clear()
                        TeamSoundPlayer.instance().stop()
                        EventBus.getDefault().unregister(this@NetworkChatActivity)
                        super.finish()
                    }
                }
            }
        } else {
            hangUp()
            setChatting(false)
            activeCallingNotifier(false)

            mCompositeDisposable.clear()
            TeamSoundPlayer.instance().stop()
            EventBus.getDefault().unregister(this@NetworkChatActivity)
            super.finish()
        }
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
                    if (accounts.none { it == getString("accid") }) finish()
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
                val accid = getString("accid")
                val priority = list.firstOrNull { it.mobile == accid }?.priority ?: ""

                getInfoData {
                    val priorityInner = list.firstOrNull { it.mobile == accid }?.priority ?: ""

                    if (priority == "0" && priorityInner == "1") {
                        if (chatMode == TeamState.CHAT_NONE) {
                            setAdminEnable(false)
                            setMuteAll(true)
                            setLocalMicMute(true)
                            setLocalAudioMute(true)
                            setVoiceLine(false)
                            chat_ptt.setImageResource(R.mipmap.icon34)
                            chat_ptt.invisible()
                        } else {
                            if (modeMaster == accid) {
                                setTalkMode(false)  //关闭对讲模式
                                setGroupMode(false) //关闭群聊模式
                                setVoiceLine(false) //关闭波浪线
                                chat_hint.text = " "
                                chat_ptt.setImageResource(R.mipmap.icon34)
                                chat_mic.setImageResource(R.mipmap.icon28)
                                chat_voice.setImageResource(R.mipmap.icon29)
                                chat_ptt.invisible()
                                AVChatManager.getInstance().muteLocalAudio(true)

                                setAdminEnable(false)
                                setMuteAll(true)
                                setLocalMicMute(true)
                                setLocalAudioMute(true)
                                setVoiceLine(false)
                                getAllStatusData("1")

                                modeMaster = ""
                                chatMode = TeamState.CHAT_NONE

                                getStatusData(0) {
                                    sendControlCommand(chatId, TeamState.NOTIFY_CUSTOM_NONE) { }
                                }
                            } else {
                                if (chatMode == TeamState.CHAT_GROUP) chat_ptt.invisible()
                            }
                        }
                    }

                    if (priority == "1" && priorityInner == "0") {
                        when (chatMode) {
                            TeamState.CHAT_GROUP -> if (accountsOnline.size > 1) chat_ptt.visible()
                            TeamState.CHAT_NONE -> {
                                setAdminEnable(true)
                                setMuteAll(false)
                                setLocalMicMute(false)
                                setLocalAudioMute(false)
                                setVoiceLine(false)
                                chat_mic.setImageResource(R.mipmap.icon28)
                                chat_voice.setImageResource(R.mipmap.icon29)
                                chat_ptt.setImageResource(R.mipmap.icon34)
                                chat_ptt.invisible()
                            }
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
            "来电响铃" -> {
                /*val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.mode = AudioManager.MODE_NORMAL*/
            }
            "电话挂断" -> {
                if (!isLocalAudioMute) AVChatManager.getInstance().muteAllRemoteAudio(false)
                if (!isLocalMute) AVChatManager.getInstance().muteLocalAudio(false)
                AVChatManager.getInstance().setSpeaker(!BluetoothHelper.isBluetoothConnected())
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        UMShareAPI.get(this@NetworkChatActivity).onActivityResult(requestCode, resultCode, data)
    }

    /* ble设备回调 */
    override fun onRecive(data_char: BluetoothGattCharacteristic) {
        if (!isLocalMute
            && chatMode != TeamState.CHAT_NONE
        ) {

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
