package com.meida.freedconn

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.lzg.extend.StringDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.meida.base.BaseActivity
import com.meida.base.*
import com.meida.model.RefreshMessageEvent
import com.meida.share.BaseHttp
import com.meida.share.Const
import kotlinx.android.synthetic.main.activity_setting.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import org.json.JSONObject
import java.io.File

class SettingActivity : BaseActivity() {

    private var selectList = ArrayList<LocalMedia>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        init_title(getString(R.string.setting))

        EventBus.getDefault().register(this@SettingActivity)

        getData()
    }

    override fun init_title() {
        super.init_title()
        loadUserHead(getString("userHead"))
        setting_name.text = getString("userName")
        setting_tel.text = getString("mobile")
        setting_code.text = getString("pollcode")
        setting_time.text = "0"
    }

    private fun loadUserHead(path: String) =
        setting_img.setImageURL(BaseHttp.baseImg + path, R.mipmap.default_user)

    override fun doClick(v: View) {
        super.doClick(v)
        when (v.id) {
            R.id.setting_tel_ll -> startActivity<SettingPhoneActivity>()
            R.id.setting_pwd -> startActivity<SettingPasswordActivity>()
            R.id.setting_version -> startActivity<SettingVersionActivity>()
            R.id.setting_charge -> startActivity<ChargeActivity>()
            R.id.setting_quit -> startActivity<LoginActivity>("offLine" to true)
            R.id.setting_img -> {
                PictureSelector.create(this@SettingActivity)
                    // 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()
                    .openGallery(PictureMimeType.ofImage())
                    // 主题样式(不设置则为默认样式)
                    .theme(R.style.picture_customer_style)
                    // 最大图片选择数量 int
                    .maxSelectNum(1)
                    // 最小选择数量 int
                    .minSelectNum(1)
                    // 每行显示个数 int
                    .imageSpanCount(4)
                    // 多选 or 单选 PictureConfig.MULTIPLE or PictureConfig.SINGLE
                    .selectionMode(PictureConfig.MULTIPLE)
                    // 是否可预览图片 true or false
                    .previewImage(true)
                    // 是否可预览视频 true or false
                    .previewVideo(false)
                    // 是否可播放音频 true or false
                    .enablePreviewAudio(false)
                    // 是否显示拍照按钮 true or false
                    .isCamera(true)
                    // 拍照保存图片格式后缀，默认jpeg
                    .imageFormat(PictureMimeType.PNG)
                    // 图片列表点击 缩放效果 默认true
                    .isZoomAnim(true)
                    // glide 加载图片大小 0~1之间 如设置 .glideOverride()无效
                    // .sizeMultiplier(0.5f)
                    // 自定义拍照保存路径,可不填
                    .setOutputCameraPath(Const.SAVE_FILE)
                    // 是否压缩 true or false
                    .compress(true)
                    // int glide 加载宽高，越小图片列表越流畅，但会影响列表图片浏览的清晰度
                    .glideOverride(160, 160)
                    // 是否裁剪 true or false
                    .enableCrop(true)
                    // int 裁剪比例 如16:9 3:2 3:4 1:1 可自定义
                    .withAspectRatio(1, 1)
                    // 是否显示uCrop工具栏，默认不显示 true or false
                    .hideBottomControls(true)
                    // 压缩图片保存地址
                    .compressSavePath(cacheDir.absolutePath)
                    // 裁剪框是否可拖拽 true or false
                    .freeStyleCropEnabled(false)
                    // 是否圆形裁剪 true or false
                    .circleDimmedLayer(false)
                    // 是否显示裁剪矩形边框 圆形裁剪时建议设为false
                    .showCropFrame(true)
                    // 是否显示裁剪矩形网格 圆形裁剪时建议设为false
                    .showCropGrid(true)
                    // 是否显示gif图片 true or false
                    .isGif(false)
                    // 是否开启点击声音 true or false
                    .openClickSound(false)
                    // 是否传入已选图片 List<LocalMedia> list
                    .selectionMedia(selectList.apply { clear() })
                    // 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中)
                    .previewEggs(true)
                    // 小于100kb的图片不压缩
                    .minimumCompressSize(100)
                    // 是否可拖动裁剪框(固定)
                    .isDragFrame(false)
                    // 结果回调onActivityResult code
                    .forResult(PictureConfig.CHOOSE_REQUEST)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PictureConfig.CHOOSE_REQUEST -> {
                    // 图片选择结果回调
                    selectList = PictureSelector.obtainMultipleResult(data) as ArrayList<LocalMedia>
                    // LocalMedia 里面返回三种path
                    // 1.media.getPath(); 为原图path
                    // 2.media.getCutPath();为裁剪后path，需判断media.isCut();是否为true
                    // 3.media.getCompressPath();为压缩后path，需判断media.isCompressed();是否为true
                    // 如果裁剪并压缩了，已取压缩路径为准，因为是先裁剪后压缩的

                    if (selectList[0].isCompressed) getHeadData()
                }
            }
        }
    }

    override fun getData() {
        OkGo.post<String>(BaseHttp.system_set)
            .tag(this@SettingActivity)
            .headers("token", getString("token"))
            .execute(object : StringDialogCallback(baseContext) {

                override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {

                    val obj = JSONObject(response.body()).optJSONObject("object")
                    val residueTime = obj.optString("residueTime")
                    setting_time.text = residueTime
                    putString("residueTime", residueTime)
                }

            })
    }

    private fun getHeadData() {
        OkGo.post<String>(BaseHttp.userhead_edit)
            .tag(this@SettingActivity)
            .headers("token", getString("token"))
            .params("img", File(selectList[0].compressPath))
            .execute(object : StringDialogCallback(baseContext) {

                override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {

                    toast(msg)
                    val userhead = JSONObject(response.body()).optString("object")
                    putString("userHead", userhead)
                    loadUserHead(userhead)
                }

            })
    }

    override fun finish() {
        super.finish()
        EventBus.getDefault().unregister(this@SettingActivity)
    }

    @Subscribe
    fun onMessageEvent(event: RefreshMessageEvent) {
        when (event.type) {
            "支付成功" -> getData()
        }
    }

}
