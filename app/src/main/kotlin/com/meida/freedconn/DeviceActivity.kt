package com.meida.freedconn

import android.os.Bundle
import com.meida.base.BaseActivity

class DeviceActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)
        init_title(getString(R.string.device))
    }
}
