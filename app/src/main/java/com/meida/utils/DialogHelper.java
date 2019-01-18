package com.meida.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.bigkoo.svprogresshud.SVProgressHUD;
import com.meida.base.BaseDialog;
import com.meida.freedconn.R;

public class DialogHelper {

    private static SVProgressHUD mSVProgressHUD;

    private DialogHelper() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static void showDialog(Context context) {
        showDialog(context, context.getString(R.string.loading));
    }

    public static void showDialog(Context context, String hint) {
        dismissDialog();

        mSVProgressHUD = new SVProgressHUD(context);
        mSVProgressHUD.showWithStatus(hint);
    }

    public static void dismissDialog() {
        if (mSVProgressHUD != null && mSVProgressHUD.isShowing())
            mSVProgressHUD.dismissImmediately();
    }

    public static void showBluetoothDialog(
            final Context context,
            final ClickCallBack callBack) {
        showBluetoothDialog(context, context.getString(R.string.bluetooth_hint), callBack);
    }

    public static void showBluetoothDialog(
            final Context context,
            final String hint,
            final ClickCallBack callBack) {
        BaseDialog dialog = new BaseDialog(context) {
            @Override
            public View onCreateView() {
                widthScale(0.9f);
                View view = View.inflate(context, R.layout.dialog_bluetooth_hint, null);

                TextView tvHint = view.findViewById(R.id.dialog_hint);
                TextView btCancel = view.findViewById(R.id.dialog_cancel);
                TextView btSure = view.findViewById(R.id.dialog_sure);

                tvHint.setText(hint);
                btCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                        callBack.onClick("no");
                    }
                });
                btSure.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                        callBack.onClick("yes");
                    }
                });

                return view;
            }
        };

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public static void showHintDialog(
            final Context context,
            final String title,
            final String hint,
            final String cancel,
            final String sure,
            final boolean isForced,
            final ClickCallBack callBack) {
        BaseDialog dialog = new BaseDialog(context) {
            @Override
            public View onCreateView() {
                widthScale(0.9f);
                View view = View.inflate(context, R.layout.dialog_bluetooth_hint, null);

                TextView tvTitle = view.findViewById(R.id.dialog_title);
                TextView tvHint = view.findViewById(R.id.dialog_hint);
                TextView btCancel = view.findViewById(R.id.dialog_cancel);
                TextView btSure = view.findViewById(R.id.dialog_sure);

                tvTitle.setText(title);
                tvHint.setText(hint);
                btCancel.setText(cancel);
                btSure.setText(sure);

                btCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                        callBack.onClick("no");
                    }
                });
                btSure.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                        callBack.onClick("yes");
                    }
                });

                return view;
            }
        };

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) return isForced;
                return false;
            }
        });
        dialog.setCanceledOnTouchOutside(!isForced);
        dialog.show();
    }

    public interface ClickCallBack {
        void onClick(String hint);
    }

}
