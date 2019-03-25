package com.meida.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.daimajia.swipe.util.Attributes
import com.lqr.ninegridimageview.LQRNineGridImageView
import com.lzg.extend.BaseResponse
import com.lzg.extend.StringDialogCallback
import com.lzg.extend.jackson.JacksonDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.netease.nim.avchatkit.AVChatKit
import com.meida.adapter.ContactAdapter
import com.meida.base.BaseFragment
import com.meida.base.*
import com.meida.chatkit.TeamAVChatEx
import com.meida.chatkit.TeamAVChatProfile
import com.meida.freedconn.*
import com.meida.model.CommonData
import com.meida.model.CommonModel
import com.meida.model.RefreshMessageEvent
import com.meida.share.BaseHttp
import com.meida.utils.*
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_contact.*
import kotlinx.android.synthetic.main.layout_empty.*
import kotlinx.android.synthetic.main.layout_list.*
import net.cachapa.expandablelayout.ExpandableLayout
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.sdk25.listeners.onClick
import org.jetbrains.anko.support.v4.startActivity
import org.jetbrains.anko.support.v4.toast
import java.util.concurrent.TimeUnit

class ContactFragment : BaseFragment() {

    private val list = ArrayList<CommonData>()
    private lateinit var mListAdapter: ContactAdapter
    private lateinit var msgData: CommonData
    private var keyWord = ""

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init_title()

        EventBus.getDefault().register(this@ContactFragment)

        swipe_refresh.isRefreshing = true
        recycle_list.adapter = mListAdapter
        list.add(msgData)
        mListAdapter.updateData(list)

