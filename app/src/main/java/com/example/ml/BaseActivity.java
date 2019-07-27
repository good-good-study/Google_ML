package com.example.ml;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/**
 * Created by izhaohu on 2018/1/9.
 */

public class BaseActivity extends AppCompatActivity {

    protected LoadingDialog loading;
    protected String TAG = this.getClass().getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loading = new LoadingDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
//            initWindowStyle();
        }
    }

    protected void initWindowStyle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE//防止系统栏隐藏时内容区域大小发生变化
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION//隐藏导航栏
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN//全屏
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION//隐藏底部的 三个 虚拟按键导航栏
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public boolean checkPermission(int requestCode, String permssion, String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, permssion) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, requestCode);
                return false;
            }
            return true;
        }
        return true;
    }

    public void goToAppSettingsPage() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private boolean hasAllPermissionsGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (hasAllPermissionsGranted(grantResults)) {
            onPermissionsAllowed(requestCode, permissions, grantResults);
        } else {
            if (!shouldShowRequestPermissionRationale(permissions[0])) {
                onPermissionsRefusedNever(requestCode, permissions, grantResults);
            } else {
                onPermissionsRefused(requestCode, permissions, grantResults);
            }
        }
    }

    public void onPermissionsAllowed(int requestCode, String[] permissions, int[] grantResults) {
        Log.e(TAG, "onPermissionsAllowed --> requestCode:" + requestCode);
    }

    public void onPermissionsRefused(int requestCode, String[] permissions, int[] grantResults) {
        Log.e(TAG, "onPermissionsRefused --> requestCode:" + requestCode);
    }

    public void onPermissionsRefusedNever(int requestCode, String[] permissions, int[] grantResults) {
        Log.e(TAG, "onPermissionsRefusedNever --> requestCode:" + requestCode);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loading.dismiss();
    }
}
