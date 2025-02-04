package com.shiraselab.overlayviewsample
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ServiceWorker (context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d("ServiceWorker", "Work started")

        // バックグラウンドタスクの実行
        try {
            // 例: 長時間かかるタスク
            Thread.sleep(3000)  // 3秒間待機
            Log.d("ServiceWorker", "Work completed")
            return Result.success()  // 失敗の場合
        } catch (e: Exception) {
            Log.e("ServiceWorker", "Work failed", e)
            return Result.failure()  // 失敗の場合
        }
    }

    override fun onStopped() {
        super.onStopped()
        if (!isStopped) {
            // タスクを手動で再スケジュール
            val retryRequest = OneTimeWorkRequestBuilder<ServiceWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(applicationContext).enqueue(retryRequest)
        }
    }
}