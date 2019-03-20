package com.meida.freedconn

import android.annotation.SuppressLint
import android.os.Bundle
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.meida.base.BaseActivity
import com.meida.base.oneClick
import com.meida.utils.Tools
import kotlinx.android.synthetic.main.activity_setting_version.*
import org.jetbrains.anko.toast
import org.json.JSONObject

class SettingVersionActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_version)
        init_title(getString(R.string.setting_version_title))

        getData()
    }

    @SuppressLint("SetTextI18n")
    override fun init_title() {
        super.init_title()
        version_now.text = "v${Tools.getVersion(baseContext)}"

        version_update.oneClick {
            toast("当前已是最新版本")
        }
        version_cancel.oneClick { onBackPressed() }
    }

    override fun getData() {
        OkGo.post<String>("https://www.pgyer.com/apiv2/app/view")
            .tag(this@SettingVersionActivity)
            .params("_api_key", "bdcb07efb995304f749c50ade9a8f7ad")
            .params("appKey", "f651e756420e1b175bd93f17381f5a3a")
            .execute(object : StringCallback() {

                @SuppressLint("SetTextI18n")
                override fun onSuccess(response: Response<String>) {
                    val obj = JSONObject(response.body()).optJSONObject("data")
                    val buildVersion = obj.optString("buildVersion")
                    version_now.text = "v$buildVersion"
                    version_new.text = "v$buildVersion"
                }

            })
    }
}
