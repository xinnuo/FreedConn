package com.meida.freedconn

import android.os.Bundle
import android.view.View
import com.lzg.extend.StringDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.meida.base.BaseActivity
import com.meida.base.getString
import com.meida.share.BaseHttp
import com.meida.utils.ActivityStack
import kotlinx.android.synthetic.main.activity_setting_password.*
import org.jetbrains.anko.toast

class SettingPasswordActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_password)
        init_title(getString(R.string.setting_pwd_title))
    }

    override fun doClick(v: View) {
        super.doClick(v)
        when (v.id) {
            R.id.bt_save -> {
                when {
                    et_now.text.isEmpty() -> {
                        et_now.requestFocus()
                        toast(getString(R.string.setting_pwd_noNow))
                        return
                    }
                    et_new.text.isEmpty() -> {
                        et_new.requestFocus()
                        toast(getString(R.string.setting_pwd_noNew))
                        return
                    }
                    et_confirm.text.isEmpty() -> {
                        et_confirm.requestFocus()
                        toast(getString(R.string.setting_pwd_noConfirm))
                        return
                    }
                    et_now.text.length < 6 -> {
                        et_now.requestFocus()
                        toast(getString(R.string.setting_pwd_shortPwd))
                        return
                    }
                    et_new.text.length < 6 -> {
                        et_new.requestFocus()
                        toast(getString(R.string.setting_pwd_shortPwd))
                        return
                    }
                    et_confirm.text.length < 6 -> {
                        et_confirm.requestFocus()
                        toast(getString(R.string.setting_pwd_shortPwd))
                        return
                    }
                    et_new.text.toString() != et_confirm.text.toString() -> {
                        toast(getString(R.string.setting_pwd_unequalPwd))
                        return
                    }
                }

                OkGo.post<String>(BaseHttp.update_pwd2)
                    .tag(this@SettingPasswordActivity)
                    .headers("token", getString("token"))
                    .params("oldpwd", et_now.text.toString())
                    .params("newpwd", et_confirm.text.toString())
                    .execute(object : StringDialogCallback(baseContext) {

                        override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {

                            toast(msg)
                            ActivityStack.screenManager.popActivities(this@SettingPasswordActivity::class.java)
                        }

                    })
            }
        }
    }
}
