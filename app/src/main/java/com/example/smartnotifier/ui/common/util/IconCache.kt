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
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.drawable.toBitmap  // ← これをインポート！
import androidx.collection.LruCache
import com.example.smartnotifier.R

/**
 * アイコンイメージのキャッシュ
 */
object IconCache {
    private val cache = LruCache<String, Bitmap>(50)  // 50個キャッシュ（適宜調整）
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
    fun getAppIcon(context: Context, packageName: String): Bitmap? {
        cache.get(packageName)?.let { return it }

        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawable.toBitmap()
            cache.put(packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            // アプリが見つからない場合のフォールバック（デフォルトアイコン）
            Log.w(THIS_CLASS, "Could not get app icon for $packageName", e)
            val default = BitmapFactory.decodeResource(context.resources, R.drawable.ic_default_app)
            cache.put(packageName, default)
            default
        }
    }
}