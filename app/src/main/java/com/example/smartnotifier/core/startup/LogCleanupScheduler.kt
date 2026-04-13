package com.example.smartnotifier.core.startup

/*
 * SmartNotifier-Rev2
 * Copyright (C) 2026  Takeaki Yoshizawa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smartnotifier.core.service.LogCleanupWorker
import java.util.concurrent.TimeUnit

/**
 * 通知ログクリーナップのWorkManager登録処理
 *
 * 参考：[WorkRequest の定義](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work?hl=ja)
 *
 * @see LogCleanupWorker
 */
object LogCleanupScheduler {

    private const val UNIQUE_WORK_NAME = "log_cleanup"

    fun enqueue(context: Context) {
        val request = PeriodicWorkRequestBuilder<LogCleanupWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
