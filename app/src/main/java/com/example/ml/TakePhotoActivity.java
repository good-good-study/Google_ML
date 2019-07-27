package com.example.ml;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by sxt on 2018/6/8.
 */
public class TakePhotoActivity extends BaseActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private boolean taking;
    private boolean successTakePhoto;
    private Camera mCamera;
    private View takePhoto;
    private ImageView imgCenter;
    private File file;
    private long delayMillis = 1500L;
    private final int MSG_AUTOFUCS = 100;
    private Camera.AutoFocusCallback autoFocusCallback;
    private String TAG = "TakePhoto";
    private SurfaceHolder surfaceHolder;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.v(TAG, "" + msg.what);
            switch (msg.what) {
                case MSG_AUTOFUCS:
                    if (mCamera != null && !taking) {
                        try {
                            mCamera.autoFocus(autoFocusCallback);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        takePhoto = findViewById(R.id.take_photo);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        imgCenter = findViewById(R.id.img_center);
        takePhoto.setOnClickListener(this);
        findViewById(R.id.img_back).setOnClickListener(this);
        findViewById(R.id.img_confirm).setOnClickListener(this);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setKeepScreenOn(true);
        autoFocusCallback = (success, camera) -> {
            if (mHandler != null && !taking) {
                mHandler.sendEmptyMessageDelayed(MSG_AUTOFUCS, delayMillis);
            }
        };

        findViewById(R.id.back).setOnClickListener(this);

    }

    private void showDialog() {
        runOnUiThread(() -> loading.show());
    }

    public void dismiss() {
        runOnUiThread(() -> {
            if (loading != null && loading.isShowing()) {
                loading.dismiss();
            }
        });
    }

    // 暂停时执行的动作：把相机关闭，避免占用导致其他应用无法使用相机
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        mHandler.removeCallbacksAndMessages(null);
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    // 恢复apk时执行的动作
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        start();
    }

    private void start() {
        mHandler.sendEmptyMessage(MSG_AUTOFUCS);
        if (null == mCamera) {
            mCamera = getCameraInstance();
            surfaceHolder.addCallback(this);
            if (mCamera == null) return;
        }
        mCamera.startPreview();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (null == mCamera) {
            mCamera = getCameraInstance();
        }
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.autoFocus(autoFocusCallback);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error setting mCamera preview: " + e.getMessage());
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
        refreshCamera(); // 这一步是否多余？在以后复杂的使用场景下，此步骤是必须的。
        int rotation = getDisplayOrientation(); //获取当前窗口方向
        mCamera.setDisplayOrientation(rotation); //设定相机显示方向
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceHolder.removeCallback(this);
        mHandler.removeCallbacksAndMessages(null);
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
            if (c == null) {
                int cametacount = Camera.getNumberOfCameras();
                c = Camera.open(cametacount - 1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    // 获取当前窗口管理器显示方向
    private int getDisplayOrientation() {
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        Camera.CameraInfo camInfo =
                new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);

        // 这里其实还是不太懂：为什么要获取camInfo的方向呢？相当于相机标定？？
        int result = (camInfo.orientation - degrees + 360) % 360;

        return result;
    }

    // 刷新相机
    private void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
            Log.i(TAG, e.toString());
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
            mCamera.autoFocus(autoFocusCallback);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    private void takePhoto() {
        setCameraPictureSize();
        takePhoto.setEnabled(false);
        taking = true;
        mHandler.removeCallbacksAndMessages(null);
        if (mCamera != null) {
            showDialog();
            successTakePhoto = false;
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(final byte[] bytes, Camera camera) {
                    runOnUiThread(() -> mCamera.stopPreview());
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try {
                                if (bytes != null && bytes.length > 0) {
                                    File files = new File(new Constant().getKEY_PATH_CAPTURE_IMG());
                                    if (!files.exists()) {
                                        files.mkdirs();
                                    }
                                    file = new File(files.getPath() + File.separator + System.currentTimeMillis() + ".jpg");
//                                    FileOutputStream os = new FileOutputStream(file);
//                                    os.write(bytes);
//                                    os.flush();
//                                    os.popub_close();

                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    Matrix matrix = new Matrix(); // 旋转图片 动作
                                    matrix.setRotate(90, bitmap.getWidth() >> 1, bitmap.getHeight() >> 1);
                                    // 创建新的图片
                                    Bitmap fixedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                                    if (fixedBitmap != null) {
                                        FileOutputStream fos = new FileOutputStream(file, false);//将压缩后的图片保存的本地上指定路径中
                                        fixedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                        fos.flush();
                                        fos.close();

                                        File newFile = new File(file.getPath());
                                        if (newFile.exists() && newFile.length() > 2 * 1024 * 1024) {

                                            BitmapFactory.Options newOpts = new BitmapFactory.Options();
                                            newOpts.inJustDecodeBounds = true;//只读边,不读内容
                                            BitmapFactory.decodeFile(file.getPath(), newOpts);
                                            newOpts.inJustDecodeBounds = false;
                                            int width = newOpts.outWidth;
                                            int height = newOpts.outHeight;
                                            float maxSize = 520;
                                            int be = 1;
                                            if (width >= height && width > maxSize) {//缩放比,用高或者宽其中较大的一个数据进行计算
                                                be = (int) (newOpts.outWidth / maxSize);
                                                //be++;
                                            } else if (width < height && height > maxSize) {
                                                be = (int) (newOpts.outHeight / maxSize);
                                                //be++;
                                            }
                                            newOpts.inSampleSize = be;//设置采样率
                                            newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;//该模式是默认的,可不设
                                            newOpts.inPurgeable = true;// 同时设置才会有效
                                            newOpts.inInputShareable = true;//。当系统内存不够时候图片自动被回收
                                            Bitmap cropped = BitmapFactory.decodeFile(file.getPath(), newOpts);
                                            FileOutputStream os = new FileOutputStream(file.getPath());//将压缩后的图片保存的本地上指定路径中
                                            cropped.compress(Bitmap.CompressFormat.JPEG, 100, os);

                                            os.flush();
                                            os.close();
                                            cropped.recycle();
                                            Log.i(TAG, "图片 压缩比 : " + be);
                                        }

                                        Log.i(TAG, file.exists() ? "图片保存成功" : "图片保存失败");
                                        Log.i(TAG, "相机返回的bytes.length : " + bytes.length);
                                        File newFile0 = new File(file.getPath());
                                        if (newFile.exists()) {
                                            Log.i(TAG, "图片保存成功后的 newFile.length = " + (newFile0.length() >= 1024f * 1024f ? newFile0.length() / 1024f / 1024f + "M" : (newFile0.length() / 1024f) + "K"));
                                        }
                                    }
                                    if (!bitmap.isRecycled()) {
                                        bitmap.recycle();
                                    }
                                    if (fixedBitmap != null && !fixedBitmap.isRecycled()) {
                                        fixedBitmap.recycle();
                                    }

                                    successTakePhoto = true;
                                    runOnUiThread(() -> {
                                        findViewById(R.id.result_layout).setVisibility(View.VISIBLE);
                                        imgCenter.setImageURI(Uri.fromFile(file));
                                    });
                                }
                            } catch (Exception e) {
                                file = null;
                                taking = false;
                                successTakePhoto = false;
                                e.printStackTrace();
                                Log.i(TAG, e.toString());
                                runOnUiThread(() -> takePhoto.setEnabled(true));
                            }
                            taking = false;
                            dismiss();
                        }
                    }.start();
                }
            });
        }
    }

    private void setCameraPictureSize() {
        try {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            Camera.Parameters mParameters = mCamera.getParameters();
            Camera.Size size = null;
            Log.i(TAG, "screenWidth ==  " + screenWidth);
            for (int i = 0; i < mParameters.getSupportedPictureSizes().size(); i++) {
                if (Math.abs(screenWidth - mParameters.getSupportedPictureSizes().get(i).width) == 0) {
                    size = mParameters.getSupportedPictureSizes().get(i);
                    break;
                }
                Log.i(TAG, "width  ==  " + mParameters.getSupportedPictureSizes().get(i).width + "  height  ==  " + mParameters.getSupportedPictureSizes().get(i).height);
            }
            if (size == null) {
                size = mParameters.getSupportedPictureSizes().get(mParameters.getSupportedPictureSizes().size() / 2);
            }
            mParameters.setPictureSize(size.width, size.height);
            mCamera.setParameters(mParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取图片的旋转角度
     */
    private int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);

            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    degree = 0;
                    break;
            }
////            Matrix matrix = new Matrix();
////            matrix.setRotate(degree, );
//            //将图片的旋转角度置为0
//            //修正图片的旋转角度，设置其不旋转。这里也可以设置其旋转的角度，可以传值过去，
//            //例如旋转90度，传值ExifInterface.ORIENTATION_ROTATE_90，需要将这个值转换为String类型的
//            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "90");
//            exifInterface.saveAttributes();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    private Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        // 旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.setRotate(angle, bitmap.getWidth() >> 1, bitmap.getHeight() >> 1);
        // 创建新的图片
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.take_photo:
                takePhoto();
                break;
            case R.id.img_back:
                findViewById(R.id.result_layout).setVisibility(View.GONE);
                takePhoto.setEnabled(true);
                start();
                break;
            case R.id.img_confirm:
                if (successTakePhoto && file != null && file.exists()) {
                    Intent intent = new Intent();
                    intent.putExtra("CROP_IMG_URI", Uri.fromFile(file));
                    setResult(RESULT_OK, intent);
                    finish();
                    return;
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) initWindowStyle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }
}
