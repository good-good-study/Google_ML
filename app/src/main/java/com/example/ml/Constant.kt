package com.example.ml

import java.io.File

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class Constant {
    public val KEY_PATH_CAPTURE_IMG = App.ctx!!.externalCacheDir.path + File.separator + "capture_img"
    public val KEY_PATH_CROP_IMG = App.ctx!!.externalCacheDir.path + File.separator + "crop_img"
    public val KEY_PATH_TAKE_PHOTO_IMG = App.ctx!!.externalCacheDir.path + File.separator + "take_photo_img"
}