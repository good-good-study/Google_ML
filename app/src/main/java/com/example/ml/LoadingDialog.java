package com.example.ml;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;


public class LoadingDialog {
    private Activity mActivity = null;
    private LayoutInflater mInflater;
    private Dialog mDialog;
    private View rootView;
    private ProgressDrawable loading;

    public LoadingDialog(Activity activity) {
        mActivity = activity;
        mInflater = LayoutInflater.from(activity);
        init();
    }

    private void init() {
        rootView = mInflater.inflate(R.layout.item_loading, null);
        mDialog = new Dialog(mActivity, R.style.AlertDialogStyle);
        ImageView img = (ImageView) rootView.findViewById(R.id.loading);
        loading = new ProgressDrawable();
        img.setImageDrawable(loading);
        mDialog.setCancelable(true);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                loading.stop();
            }
        });
    }

    public LoadingDialog setCancelable(boolean cancelable) {
        if (mDialog != null) {
            mDialog.setCancelable(cancelable);
        }
        return this;
    }

    @SuppressWarnings("deprecation")
    public void show() {
        try {
            mDialog.setContentView(rootView);
            mDialog.show();
            loading.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            loading.stop();
        }
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }
}
