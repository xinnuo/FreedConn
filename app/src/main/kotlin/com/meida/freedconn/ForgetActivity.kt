package com.meida.freedconn

import android.os.Bundle
import android.view.View
import com.lzg.extend.StringDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.meida.base.BaseActivity
import com.meida.share.BaseHttp
import com.meida.utils.ActivityStack
import com.meida.utils.isMobile
import com.meida.utils.trimToUpperCase
import kotlinx.android.synthetic.main.activity_forget.*
import org.jetbrains.anko.toast

class ForgetActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget)
        init_title(getString(R.string.forget))
    }

    override fun doClick(v: View) {
        super.doClick(v)
        when (v.id) {
            R.id.bt_submit -> {
                when {
                    et_tel.text.isEmpty() -> {
                        et_tel.requestFocus()
                        toast(getString(R.string.forget_noTel))
                        return
                    }
                    et_code.text.isEmpty() -> {
                        et_code.requestFocus()
                        toast(getString(R.string.forget_noCode))
                        return
                    }
                    et_pwd.text.isEmpty() -> {
                        et_pwd.requestFocus()
                        toast(getString(R.string.forget_noPwd))
                        return
                    }
                    !et_tel.text.isMobile() -> {
                        et_tel.requestFocus()
                        toast(getString(R.string.forget_wrongTel))
                        return
                    }
                    et_pwd.text.length < 6 -> {
                        et_pwd.requestFocus()
                        toast(getString(R.string.forget_shortPwd))
                        return
                    }
                }

                OkGo.post<String>(BaseHttp.update_pwd)
                    .tag(this@ForgetActivity)
                    .params("accountName", et_tel.text.toString())
                    .params("pollcode", et_code.text.trimToUpperCase())
                    .params("newpwd", et_pwd.text.toString())
                    .execute(object : StringDialogCallback(baseContext) {

                        override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {

                            toast(msg)
                            ActivityStack.screenManager.popActivities(this@ForgetActivity::class.java)
                        }

                    })
            }
        }
    }
}
