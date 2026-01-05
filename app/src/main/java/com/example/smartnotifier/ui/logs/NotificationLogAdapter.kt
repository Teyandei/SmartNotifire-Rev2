package com.example.smartnotifier.ui.logs

import android.content.pm.PackageManager
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import com.example.smartnotifier.databinding.ItemNotificationLogBinding

class NotificationLogAdapter(
    private val onItemDoubleTap: (NotificationLogEntity) -> Unit
) : ListAdapter<NotificationLogEntity, NotificationLogAdapter.LogViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemNotificationLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding, onItemDoubleTap)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(
        private val binding: ItemNotificationLogBinding,
        private val onItemDoubleTap: (NotificationLogEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val gestureDetector: GestureDetectorCompat =
            GestureDetectorCompat(binding.root.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    boundLog?.let { onItemDoubleTap(it) }
                    return true
                }
            })

        private var boundLog: NotificationLogEntity? = null

        fun bind(log: NotificationLogEntity) {
            boundLog = log
            val context = binding.root.context
            val pm: PackageManager = context.packageManager

            try {
                val appInfo = pm.getApplicationInfo(log.packageName, 0)
                binding.txtLogAppName.text = pm.getApplicationLabel(appInfo)
                if (log.notificationIcon != null) {
                    binding.imgLogAppIcon.setImageURI(log.notificationIcon)
                } else {
                    binding.imgLogAppIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
                }
            } catch (_: Exception) {
                binding.txtLogAppName.text = log.packageName
                binding.imgLogAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            binding.txtLogTitle.text = log.title
            binding.root.setOnTouchListener { _, motionEvent ->
                gestureDetector.onTouchEvent(motionEvent)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<NotificationLogEntity>() {
            override fun areItemsTheSame(
                oldItem: NotificationLogEntity,
                newItem: NotificationLogEntity
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: NotificationLogEntity,
                newItem: NotificationLogEntity
            ): Boolean = oldItem == newItem
        }
    }
}
