package com.shiraselab.overlayviewsample

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private var isServiceBound = false
    private lateinit var service: OverlayService

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            unbindMyService()
        }

        override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
            moveTaskToBack(true)
            val binder = binder as OverlayService.OverlayServiceBinder
            service = binder.getService()
            isServiceBound = true
            unbindMyService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (intent.getBooleanExtra("EXIT", false)) {
            finishAffinity() // アクティビティスタックを全て終了
            exitProcess(0)  // 完全終了
        }

        val overlayButton: Button = findViewById(R.id.overlayButton)

        overlayButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                // オーバーレイが許可されている場合、サービスを開始
                moveTaskToBack(true)
                bindService()
            } else {
                // 許可されていない場合は設定画面に遷移
                requestOverlayPermission()
            }
        }
        if (Settings.canDrawOverlays(this)) {
            startOverlayService()
            bindService()
            moveTaskToBack(true)
            unbindMyService()
        } else {
            // 許可されていない場合は設定画面に遷移
            requestOverlayPermission()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("EXIT", false)) {
            finishAffinity() // アクティビティスタックを全て終了
            exitProcess(0)  // 完全終了
        }

    }

    private fun bindService() {
        // We can also write like this using "also".
        Intent(this, OverlayService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)

        ContextCompat.startForegroundService(this, intent)
    }

    private fun unbindMyService() {
        if (isServiceBound) {
            unbindService(connection)
            isServiceBound = false
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                // 許可されたらサービスを開始
                startOverlayService()
                bindService()
            } else {
                Toast.makeText(this, "オーバーレイの許可が必要です", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 1
    }
}