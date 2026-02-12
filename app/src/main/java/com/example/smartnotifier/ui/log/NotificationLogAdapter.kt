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

import android.app.NotificationManager
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnotifier.R
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import com.example.smartnotifier.databinding.ItemNotificationLogBinding
import com.example.smartnotifier.ui.common.util.IconCache
import kotlin.time.Duration.Companion.milliseconds

/**
 * 通知ログ([NotificationLogEntity])の一覧を[RecyclerView]に表示するためのアダプターです。
 *
 * 設計書「３．通知ログリスト」の仕様に基づき、ユーザーが過去の通知履歴を確認し、
 * アイテムをダブルタップすることで新しいルールとして簡単に追加する機能を提供します。
 *
 * @param onLogDoubleTapped ユーザーがリストのアイテムをダブルタップした際に呼び出されるコールバック。
 *                          タップされた[NotificationLogEntity]を引数として受け取り、
 *                          ViewModelにルールの追加処理を依頼します。
 */
class NotificationLogAdapter(
    private val onLogDoubleTapped: (NotificationLogEntity) -> Unit,
) : ListAdapter<NotificationLogEntity, NotificationLogAdapter.LogViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemNotificationLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 通知ログの単一アイテムを表示するための[RecyclerView.ViewHolder]です。
     *
     * @property binding このViewHolderが管理するビューへの参照を持つ[ItemNotificationLogBinding]。
     */
    inner class LogViewHolder(private val binding: ItemNotificationLogBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * 指定された[NotificationLogEntity]のデータをビューにバインドします。
         *
         * アプリ名、通知タイトル、および[IconCache]から取得したアプリアイコンを対応するビューに設定します。
         * また、アイテムビューに対するダブルタップを検出するための[GestureDetector]をセットアップし、
         * ダブルタップが検出された際には[onLogDoubleTapped]コールバックを呼び出します。
         *
         * @param log 表示する[NotificationLogEntity]インスタンス。
         */
        fun bind(log: NotificationLogEntity) {
            val context = binding.root.context

            // アプリ情報の表示
            binding.txtAppName.text = log.appLabel
            binding.imgAppIcon.setImageBitmap(IconCache.getAppIcon(context, log.packageName))

            // チャンネル名
            binding.txtNtfTitle.text = log.channelName

            // 受信回数
            val diffDays = (System.currentTimeMillis() - log.created).milliseconds.inWholeDays + 1L
            val receivedParDay = log.receivedCount.toDouble() / diffDays
            binding.txtReceived.text = context.getString(R.string.receivedCount, receivedParDay)

            // Importance
            if (log.importance < NotificationManager.IMPORTANCE_DEFAULT) {
                binding.root.alpha = 0.5f
                binding.txtSilent.isVisible = true
                binding.txtSilent.text = context.getString(R.string.importance_silent)
            } else {
                binding.root.alpha = 1.0f
                binding.txtSilent.isVisible = false
                binding.txtSilent.text = ""
            }

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
        private const val THIS_CLASS :String = "NotificationLogAdapter"
        private val DiffCallback = object : DiffUtil.ItemCallback<NotificationLogEntity>() {
            override fun areItemsTheSame(oldItem: NotificationLogEntity, newItem: NotificationLogEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: NotificationLogEntity, newItem: NotificationLogEntity): Boolean =
                oldItem == newItem
        }
    }
}
