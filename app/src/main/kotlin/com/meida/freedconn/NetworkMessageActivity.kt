package com.meida.freedconn

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.lzg.extend.BaseResponse
import com.lzg.extend.StringDialogCallback
import com.lzg.extend.jackson.JacksonDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.meida.base.BaseActivity
import com.meida.base.*
import com.meida.chatkit.TeamAVChatEx
import com.meida.model.CommonData
import com.meida.model.RefreshMessageEvent
import com.meida.share.BaseHttp
import kotlinx.android.synthetic.main.layout_empty.*
import kotlinx.android.synthetic.main.layout_list.*
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.include
import org.jetbrains.anko.toast

class NetworkMessageActivity : BaseActivity() {

    private val list = ArrayList<Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        include<View>(R.layout.layout_list)
        init_title(getString(R.string.network_msg_title))

        swipe_refresh.isRefreshing = true
        getData()
    }

    override fun init_title() {
        super.init_title()
        empty_hint.text = getString(R.string.empty_hint_msg)
        swipe_refresh.refresh { getData() }
        recycle_list.load_Linear(baseContext, swipe_refresh)
        mAdapter = SlimAdapter.create()
            .register<CommonData>(R.layout.item_contact_message_list) { data, injector ->

                val index = list.indexOf(data)

                injector.text(R.id.item_msg_name, data.userName)
                    .visibility(R.id.item_msg_status, if (data.status == "0") View.VISIBLE else View.GONE)
                    .visibility(R.id.item_msg_hint, if (data.status == "0") View.VISIBLE else View.GONE)
                    .visibility(R.id.item_msg_hint2, if (data.status == "1") View.VISIBLE else View.GONE)
                    .with<ImageView>(R.id.item_msg_img) {
                        it.setImageURL(BaseHttp.baseImg + data.userHead)
                    }

                    .clicked(R.id.item_msg_agree) {
                        OkGo.post<String>(BaseHttp.add_friend)
                            .tag(this@NetworkMessageActivity)
                            .headers("token", getString("token"))
                            .params("accountInfoId", data.accountInfoId)
                            .params("friendRequestId", data.friendRequestId)
                            .execute(object : StringDialogCallback(baseContext) {

                                override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {
                                    toast(msg)
                                    data.status = "1"
                                    mAdapter.notifyItemChanged(index)
                                    EventBus.getDefault().post(RefreshMessageEvent("同意添加"))
                                    TeamAVChatEx.onAgreeFriendSuccess(baseContext, data.mobile)
                                }

                            })
                    }
                    .clicked(R.id.item_msg_refuse) {
                        OkGo.post<String>(BaseHttp.lose_request)
                            .tag(this@NetworkMessageActivity)
                            .headers("token", getString("token"))
                            .params("friendRequestId", data.friendRequestId)
                            .execute(object : StringDialogCallback(baseContext) {

                                override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {
                                    toast(msg)
                                    list.removeAt(index)
                                    mAdapter.notifyItemRemoved(index)

                                    empty_view.apply { if (list.isEmpty()) visible() else gone() }
                                }

                            })
                    }
            }
            .attachTo(recycle_list)
    }

    override fun getData() {
        OkGo.post<BaseResponse<ArrayList<CommonData>>>(BaseHttp.friend_request_list)
            .tag(this@NetworkMessageActivity)
            .headers("token", getString("token"))
            .execute(object :
                JacksonDialogCallback<BaseResponse<ArrayList<CommonData>>>(baseContext) {

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
}
