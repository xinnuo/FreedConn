package com.meida.freedconn

import android.annotation.SuppressLint
import android.graphics.Color
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
import com.meida.model.CommonData
import com.meida.model.RefreshMessageEvent
import com.meida.share.BaseHttp
import com.meida.utils.ActivityStack
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_empty.*
import kotlinx.android.synthetic.main.layout_list.*
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.include
import org.jetbrains.anko.sdk25.listeners.onClick
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit

class NetworkHandleActivity : BaseActivity() {

    private val list = ArrayList<CommonData>()
    private val listHave = ArrayList<CommonData>()
    private var type = ""
    private val mDisposable by lazy { CompositeDisposable() }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        include<View>(R.layout.layout_list)
        init_title("", "")

        type = intent.getStringExtra("type")
        when (type) {
            "1" -> {
                tvRight.text = getString(R.string.network_del)
                tvRight.setTextColor(Color.parseColor("#66FFFFFF"))
            }
            "2", "4" -> {
                tvRight.text = getString(R.string.network_done)
                tvRight.setTextColor(Color.parseColor("#66FFFFFF"))
            }
            "3" -> tvRight.text = getString(R.string.network_done)
        }
        @Suppress("UNCHECKED_CAST")
        val items = intent.getSerializableExtra("list") as ArrayList<CommonData>
        when (type) {
            "1" -> {
                val priorityMine = items.first { it.mobile == getString("accid") }.priority

                list.addAll(items.filter {
                    if (priorityMine == "0") it.master != "0" && it.priority != "0"
                    else it.master != "0"
                })
                mAdapter.updateData(list)

                empty_hint.text = getString(R.string.empty_hint_member)
                empty_view.apply { if (list.isEmpty()) visible() else gone() }
            }
            "2" -> {
                listHave.addAll(items.filter { it.accountInfoId != getString("token") })
                swipe_refresh.isRefreshing = true
                getData()
            }
            "3" -> {
                list.addAll(items.filter { it.accountInfoId != getString("token") })
                list.filter { it.priority == "0" }.forEach { it.isChecked = true }
                tvRight.text = getString(R.string.network_done) +
                        "(${list.filter { it.isChecked }.size})"
                mAdapter.updateData(list)

                empty_hint.text = getString(R.string.empty_hint_member)
                empty_view.apply { if (list.isEmpty()) visible() else gone() }
            }
            "4" -> {
                list.addAll(items.filter { it.accountInfoId != getString("token") })
                mAdapter.updateData(list)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun init_title() {
        super.init_title()
        empty_hint.text = getString(R.string.empty_hint_friend)
        swipe_refresh.refresh {
            if (type == "2") getData()
            else {
                mDisposable.add(
                    Completable.timer(500, TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            mAdapter.notifyDataSetChanged()
                            swipe_refresh.isRefreshing = false
                        }
                )
            }
        }
        recycle_list.load_Linear(baseContext, swipe_refresh)

        mAdapter = SlimAdapter.create()
            .register<CommonData>(R.layout.item_talk_list) { data, injector ->

                val index = list.indexOf(data)

                injector.text(R.id.item_talk_name, data.userName)
                    .checked(R.id.item_talk_check, data.isChecked)
                    .with<ImageView>(R.id.item_talk_img) {
                        it.setImageURL(BaseHttp.baseImg + data.userHead)
                    }
                    .clicked(R.id.item_talk) { _ ->
                        if (list.filter { it.isChecked }.size > 2
                            && !data.isChecked
                            && type == "3"
                        ) {
                            toast(getString(R.string.network_level_limit))
                            return@clicked
                        }

                        if (type == "4") {
                            list.filter { it.isChecked }.forEach { it.isChecked = false }
                            data.isChecked = true
                            mAdapter.notifyDataSetChanged()
                        } else {
                            data.isChecked = !data.isChecked
                            mAdapter.notifyItemChanged(index)
                        }

                        if (list.none { it.isChecked }) {
                            when (type) {
                                "1" -> {
                                    tvRight.text = getString(R.string.network_del)
                                    tvRight.setTextColor(Color.parseColor("#66FFFFFF"))
                                }
                                "2", "4" -> {
                                    tvRight.text = getString(R.string.network_done)
                                    tvRight.setTextColor(Color.parseColor("#66FFFFFF"))
                                }
                                "3" -> tvRight.text = getString(R.string.network_done) +
                                        "(${list.filter { it.isChecked }.size})"
                            }
                        } else {
                            when (type) {
                                "1" -> {
                                    tvRight.text = getString(R.string.network_del) +
                                            "(${list.filter { it.isChecked }.size})"
                                    tvRight.setTextColor(Color.parseColor("#FFFFFF"))
                                }
                                "2", "4" -> {
                                    tvRight.text = getString(R.string.network_done) +
                                            "(${list.filter { it.isChecked }.size})"
                                    tvRight.setTextColor(Color.parseColor("#FFFFFF"))
                                }
                                "3" -> tvRight.text = getString(R.string.network_done) +
                                        "(${list.filter { it.isChecked }.size})"
                            }
                        }
                    }
            }
            .attachTo(recycle_list)

        tvRight.onClick { _ ->
            if (type != "3") {
                if (list.none { it.isChecked }) return@onClick
            }

            val accountIds = ArrayList<String>()
            list.filter { it.isChecked }.forEach {
                if (type == "2") accountIds.add(it.friendId)
                else accountIds.add(it.accountInfoId)
            }

            when (type) {
                "1" -> {
                    OkGo.post<String>(BaseHttp.out_cluster)
                        .tag(this@NetworkHandleActivity)
                        .headers("token", getString("token"))
                        .params("clusterId", intent.getStringExtra("roomId"))
                        .params("accountInfoIds", accountIds.joinToString(","))
                        .execute(object : StringDialogCallback(baseContext) {

                            override fun onSuccessResponse(
                                response: Response<String>,
                                msg: String,
                                msgCode: String
                            ) {
                                toast(msg)
                                EventBus.getDefault().post(RefreshMessageEvent("踢出群组"))
                                ActivityStack.screenManager.popActivities(this@NetworkHandleActivity::class.java)
                            }

                        })
                }
                "2" -> {
                    OkGo.post<String>(BaseHttp.add_cluster)
                        .tag(this@NetworkHandleActivity)
                        .params("clusterId", intent.getStringExtra("roomId"))
                        .params("accountInfoIds", accountIds.joinToString(","))
                        .execute(object : StringDialogCallback(baseContext) {

                            override fun onSuccessResponse(
                                response: Response<String>,
                                msg: String,
                                msgCode: String
                            ) {
                                toast(msg)
                                EventBus.getDefault().post(RefreshMessageEvent("拉入群组"))
                                ActivityStack.screenManager.popActivities(this@NetworkHandleActivity::class.java)
                            }

                        })
                }
                "3" -> {
                    OkGo.post<String>(BaseHttp.set_priority)
                        .tag(this@NetworkHandleActivity)
                        .headers("token", getString("token"))
                        .params("clusterId", intent.getStringExtra("roomId"))
                        .params("accountInfoIds", accountIds.joinToString(","))
                        .execute(object : StringDialogCallback(baseContext) {

                            override fun onSuccessResponse(
                                response: Response<String>,
                                msg: String,
                                msgCode: String
                            ) {
                                toast(msg)
                                EventBus.getDefault().post(RefreshMessageEvent("优先权"))
                                ActivityStack.screenManager.popActivities(this@NetworkHandleActivity::class.java)
                            }

                        })
                }
                "4" -> {
                    EventBus.getDefault().post(
                        RefreshMessageEvent(
                            "指定群主",
                            intent.getStringExtra("position"),
                            accountIds[0])
                    )

                    ActivityStack.screenManager.popActivities(this@NetworkHandleActivity::class.java)
                }
            }
        }
    }

    override fun getData() {
        OkGo.post<BaseResponse<ArrayList<CommonData>>>(BaseHttp.friend_list)
            .tag(this@NetworkHandleActivity)
            .headers("token", getString("token"))
            .execute(object :
                JacksonDialogCallback<BaseResponse<ArrayList<CommonData>>>(baseContext) {

                override fun onSuccess(response: Response<BaseResponse<ArrayList<CommonData>>>) {

                    val items = ArrayList<CommonData>()
                    items.addItems(response.body().`object`)

                    list.clear()
                    list.addAll(items.filter {
                        listHave.none { inner ->
                            inner.accountInfoId == it.friendId
                        }
                    })

                    mAdapter.updateData(list)
                }

                override fun onFinish() {
                    super.onFinish()
                    swipe_refresh.isRefreshing = false

                    empty_view.apply { if (list.isEmpty()) visible() else gone() }
                }

            })
    }

    override fun finish() {
        super.finish()
        mDisposable.clear()
    }

}
