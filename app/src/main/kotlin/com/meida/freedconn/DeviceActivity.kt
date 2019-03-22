package com.meida.freedconn

import android.os.Bundle
import android.view.View
import com.meida.base.BaseActivity
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class DeviceActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)
        init_title(getString(R.string.device))
    }

    override fun doClick(v: View) {
        super.doClick(v)
        when (v.id) {
            R.id.device_remote -> startActivity<DeviceRemoteActivity>()
            R.id.device_bluetooth -> toast("暂不对外开放")
        }
    }

}
