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
import androidx.core.graphics.drawable.toBitmap  // ← これをインポート！
import androidx.collection.LruCache
import com.example.smartnotifier.R


object IconCache {
    private val cache = LruCache<String, Bitmap>(50)  // 50個キャッシュ（適宜調整）

    fun getAppIcon(context: Context, packageName: String): Bitmap? {
        cache.get(packageName)?.let { return it }

        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawable.toBitmap()
            cache.put(packageName, bitmap)
            bitmap
        } catch (_: PackageManager.NameNotFoundException) {
            // アプリが見つからない場合のフォールバック（デフォルトアイコン）
            val default = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
            cache.put(packageName, default)
            default
        }
    }
}