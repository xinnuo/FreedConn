package com.meida.freedconn

import android.os.Bundle
import android.text.InputFilter
import android.view.View
import com.lzg.extend.StringDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.meida.base.BaseActivity
import com.meida.share.BaseHttp
import com.meida.utils.*
import kotlinx.android.synthetic.main.activity_register.*
import org.jetbrains.anko.sdk25.listeners.textChangedListener
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import java.util.regex.Pattern

class RegisterActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        init_title(getString(R.string.register))
    }

    override fun init_title() {
        super.init_title()

        et_name.filters = arrayOf<InputFilter>(NameLengthFilter(16))
        et_name.textChangedListener {
            afterTextChanged { s ->
                pageNum = 0
                (0 until s!!.trim().length).forEach {
                    val matcher = Pattern.compile("[\u4e00-\u9fa5]").matcher(s[it].toString())
                    if (matcher.matches()) pageNum += 2
                    else pageNum++
                }
            }
        }
    }

    override fun doClick(v: View) {
        super.doClick(v)
        when (v.id) {
            R.id.tv_deal -> startActivity<WebActivity>()
            R.id.bt_register -> {
                when {
                    et_tel.text.isEmpty() -> {
                        et_tel.requestFocus()
                        toast(getString(R.string.register_noTel))
                        return
                    }
                    et_name.text.isBlank() -> {
                        et_name.requestFocus()
                        toast(getString(R.string.register_noName))
                        return
                    }
                    et_email.text.isEmpty() -> {
                        et_email.requestFocus()
                        toast(getString(R.string.register_noEmail))
                        return
                    }
                    et_pwd.text.isEmpty() -> {
                        et_pwd.requestFocus()
                        toast(getString(R.string.register_noPwd))
                        return
                    }
                    et_confirm.text.isEmpty() -> {
                        et_confirm.requestFocus()
                        toast(getString(R.string.register_noConfirm))
                        return
                    }
                    !et_tel.text.isMobile() -> {
                        et_tel.requestFocus()
                        toast(getString(R.string.register_wrongTel))
                        return
                    }
                    !et_email.text.isEmail() -> {
                        et_email.requestFocus()
                        toast(getString(R.string.register_wrongEmail))
                        return
                    }
                    pageNum < 4 -> {
                        et_name.requestFocus()
                        toast(getString(R.string.register_shortName))
                        return
                    }
                    et_pwd.text.length < 6 -> {
                        et_pwd.requestFocus()
                        toast(getString(R.string.register_shortPwd))
                        return
                    }
                    et_confirm.text.length < 6 -> {
                        et_confirm.requestFocus()
                        toast(getString(R.string.register_shortPwd))
                        return
                    }
                    et_pwd.text.toString() != et_confirm.text.toString() -> {
                        toast(getString(R.string.register_unequalPwd))
                        return
                    }
                }

                OkGo.post<String>(BaseHttp.account_reg)
                    .tag(this@RegisterActivity)
                    .isMultipart(true)
                    .apply {
                        params("mobile", et_tel.text.toString())
                        params("userName", et_name.text.trimString())
                        params("email", et_email.text.toString())
                        params("password", et_confirm.text.toString())
                        if (et_code.text.isNotEmpty())
                            params("pollcode", et_code.text.trimToUpperCase())
                    }.execute(object : StringDialogCallback(baseContext) {

                        override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {

                            toast(getString(R.string.register_done))
                            ActivityStack.screenManager.popActivities(this@RegisterActivity::class.java)
                        }

                    })
            }
        }
    }
}
