package com.example.smartnotifier.ui.log

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

import android.content.pm.PackageManager
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnotifier.R
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import com.example.smartnotifier.databinding.ItemNotificationLogBinding
import com.example.smartnotifier.ui.common.util.IconCache



/**
 * 通知ログ表示用の RecyclerView.Adapter
 * 設計書「３．通知ログリスト」に対応
 */
class NotificationLogAdapter(
    private val onLogDoubleTapped: (NotificationLogEntity) -> Unit,
) : ListAdapter<NotificationLogEntity, NotificationLogAdapter.LogViewHolder>(DiffCallback) {
    private val THIS_CLASS :String = "NotificationLogAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemNotificationLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LogViewHolder(private val binding: ItemNotificationLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(log: NotificationLogEntity) {
            val context = binding.root.context
            val pm = context.packageManager

           // アプリ情報の表示
            try {
                val appInfo = pm.getApplicationInfo(log.packageName, 0)
                binding.txtAppName.text = pm.getApplicationLabel(appInfo).toString()
                binding.imgAppIcon.setImageBitmap(IconCache.getAppIcon(context, log.packageName))
            } catch (_: PackageManager.NameNotFoundException) {
                Log.w(THIS_CLASS, "Package not found/visible: ${log.packageName}")
                binding.txtAppName.text = log.packageName  // パッケージ名表示（または "Unknown App"）
                binding.imgAppIcon.setImageResource(R.drawable.ic_default_app)  // ← nullじゃなくデフォルト画像
            }catch (e: Exception) {
                Log.e(THIS_CLASS, "Exception in bindAppInfo", e)
                binding.txtAppName.text = log.packageName
                binding.imgAppIcon.setImageResource(R.drawable.ic_default_app)
            }



            // 通知タイトル
            binding.txtNtfTitle.text = log.title

            // ダブルタップ検知
            val gestureDetector =
                GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        onLogDoubleTapped(log)
                        return true
                    }
                })

            binding.root.setOnTouchListener { v, event ->
                gestureDetector.onTouchEvent(event)
                v.performClick()
                true
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<NotificationLogEntity>() {
            override fun areItemsTheSame(oldItem: NotificationLogEntity, newItem: NotificationLogEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: NotificationLogEntity, newItem: NotificationLogEntity): Boolean =
                oldItem == newItem
        }
    }
}