package com.meida.freedconn

import android.os.Bundle
import com.meida.base.BaseActivity

class SettingPhoneActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_phone)
        init_title(getString(R.string.setting_phone))
    }
}
