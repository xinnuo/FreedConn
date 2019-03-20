package com.meida.freedconn

import android.os.Bundle
import com.meida.base.BaseActivity

class ChargeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charge)
        init_title("通话时长充值")
    }
}
