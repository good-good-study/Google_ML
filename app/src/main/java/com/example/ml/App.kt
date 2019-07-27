package com.example.ml


import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp

/**
 * Created by Administrator on 2017/3/20.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        ctx = this
        FirebaseApp.initializeApp(this)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var ctx: Context? = null
            private set
    }
}
