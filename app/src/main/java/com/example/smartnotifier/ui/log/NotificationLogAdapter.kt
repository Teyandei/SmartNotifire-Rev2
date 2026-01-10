package com.example.smartnotifier.ui.log

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