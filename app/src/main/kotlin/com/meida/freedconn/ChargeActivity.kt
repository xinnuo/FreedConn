package com.meida.freedconn

import android.os.Bundle
import android.view.View
import com.cuieney.rxpay_annotation.WX
import com.cuieney.sdk.rxpay.RxPay
import com.lzg.extend.StringDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.lzy.okgo.utils.OkLogger
import com.meida.base.*
import com.meida.model.RefreshMessageEvent
import com.meida.share.BaseHttp
import com.meida.utils.ActivityStack
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_charge.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.sdk25.listeners.onCheckedChange
import org.jetbrains.anko.toast
import org.json.JSONObject

@WX(packageName = "com.meida.freedconn")
class ChargeActivity : BaseActivity() {

    private var mPayTime = "27F2245648E34C4183AA95ACAC070990"
    private var mPayWay = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charge)
        init_title("通话时长充值")

        charge_check1.isChecked = true
    }

    override fun init_title() {
        super.init_title()
        charge_tel.text = getString("mobile")
        charge_img.setImageURL(
            BaseHttp.baseImg + getString("userHead"),
            R.mipmap.default_user
        )

        charge_group.onCheckedChange { _, checkedId ->
            when (checkedId) {
                R.id.charge_check1 -> mPayWay = "AliPay"
                R.id.charge_check2 -> mPayWay = "WxPay"
                R.id.charge_check3 -> mPayWay = ""
            }
        }
    }

    override fun doClick(v: View) {
        super.doClick(v)
        when (v.id) {
            R.id.charge_time1 -> {
                charge_img1.visible()
                charge_img2.invisible()
                charge_img3.invisible()
                mPayTime = "27F2245648E34C4183AA95ACAC070990"
            }
            R.id.charge_time2 -> {
                charge_img1.invisible()
                charge_img2.visible()
                charge_img3.invisible()
                mPayTime = "34FA5302D44D4335A107499CA9027007"
            }
            R.id.charge_time3 -> {
                charge_img1.invisible()
                charge_img2.invisible()
                charge_img3.visible()
                mPayTime = "3973DE274C2B4C72BB275DF0B67F9BDA"
            }
            R.id.bt_pay -> {
                if (mPayWay.isEmpty()) {
                    toast("暂不支持其他充值方式")
                    return
                }

                getPayData()
            }
        }
    }

    /* 支付宝、微信支付 */
    private fun getPayData() {
        OkGo.post<String>(BaseHttp.pay_sub)
            .tag(this@ChargeActivity)
            .headers("token", getString("token"))
            .params("goodsId", mPayTime)
            .params("payType", mPayWay)
            .execute(object : StringDialogCallback(baseContext) {

                override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {

                    val obj = JSONObject(response.body()).optString("object")
                    val data = JSONObject(response.body()).optString("object")
                    when (mPayWay) {
                        "AliPay" -> RxPay(baseContext)
                            .requestAlipay(obj)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                if (it) {
                                    EventBus.getDefault().post(RefreshMessageEvent("支付成功"))
                                    ActivityStack.screenManager.popActivities(this@ChargeActivity::class.java)
                                } else {
                                    toast("支付失败")
                                }
                            }) { OkLogger.printStackTrace(it) }
                        "WxPay" -> RxPay(baseContext)
                            .requestWXpay(data)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                if (it) {
                                    EventBus.getDefault().post(RefreshMessageEvent("支付成功"))
                                    ActivityStack.screenManager.popActivities(this@ChargeActivity::class.java)
                                } else {
                                    toast("支付失败")
                                }
                            }) { OkLogger.printStackTrace(it) }
                    }
                }

            })
    }

}
