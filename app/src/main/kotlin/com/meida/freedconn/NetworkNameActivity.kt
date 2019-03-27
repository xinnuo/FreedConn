package com.meida.freedconn

import android.os.Bundle
import com.lzg.extend.StringDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.meida.base.BaseActivity
import com.meida.base.getString
import com.meida.base.oneClick
import com.meida.model.RefreshMessageEvent
import com.meida.share.BaseHttp
import com.meida.utils.ActivityStack
import com.meida.utils.trimString
import kotlinx.android.synthetic.main.activity_network_name.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class NetworkNameActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_name)
        init_title(getString(R.string.network_chat_name_modify))
    }

    override fun init_title() {
        super.init_title()
        network_name.setText(intent.getStringExtra("name"))
        network_name.setSelection(network_name.text.length)

        bt_save.oneClick {
            if (network_name.text.isBlank()) {
                network_name.requestFocus()
                toast(getString(R.string.network_noName))
                return@oneClick
            }

            OkGo.post<String>(BaseHttp.update_name_cluster)
                .tag(this@NetworkNameActivity)
                .isMultipart(true)
                .headers("token", getString("token"))
                .params("clusterId", intent.getStringExtra("clusterId"))
                .params("clusterName", network_name.text.trimString())
                .execute(object : StringDialogCallback(baseContext) {

                    override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {
                        toast(msg)
                        EventBus.getDefault().post(RefreshMessageEvent(
                            "修改群名",
                            intent.getStringExtra("clusterId"),
                            network_name.text.trimString()
                        ))
                        ActivityStack.screenManager.popActivities(this@NetworkNameActivity::class.java)
                    }

                })
        }
    }

    override fun finish() {
        super.finish()
        startActivity<NetworkChatActivity>()
    }

}