        getData()
    }

    override fun init_title() {
        msgData = CommonData().apply { requtsetCount = "0" }

        swipe_refresh.refresh {
            if (keyWord.isEmpty()) getData()
            else getSearchData()
        }
        recycle_list.load_Linear(activity!!, swipe_refresh)
        //通讯录适配器
        mListAdapter = ContactAdapter(activity).apply {
            mode = Attributes.Mode.Single

            setOnItemClickListener {
                if (it == 0) startActivity<NetworkMessageActivity>()
                else {
                    val timeRemain = getString("residueTime").toNotInt()
                    if (timeRemain < 1) {
                        toast(getString(R.string.network_chat_no_time))
                        return@setOnItemClickListener
                    }

                    if (list[it].clusterId.isNotEmpty()) {

                        if (TeamAVChatProfile.sharedInstance().isTeamAVChatting) {
                            val chatId = TeamAVChatProfile.sharedInstance().teamAVChatId
                            val chatName = TeamAVChatProfile.sharedInstance().teamAVChatName
                            when {
                                chatId == list[it].clusterId -> AVChatKit.outgoingTeamCall(
                                        activity,
                                        list[it].clusterId
                                )
                                TeamAVChatProfile.sharedInstance().isTeamAVEnable -> DialogHelper.showHintDialog(
                                        activity,
                                        "加入群聊",
                                        "${chatName}群正在对讲中，是否结束该的对讲",
                                        "取消",
                                        "确定",
                                        false
                                ) { hint ->
                                    if (hint == "yes") {
                                        ActivityStack.screenManager.popActivities(
                                                NetworkChatActivity::class.java
                                        )

                                        Completable.timer(500, TimeUnit.MILLISECONDS)
                                                .subscribeOn(Schedulers.io())
                                                .subscribe {
                                                    if (!TeamAVChatProfile.sharedInstance().isTeamAVChatting) {
                                                        AVChatKit.outgoingTeamCall(
                                                                activity,
                                                                list[it].clusterId
                                                        )
                                                    }
                                                }
                                    }
                                }
                                else -> {
                                    ActivityStack.screenManager.popActivities(NetworkChatActivity::class.java)

                                    Completable.timer(500, TimeUnit.MILLISECONDS)
                                            .subscribeOn(Schedulers.io())
                                            .subscribe {
                                                if (!TeamAVChatProfile.sharedInstance().isTeamAVChatting) {
                                                    AVChatKit.outgoingTeamCall(
                                                            activity,
                                                            list[it].clusterId
                                                    )
                                                }
                                            }
                                }
                            }
                        } else
                            AVChatKit.outgoingTeamCall(activity, list[it].clusterId)
                    }
                }
            }

            setOnItemDeleteClickListener { index ->
                if (list[index].clusterId.isEmpty()) {
                    OkGo.post<String>(BaseHttp.del_friend)
                            .tag(this@ContactFragment)
                            .headers("token", getString("token"))
                            .params("friendId", list[index].friendId)
                            .execute(object : StringDialogCallback(activity) {

                                override fun onSuccessResponse(
                                        response: Response<String>,
                                        msg: String,
                                        msgCode: String
                                ) {
                                    toast(msg)
                                    TeamAVChatEx.onDelFriendSuccess(activity!!, list[index].mobile)
                                    list.removeAt(index)
                                    this@apply.updateData(list)
                                }

                            })
                } else {
                    if (TeamAVChatProfile.sharedInstance().isTeamAVChatting
                            && TeamAVChatProfile.sharedInstance().teamAVChatId == list[index].clusterId
                    ) {
                        if (TeamAVChatProfile.sharedInstance().isTeamAVEnable) {
                            toast("正在网络对讲，无法操作")
                            return@setOnItemDeleteClickListener
                        } else {
                            ActivityStack.screenManager.popActivities(NetworkChatActivity::class.java)

                            val datas = ArrayList<CommonData>()
                            datas.addItems(list[index].clusterMembers)

                            when (datas.size) {
                                0 -> {
                                    list.removeAt(index)
                                    mListAdapter.updateData(list)
                                }
                                1 -> getQuitData(index)
                                else -> {
                                    if (datas.any { it.master == "0" }) {
                                        val item = datas.first { it.master == "0" }
                                        if (getString("token") == item.accountInfoId) {
                                            DialogHelper.showHintDialog(
                                                    activity,
                                                    "退出群聊",
                                                    "确定要退出并转让群主吗？",
                                                    "取消",
                                                    "确定",
                                                    false
                                            ) {
                                                if (it == "yes") {
                                                    startActivity<NetworkHandleActivity>(
                                                            "type" to "4",
                                                            "position" to index.toString(),
                                                            "list" to datas
                                                    )
                                                }
                                            }
                                        } else getQuitData(index)

                                    } else {
                                        getQuitData(index)
                                    }
                                }
                            }
                        }
                    } else {
                        val datas = ArrayList<CommonData>()
                        datas.addItems(list[index].clusterMembers)

                        when (datas.size) {
                            0 -> {
                                list.removeAt(index)
                                mListAdapter.updateData(list)
                            }
                            1 -> getQuitData(index)
                            else -> {
                                if (datas.any { it.master == "0" }) {
                                    val item = datas.first { it.master == "0" }
                                    if (getString("token") == item.accountInfoId) {
                                        DialogHelper.showHintDialog(
                                                activity,
                                                "退出群聊",
                                                "确定要退出并转让群主吗？",
                                                "取消",
                                                "确定",
                                                false
                                        ) {
                                            if (it == "yes") {
                                                startActivity<NetworkHandleActivity>(
                                                        "type" to "4",
                                                        "position" to index.toString(),
                                                        "list" to datas
                                                )
                                            }
                                        }
                                    } else getQuitData(index)

                                } else {
                                    getQuitData(index)
                                }
                            }
                        }
                    }
                }
            }
        }
        //查找适配器
        mAdapter = SlimAdapter.create()
                .register<CommonData>(R.layout.item_contact_check_list) { data, injector ->

                    val index = list.indexOf(data)

                    injector.text(
                            R.id.item_check_name,
                            if (data.clusterId.isEmpty()) data.userName else data.clusterName
                    )
                            .text(
                                    R.id.item_check_add,
                                    getString(if (data.clusterId.isEmpty()) R.string.network_check_add else R.string.network_check_join)
                            )
                            .visibility(
                                    R.id.item_check_add,
                                    if (data.accountInfoId == getString("token")
                                            || data.whetherCluster == "1"
                                            || data.friend == "1"
                                    ) View.GONE else View.VISIBLE
                            )

                            .with<LQRNineGridImageView<String>>(R.id.item_check_nine) { view ->
                                view.setAdapter {
                                    onDisplayImage { _, imageView, url ->
                                        imageView.setImageURL(BaseHttp.baseImg + url)
                                    }
                                }
                                if (data.clusterId.isEmpty()) view.setImagesData(listOf(data.userHead))
                                else {
                                    val items = ArrayList<CommonData>()
                                    val imgs = ArrayList<String>()
                                    items.addItems(data.clusterMembers)
                                    items.mapTo(imgs) { it.userHead }
                                    view.setImagesData(imgs)
                                }
                            }

                            .with<ExpandableLayout>(R.id.item_check_expand) {
                                if (data.isExpanded) it.expand()
                                else it.collapse()
                            }

                            .with<EditText>(R.id.item_check_code) {

                                if (it.tag != null && it.tag is TextWatcher) {
                                    it.removeTextChangedListener(it.tag as TextWatcher)
                                }

                                it.setText(data.commandLocal)
                                it.setSelection(it.text.length)

                                val textWatcher = object : _TextWatcher() {
                                    override fun afterTextChanged(s: Editable) {
                                        data.commandLocal = s.toString()
                                    }
                                }

                                it.addTextChangedListener(textWatcher)
                                it.tag = textWatcher
                            }

                            .clicked(R.id.item_check_add) {
                                if (data.clusterId.isEmpty()) getAppendData(data.accountInfoId, data.mobile)
                                else {
                                    data.isExpanded = !data.isExpanded
                                    data.commandLocal = ""
                                    mAdapter.notifyItemChanged(index)
                                }
                            }

                            .clicked(R.id.item_check_join) { _ ->
                                if (data.command.isNotEmpty() && data.command != data.commandLocal) {
                                    toast(getString(R.string.network_chat_error_code))
                                    return@clicked
                                }

                                OkGo.post<String>(BaseHttp.jion_cluster)
                                        .tag(this@ContactFragment)
                                        .headers("token", getString("token"))
                                        .params("clusterId", data.clusterId)
                                        .params("command", data.commandLocal)
                                        .execute(object : StringDialogCallback(activity) {

                                            override fun onSuccessResponse(
                                                    response: Response<String>,
                                                    msg: String,
                                                    msgCode: String
                                            ) {
                                                toast(msg)

                                                val items = ArrayList<CommonData>()
                                                val accounts = ArrayList<String>()
                                                items.addItems(data.clusterMembers)
                                                items.mapTo(accounts) { it.mobile }
                                                TeamAVChatEx.onJoinRoomSuccess(
                                                        activity!!,
                                                        data.clusterId,
                                                        accounts
                                                )
                                                contact_close.performClick()
                                            }

                                        })
                            }
                }

        contact_edit.addTextChangedListener(this@ContactFragment)
        contact_edit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                KeyboardHelper.hideSoftInput(activity!!) //隐藏软键盘

                if (contact_edit.text.isBlank()) {
                    toast(getString(R.string.network_empty_hint))
                } else {
                    keyWord = contact_edit.text.trimString()
                    updateSearchData()
                }
            }
            return@setOnEditorActionListener false
        }

        contact_close.onClick { contact_edit.setText("") }
        contact_back.onClick { (activity as OnFragmentListener).onViewClick("back") }
    }

    override fun getData() {
        OkGo.post<BaseResponse<CommonModel>>(BaseHttp.phoneBook_list)
                .tag(this@ContactFragment)
                .headers("token", getString("token"))
                .execute(object : JacksonDialogCallback<BaseResponse<CommonModel>>(activity) {

                    override fun onSuccess(response: Response<BaseResponse<CommonModel>>) {

                        list.apply {
                            clear()
                            add(msgData)
                            addItems(response.body().`object`.clusters)
                            addItems(response.body().`object`.friends)
                        }

                        if (TeamAVChatProfile.sharedInstance().isTeamAVChatting) {
                            val teamId = TeamAVChatProfile.sharedInstance().teamAVChatId
                            val index = list.indexOfFirst { it.clusterId == teamId }
                            if (index > 0) {
                                val item = list[index]
                                item.isTalking = true
                                list.removeAt(index)
                                list.add(1, item)
                            }
                        }

                        mListAdapter.updateData(list)
                        recycle_list.adapter = mListAdapter
                        getMessageCount()
                    }

                    override fun onFinish() {
                        super.onFinish()
                        swipe_refresh.isRefreshing = false
                    }

                })
    }

    private fun getSearchData() {
        OkGo.post<BaseResponse<CommonModel>>(BaseHttp.cluster_list)
                .tag(this@ContactFragment)
                .isMultipart(true)
                .headers("token", getString("token"))
                .params("parama", keyWord)
                .execute(object : JacksonDialogCallback<BaseResponse<CommonModel>>(activity) {

                    override fun onSuccess(response: Response<BaseResponse<CommonModel>>) {

                        list.apply {
                            clear()
                            addItems(response.body().`object`.clusters)
                            addItems(response.body().`object`.friends)
                        }

                        mAdapter.attachTo(recycle_list)
                        mAdapter.updateData(list)
                    }

                    override fun onFinish() {
                        super.onFinish()
                        swipe_refresh.isRefreshing = false

                        empty_view.apply { if (list.isEmpty()) visible() else gone() }
                    }

                })
    }

    private fun getQuitData(index: Int, userId: String = "") {
        OkGo.post<String>(BaseHttp.quit_cluster)
                .tag(this@ContactFragment)
                .headers("token", getString("token"))
                .params("clusterId", list[index].clusterId)
                .params("accountInfoId", userId)
                .execute(object : StringDialogCallback(activity) {

                    override fun onSuccessResponse(
                            response: Response<String>,
                            msg: String,
                            msgCode: String
                    ) {
                        toast(msg)
                        val items = ArrayList<CommonData>()
                        val accounts = ArrayList<String>()
                        val inner = list.removeAt(index)
                        items.addItems(inner.clusterMembers)
                        items.filter { it.accountInfoId != getString("token") }
                                .mapTo(accounts) { it.mobile }
                        TeamAVChatEx.onQuitRoomSuccess(
                                activity!!,
                                inner.clusterId,
                                accounts
                        )

                        mListAdapter.updateData(list)
                    }

                })
    }

    private fun getMessageCount() {
        OkGo.post<BaseResponse<ArrayList<CommonData>>>(BaseHttp.friend_request_list)
                .tag(this@ContactFragment)
                .headers("token", getString("token"))
                .execute(object :
                        JacksonDialogCallback<BaseResponse<ArrayList<CommonData>>>(activity) {

                    override fun onSuccess(response: Response<BaseResponse<ArrayList<CommonData>>>) {

                        val items = ArrayList<CommonData>()
                        items.apply {
                            clear()
                            addItems(response.body().`object`)
                        }

                        msgData.requtsetCount = items.filter { it.status == "0" }.size.toString()
                        if (recycle_list.adapter is ContactAdapter) {
                            mListAdapter.notifyItemChanged(0)
                        }
                    }

                })
    }

    private fun getAppendData(accountId: String, account: String) {
        OkGo.post<String>(BaseHttp.add_friend_request)
                .tag(this@ContactFragment)
                .headers("token", getString("token"))
                .params("friendId", accountId)
                .execute(object : StringDialogCallback(activity) {

                    override fun onSuccessResponse(
                            response: Response<String>,
                            msg: String,
                            msgCode: String
                    ) {
                        toast(msg)
                        TeamAVChatEx.onAddFriendSuccess(activity!!, account)
                        contact_close.performClick()
                    }

                })
    }

    private fun updateListData() {
        swipe_refresh.isRefreshing = true

        empty_view.gone()
        if (list.isNotEmpty()) {
            list.clear()
            mAdapter.notifyDataSetChanged()
        }

        getData()
    }

    private fun updateSearchData() {
        swipe_refresh.isRefreshing = true

        empty_view.gone()
        if (list.isNotEmpty()) {
            list.clear()
            mListAdapter.updateData(list)
        }

        getSearchData()
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        contact_close.visibility = if (s.isEmpty()) View.GONE else View.VISIBLE
        if (s.isEmpty() && keyWord.isNotEmpty()) {
            keyWord = ""
            updateListData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        OkGo.getInstance().cancelTag(this@ContactFragment)
        EventBus.getDefault().unregister(this@ContactFragment)
    }

    @Subscribe
    fun onMessageEvent(event: RefreshMessageEvent) {
        when (event.type) {
            "同意添加", "创建群组", "踢出群组", "拉入群组",
            "踢出群组通知", "拉入群组通知", "删好友通知",
            "好友同意通知", "创建群组通知", "加入群组通知",
            "退出群组通知", "修改群名" -> {
                swipe_refresh.isRefreshing = true
                if (keyWord.isEmpty()) getData() else getSearchData()
            }
            "加好友通知" -> getMessageCount()
            "指定群主" -> getQuitData(event.id.toInt(), event.name)
        }
    }

}
