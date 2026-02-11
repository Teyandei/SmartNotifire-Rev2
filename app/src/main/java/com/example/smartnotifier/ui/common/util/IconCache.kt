package com.example.smartnotifier.ui.common.util

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
import android.graphics.Bitmap
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.collection.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.example.smartnotifier.R
import androidx.core.graphics.createBitmap

/**
 * アイコンイメージのキャッシュ
 */
object IconCache {
    // Cache size matches NotificationLog max (100) to avoid icon re-fetch during scrolling

    private val cache = LruCache<String, Bitmap>(100)  // 100個キャッシュ（適宜調整）
    private const val THIS_CLASS = "IconCache"

    /**
     * アイコンイメージの取得とキャッシュ
     *
     * - パッケージ名から見つからない場合は、デフォルトアイコンを使用する。
     * - 本処理のコールで例外は発生しない
     *
     * @param context Context
     * @param packageName パッケージ名
     * @return Bitmap
     */
    fun getAppIcon(context: Context, packageName: String): Bitmap {
        cache[packageName]?.let { return it }

        val pm = context.packageManager

        val bmp = try {
            pm.getApplicationIcon(packageName).toBitmap()
        } catch (e: Exception) {
            Log.w(THIS_CLASS,
                "Could not get app icon. pkg=$packageName ex=${e::class.java.name} msg=${e.message}",
                e
            )
            val d = AppCompatResources.getDrawable(context, R.drawable.ic_default_app)
            if (d == null) {
                Log.e(THIS_CLASS, "Default icon drawable is null. resource missing?")
                createBitmap(1, 1) // 最低限の保険
            } else {
                d.toBitmap()
            }
        }

        cache.put(packageName, bmp)
        return bmp
    }
}
