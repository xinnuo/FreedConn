package com.meida.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.lzg.extend.BaseResponse
import com.lzg.extend.StringDialogCallback
import com.lzg.extend.jackson.JacksonDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.meida.base.BaseFragment
import com.meida.base.*
import com.meida.chatkit.TeamAVChatEx
import com.meida.freedconn.R
import com.meida.model.CommonData
import com.meida.model.RefreshMessageEvent
import com.meida.share.BaseHttp
import com.meida.utils.trimString
import kotlinx.android.synthetic.main.fragment_talk.*
import kotlinx.android.synthetic.main.layout_empty.*
import kotlinx.android.synthetic.main.layout_list.*
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.sdk25.listeners.onClick
import org.jetbrains.anko.support.v4.toast
import org.json.JSONObject

class TalkFragment : BaseFragment() {

    private val list = ArrayList<CommonData>()
    private val accounts = ArrayList<String>()
    private val accountIds = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_talk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init_title()

        swipe_refresh.isRefreshing = true
        getData()
    }

    override fun init_title() {
        empty_hint.text = getString(R.string.empty_hint_friend)
        swipe_refresh.refresh { getData() }
        recycle_list.load_Linear(activity!!, swipe_refresh)

        mAdapter = SlimAdapter.create()
            .register<CommonData>(R.layout.item_talk_list) { data, injector ->

                val index = list.indexOf(data)

                injector.text(R.id.item_talk_name, data.userName)
                    .checked(R.id.item_talk_check, data.isChecked)
                    .with<ImageView>(R.id.item_talk_img) {
                        it.setImageURL(BaseHttp.baseImg + data.userHead)
                    }
                    .clicked(R.id.item_talk) {
                        data.isChecked = !data.isChecked
                        mAdapter.notifyItemChanged(index)
                    }
            }
            .attachTo(recycle_list)

        talk_left.onClick { (activity as OnFragmentListener).onViewClick("back") }
        talk_right.onClick { _ ->
            if (talk_name.text.isBlank()) {
                talk_name.requestFocus()
                toast(getString(R.string.network_noName))
                return@onClick
            }

            list.filter { it.isChecked }.forEach {
                accountIds.add(it.accountInfoId)
                accounts.add(it.telephone)
            }

            OkGo.post<String>(BaseHttp.create_cluster)
                .tag(this@TalkFragment)
                .isMultipart(true)
                .headers("token", getString("token"))
                .params("clusterName", talk_name.text.trimString())
                .params("command", talk_code.text.toString())
                .params("accountInfoIds", accountIds.joinToString(","))
                .execute(object : StringDialogCallback(activity) {

                    override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {

                        val obj = JSONObject(response.body())
                            .optJSONObject("object") ?: JSONObject()

                        val roomNameId = obj.optString("clusterId")

                        toast(getString(R.string.network_chat_success_create))
                        EventBus.getDefault().post(RefreshMessageEvent("创建群组"))
                        TeamAVChatEx.onCreateRoomSuccess(activity!!, roomNameId, accounts)
                        (activity as OnFragmentListener).onViewClick("done")
                    }

                })
        }
    }

    override fun getData() {
        OkGo.post<BaseResponse<ArrayList<CommonData>>>(BaseHttp.friend_list)
            .tag(this@TalkFragment)
            .headers("token", getString("token"))
            .execute(object :
                JacksonDialogCallback<BaseResponse<ArrayList<CommonData>>>(activity) {

                override fun onSuccess(response: Response<BaseResponse<ArrayList<CommonData>>>) {

                    list.apply {
                        clear()
                        addItems(response.body().`object`)
                    }

                    mAdapter.updateData(list)
                }

                override fun onFinish() {
                    super.onFinish()
                    swipe_refresh.isRefreshing = false

                    empty_view.apply { if (list.isEmpty()) visible() else gone() }
                }

            })
    }

    override fun onDestroy() {
        super.onDestroy()
        OkGo.getInstance().cancelTag(this@TalkFragment)
    }

}
