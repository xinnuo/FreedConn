package com.meida.freedconn

import android.os.Bundle
import android.view.View
import com.meida.base.BaseActivity
import com.meida.base.invisible
import com.meida.base.visible
import com.meida.fragment.ContactFragment
import com.meida.fragment.OnFragmentListener
import com.meida.fragment.TalkFragment
import com.meida.model.RefreshMessageEvent
import com.meida.utils.BluetoothHelper.isBluetoothConnected
import kotlinx.android.synthetic.main.activity_network.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.sdk25.listeners.onClick

class NetworkActivity : BaseActivity(), OnFragmentListener {

    private var selectedPosition = 0
    private lateinit var mContact: ContactFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)
        setToolbarVisibility(false)
        init_title()

        EventBus.getDefault().register(this@NetworkActivity)
    }

    override fun init_title() {
        mContact = ContactFragment()
        network_disconnect.visibility = if (isBluetoothConnected()) View.INVISIBLE else View.VISIBLE

        supportFragmentManager.addOnBackStackChangedListener {
            when (supportFragmentManager.backStackEntryCount) {
                1 -> {
                    selectedPosition = 1
                    network_contact.setBackgroundResource(R.mipmap.icon15)
                    network_talk.setBackgroundResource(R.mipmap.icon14)
                }
                else -> {
                    selectedPosition = 0
                    network_contact.setBackgroundResource(R.mipmap.icon14)
                    network_talk.setBackgroundResource(R.mipmap.icon15)
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.network_container, mContact)
            .commit()

        network_contact.onClick {
            if (selectedPosition == 1) onBackPressed()
        }

        network_talk.onClick {
            if (selectedPosition == 0) {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.push_left_in,
                        R.anim.push_left_out,
                        R.anim.push_right_in,
                        R.anim.push_right_out
                    )
                    .add(R.id.network_container, TalkFragment())
                    .hide(mContact)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onViewClick(name: String) = onBackPressed()

    override fun finish() {
        super.finish()
        EventBus.getDefault().unregister(this@NetworkActivity)
    }

    @Subscribe
    fun onMessageEvent(event: RefreshMessageEvent) {
        when (event.type) {
            "蓝牙连接" -> network_disconnect.invisible()
            "蓝牙断开" -> network_disconnect.visible()
        }
    }

}
