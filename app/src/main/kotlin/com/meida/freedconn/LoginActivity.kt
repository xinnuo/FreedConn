package com.meida.freedconn

import android.os.Bundle
import android.view.View
import com.lzg.extend.StringDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.meida.base.BaseActivity
import com.netease.nim.avchatkit.AVChatKit
import com.netease.nimlib.sdk.auth.AuthService
import com.meida.base.*
import com.meida.chatkit.getService
import com.meida.chatkit.login
import com.meida.share.BaseHttp
import com.meida.utils.ActivityStack
import com.meida.utils.isMobile
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import org.json.JSONObject

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        init_title(getString(R.string.login))
    }

    override fun init_title() {
        super.init_title()
        ivBack.gone()

        if (getString("mobile").isNotEmpty()) {
            et_tel.setText(getString("mobile"))
            et_tel.setSelection(et_tel.text.length)
        }

        if (intent.getBooleanExtra("offLine", false)) {
            clearData()
            ActivityStack.screenManager.popAllActivityExcept(this@LoginActivity::class.java)
        }
    }

    override fun doClick(v: View) {
        super.doClick(v)
        when (v.id) {
            R.id.tv_forget -> startActivity<ForgetActivity>()
            R.id.tv_register -> startActivity<RegisterActivity>()
            R.id.bt_login -> {
                when {
                    et_tel.text.isEmpty() -> {
                        et_tel.requestFocus()
                        toast(getString(R.string.login_noTel))
                        return
                    }
                    et_pwd.text.isEmpty() -> {
                        et_pwd.requestFocus()
                        toast(getString(R.string.login_noPwd))
                        return
                    }
                    !et_tel.text.isMobile() -> {
                        et_tel.requestFocus()
                        toast(getString(R.string.login_wrongTel))
                        return
                    }
                    et_pwd.text.length < 6 -> {
                        et_pwd.requestFocus()
                        toast(getString(R.string.login_shortPwd))
                        return
                    }
                }

                OkGo.post<String>(BaseHttp.account_login)
                    .tag(this@LoginActivity)
                    .params("accountName", et_tel.text.toString())
                    .params("password", et_pwd.text.toString())
                    .params("accountType", "Vip")
                    .execute(object : StringDialogCallback(baseContext) {

                        override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {

                            val obj = JSONObject(response.body())
                                .optJSONObject("object") ?: JSONObject()

                            val account = obj.optString("accid")
                            val token = obj.optString("token")

                            login(account, token) {
                                onSuccess {
                                    putBoolean("isLogin", true)
                                    putString("mobile", it.account)
                                    putString("accid", it.account)
                                    putString("token", it.token)
                                    putString("email", obj.optString("email"))
                                    putString("userName", obj.optString("userName"))
                                    putString("userHead", obj.optString("userHead"))
                                    putString("pollcode", obj.optString("pollCode"))
                                    putString("residueTime", obj.optString("residueTime"))
                                    AVChatKit.setAccount(it.account)

                                    startActivity<MainActivity>()
                                    ActivityStack.screenManager.popActivities(this@LoginActivity::class.java)
                                }
                                onFailed {
                                    when (it) {
                                        302, 404 -> toast(getString(R.string.login_wrongAccoutOrPwd))
                                        else -> toast(getString(R.string.login_failed))
                                    }
                                }
                            }
                        }

                    })

            }
        }
    }

    private fun clearData() {
        putBoolean("isLogin", false)
        putString("accid", "")
        putString("token", "")
        putString("email", "")
        putString("userName", "")
        putString("userHead", "")
        putString("pollcode", "")
        putString("residueTime", "")

        getService<AuthService>().logout()
    }
}
