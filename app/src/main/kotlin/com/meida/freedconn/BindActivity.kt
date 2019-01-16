package com.meida.freedconn

import android.os.Bundle
import android.view.View
import com.lzg.extend.StringDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.meida.base.BaseActivity
import com.meida.base.getString
import com.meida.base.putString
import com.meida.share.BaseHttp
import com.meida.utils.ActivityStack
import com.meida.utils.isMobile
import com.meida.utils.trimToUpperCase
import kotlinx.android.synthetic.main.activity_bind.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class BindActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bind)
        init_title(getString(R.string.bind))
    }

    override fun init_title() {
        super.init_title()
        et_tel.setText(getString("mobile"))
        et_tel.setSelection(et_tel.text.length)
    }

    override fun doClick(v: View) {
        super.doClick(v)
        when (v.id) {
            R.id.bt_save -> {
                when {
                    et_tel.text.isEmpty() -> {
                        et_tel.requestFocus()
                        toast(getString(R.string.bind_noTel))
                        return
                    }
                    et_code.text.isEmpty() -> {
                        et_tel.requestFocus()
                        toast(getString(R.string.bind_noCode))
                        return
                    }
                    !et_tel.text.isMobile() -> {
                        et_tel.requestFocus()
                        toast(getString(R.string.bind_wrongTel))
                        return
                    }
                }

                OkGo.post<String>(BaseHttp.pollcode_bind)
                    .tag(this@BindActivity)
                    .headers("token", getString("token"))
                    .params("accountName", getString("mobile"))
                    .params("pollcode", et_code.text.trimToUpperCase())
                    .execute(object : StringDialogCallback(baseContext) {

                        override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {

                            toast(msg)
                            putString("pollcode", et_code.text.trimToUpperCase())
                            startActivity<NetworkActivity>()
                            ActivityStack.screenManager.popActivities(this@BindActivity::class.java)
                        }

                    })

            }
        }
    }
}
