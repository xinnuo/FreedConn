package com.meida.freedconn

import android.os.Bundle
import com.meida.base.BaseActivity

class SettingVersionActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_version)
        init_title(getString(R.string.setting_version_title))
    }
}
