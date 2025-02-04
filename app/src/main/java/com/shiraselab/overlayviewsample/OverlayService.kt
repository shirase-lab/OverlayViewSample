package com.shiraselab.overlayviewsample

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import java.util.concurrent.TimeUnit

class OverlayService : Service() {
    companion object {
        val TAG: String = this::class.java.simpleName
        const val ONGOING_NOTIFICATION_ID = 1
        const val CHANNEL_ID = "ServiceBasicStudy"
        const val CHANNEL_NAME = "ServiceBasicStudy"
    }

    private var startMode = START_NOT_STICKY    // indicates how to behave if the service is killed
    private var binder = MyBinder()             // interface for clients that bind
    private var allowRebind = true              // indicates whether onRebind should be used

    private val isUiThread = isUiThread()

    private lateinit var windowManager: WindowManager
    private val overlayView: LinearLayout by lazy { LayoutInflater.from(this).inflate(R.layout.overlay_layout, null) as LinearLayout }
    private lateinit var params: WindowManager.LayoutParams

    @SuppressLint("ScheduleExactAlarm")
    override fun onCreate() {
        // The service is being created
        Log.d(TAG, "onCreate")

        // WindowManager を取得
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // LinearLayout をインフレート
        overlayView.apply(clickListener())

        // オーバーレイを表示するためのパラメータ設定
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        val restartServiceIntent = Intent(applicationContext, OverlayService::class.java)
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE // FLAG_ONE_SHOT を使用して新しいインテントで上書き
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 1000, // 1秒後にサービス再起動
            restartServicePendingIntent
        )
// WorkRequest を作成
        // オーバーレイビューを画面に追加
        windowManager.addView(overlayView, params)
    }

    private val channelId by lazy {
        createNotificationChannel(this, CHANNEL_ID, CHANNEL_NAME)
    }

    private fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String
    ): String {
        val channel = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val service = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The service is starting, due to a call to startService()
        Log.d(TAG, "onStartCommand")
        Toast.makeText(applicationContext, "onStartCommand", Toast.LENGTH_SHORT).show()

        val notification = buildNotification(this, channelId)
        startForeground(ONGOING_NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // A client is binding to the service with bindService()
        Log.d(TAG, "onBind")
        Toast.makeText(applicationContext, "onBind", Toast.LENGTH_SHORT).show()
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        Log.d(TAG, "onUnbind")
        Toast.makeText(applicationContext, "onUnbind", Toast.LENGTH_SHORT).show()

        return allowRebind
    }

    override fun onRebind(intent: Intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
        Log.d(TAG, "onRebind")
        Toast.makeText(applicationContext, "onRebind", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        // The service is no longer used and is being destroyed
        Log.d(TAG, "onDestroy")
        Toast.makeText(applicationContext, "onDestroy", Toast.LENGTH_SHORT).show()
    }

    inner class MyBinder : Binder() {
        fun getService(): OverlayService = this@OverlayService
    }


    fun awesomeMethod(msg: String) {
        Log.d(TAG, "awesomeMethod")

        Toast.makeText(
            applicationContext,
            "isUiThread $isUiThread / $msg",
            Toast.LENGTH_SHORT
        ).show()
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved")
        Toast.makeText(applicationContext, "onTaskRemoved", Toast.LENGTH_SHORT).show()

        // サービスを再起動するための処理
        val restartServiceIntent = Intent(applicationContext, OverlayService::class.java)
        val restartServicePendingIntent = PendingIntent.getService(applicationContext, 1, restartServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, restartServicePendingIntent)  // 1秒後にサービス再起動
    }
    private fun buildNotification(context: Context, channelId: String): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Service Basic Study")
            .setContentText("Application is active.")
            .setContentIntent(pendingIntent)
            .setTicker("Application is active")
            .build()
    }

    private fun isUiThread(): Boolean {
        return Thread.currentThread() == Looper.getMainLooper().thread
    }
    val displaySize: Point by lazy {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        size
    }

    private var isLongClick: Boolean = false
    @SuppressLint("ClickableViewAccessibility")
    private fun clickListener(): View.() -> Unit {
        return {
            setOnLongClickListener { view -> // ロングタップ状態にする
                isLongClick = true
                // ロングタップ状態が分かりやすいように背景色を変える
                view.setBackgroundResource(R.color.black)
                false
            }.apply {
                setOnTouchListener { view, motionEvent ->
                    // タップした位置を取得する
                    val x = motionEvent.rawX.toInt()
                    val y = motionEvent.rawY.toInt()

                    when (motionEvent.action) {
                        // Viewを移動させてるときに呼ばれる
                        MotionEvent.ACTION_MOVE -> {
                            if (isLongClick) {
                                // 中心からの座標を計算する
                                val centerX = x - (displaySize.x / 2)
                                val centerY = y - (displaySize.y / 2)

                                // オーバーレイ表示領域の座標を移動させる
                                params.x = centerX
                                params.y = centerY

                                // 移動した分を更新する
                                Handler(Looper.getMainLooper()).post {
                                    windowManager.updateViewLayout(overlayView, params)
                                }
                            }
                        }

                        // Viewの移動が終わったときに呼ばれる
                        MotionEvent.ACTION_UP -> {
                            if (isLongClick) {
                                // 背景色を戻す
                                view.setBackgroundResource(android.R.color.white)
                            }
                            isLongClick = false
                        }
                    }
                    false
                }
            }
        }
    }
}
