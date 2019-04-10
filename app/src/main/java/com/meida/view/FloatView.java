package com.meida.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;

import com.meida.freedconn.R;

/**
 * 全系统悬浮按钮
 */
@SuppressLint("AppCompatCustomView")
public class FloatView extends ImageView {

    private float mTouchX;
    private float mTouchY;
    private float x;
    private float y;
    private int startX;
    private int startY;
    private int imgId = R.mipmap.ic_launcher;
    private int controlledSpace = 20;
    private int screenWidth;
    private int screenHeight;
    boolean isShow = false;
    private OnClickListener mClickListener;

    private WindowManager windowManager;

    private WindowManager.LayoutParams windowManagerParams = new WindowManager.LayoutParams();

    public FloatView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatView(Context context) {
        super(context);
        initView(context);
    }

    // 初始化窗体
    public void initView(Context context) {
        windowManager = (WindowManager) context.getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        this.setImageResource(imgId);

        if(Build.VERSION.SDK_INT==25){
            windowManagerParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }else {
            if (Build.VERSION.SDK_INT > 25) {
                windowManagerParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                windowManagerParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            }
        }
        windowManagerParams.format = PixelFormat.RGBA_8888; // 背景透明
        windowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        // 调整悬浮窗口至左上角，便于调整坐标
        windowManagerParams.gravity = Gravity.START | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值
        windowManagerParams.x = 0;
        windowManagerParams.y = screenHeight >> 1;
        // 设置悬浮窗口长宽数据
        windowManagerParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowManagerParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
    }

    public void setImgResource(int id) {
        imgId = id;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        x = event.getRawX();
        y = event.getRawY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mTouchX = event.getX();
                mTouchY = event.getY();
                startX = (int) event.getRawX();
                startY = (int) event.getRawY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (Math.abs(x - startX) > controlledSpace
                        || Math.abs(y - startY) > controlledSpace) updateViewPosition();
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (Math.abs(x - startX) < controlledSpace
                        && Math.abs(y - startY) < controlledSpace) {
                    if (mClickListener != null) mClickListener.onClick(this);
                } else {
                    if (x <= screenWidth / 2) x = 0;
                    else x = screenWidth;

                    updateViewPosition();
                }
                break;
            }
        }

        return super.onTouchEvent(event);
    }

    // 隐藏该窗体
    public void hide() {
        if (isShow) {
            windowManager.removeView(this);
            isShow = false;
        }
    }

    // 显示该窗体
    public void show() {
        if (!isShow) {
            if (Build.VERSION.SDK_INT >= 23
                    && !Settings.canDrawOverlays(getContext())) {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:com.meida.freedconn")
                );
                getContext().startActivity(intent);
            } else {
                windowManager.addView(this, windowManagerParams);
                isShow = true;
            }
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        this.mClickListener = l;
    }

    private void updateViewPosition() {
        // 更新浮动窗口位置参数
        windowManagerParams.x = (int) (x - mTouchX);
        windowManagerParams.y = (int) (y - mTouchY);
        windowManager.updateViewLayout(this, windowManagerParams); // 刷新显示
    }

}
