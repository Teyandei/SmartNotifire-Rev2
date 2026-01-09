package com.example.smartnotifier.ui.common.util

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