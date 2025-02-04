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
import com.shiraselab.overlayviewsample.OverlayService.Companion.TAG
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager
    private var isServiceBound = false
    private lateinit var service: OverlayService
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            unbindMyService()
        }

        override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
            val binder = binder as OverlayService.MyBinder
            service = binder.getService()
            isServiceBound = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val overlayButton: Button = findViewById(R.id.overlayButton)

        overlayButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                // オーバーレイが許可されている場合、サービスを開始
                moveTaskToBack(true)
                bindMyService()
            } else {
                // 許可されていない場合は設定画面に遷移
                requestOverlayPermission()
            }
        }
        startOverlayService()

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)  // バッテリーが十分な場合
            .setRequiresCharging(false)      // 充電していなくても実行
            .build()
        val workRequest = PeriodicWorkRequest.Builder(ServiceWorker::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)

        val workManager = WorkManager.getInstance(applicationContext)
        val workInfo = workManager.getWorkInfoByIdLiveData(workRequest.id)
        workInfo.observe(this, Observer { info ->
            if (info != null) {
                if (info.state == WorkInfo.State.CANCELLED) {
                    Log.d(TAG, "Work is cancelled, retrying if needed.")
                    // 必要に応じて再スケジュール
                }
            }
        })
    }

    private fun bindMyService() {
        // We can also write like this using "also".
        Intent(this, OverlayService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }
    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)

        ContextCompat.startForegroundService(this, intent)
    }

    private fun callMyServiceMethod() {
        if (isServiceBound) {
            service.awesomeMethod("Hello MyService!")
        }
    }

    private fun unbindMyService() {
        if (isServiceBound) {
            unbindService(connection)
            isServiceBound = false
        }
    }

    private fun stopMyService() {
        unbindMyService()
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
    }
    // 設定画面を開いてオーバーレイ許可をリクエスト
    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
    }

    // 許可をリクエストした後の結果を受け取る
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                // 許可されていればサービスを開始
            } else {
                Toast.makeText(this, "オーバーレイの許可が必要です", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 1
    }
}