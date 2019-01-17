package com.meida.freedconn

import android.annotation.SuppressLint
import android.os.Bundle
import com.meida.base.BaseActivity
import com.meida.utils.Tools
import kotlinx.android.synthetic.main.activity_setting_version.*

class SettingVersionActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_version)
        init_title(getString(R.string.setting_version_title))
    }

    @SuppressLint("SetTextI18n")
    override fun init_title() {
        super.init_title()
        version_now.text = "v${Tools.getVersion(baseContext)}"
    }
}
