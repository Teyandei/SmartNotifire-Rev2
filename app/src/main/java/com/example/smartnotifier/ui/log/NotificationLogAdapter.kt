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
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnotifier.R
import com.example.smartnotifier.data.db.NotificationLogListItem
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import com.example.smartnotifier.databinding.ItemNotificationLogBinding
import com.example.smartnotifier.ui.common.util.IconCache
import kotlin.time.Duration.Companion.milliseconds

/**
 * 通知ログ([NotificationLogEntity])の一覧を[RecyclerView]に表示するためのアダプターです。
 *
 * 設計書「３．通知ログリスト」の仕様に基づき、ユーザーが過去の通知履歴を確認し、
 * 追加ボタンをタップすることで新しいルールとして簡単に追加する機能を提供します。
 *
 * @param onAddRuleClicked ユーザーがリストのプラスのpタンをタップした際に呼び出されるコールバック。
 *                          タップされた[NotificationLogEntity]を引数として受け取り、
 *                          ViewModelにルールの追加処理を依頼します。
 */
class NotificationLogAdapter(
    private val onAddRuleClicked: (NotificationLogListItem) -> Unit,
) : ListAdapter<NotificationLogListItem, NotificationLogAdapter.LogViewHolder>(DiffCallback) {

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
         * アプリ名、チャンネル名、および[IconCache]から取得したアプリアイコンを対応するビューに設定します。
         * また、アイテムビューに対するルール追加ボタンタップを検出するためのリスナーをセットアップし、
         * タップした際には[onAddRuleClicked]コールバックを呼び出します。
         *
         * @param log 表示する[NotificationLogListItem]インスタンス。
         */
        fun bind(log: NotificationLogListItem) {
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
            val silent = log.importance < NotificationManager.IMPORTANCE_DEFAULT
            binding.root.setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (silent) {
                        R.color.notification_log_card_silent
                    } else {
                        R.color.notification_log_card_normal
                    }
                )
            )
            binding.txtAppName.setTextColor(
                ContextCompat.getColor(context, R.color.notification_log_text_primary)
            )
            binding.txtNtfTitle.setTextColor(
                ContextCompat.getColor(context, R.color.notification_log_text_secondary)
            )
            binding.txtReceived.setTextColor(
                ContextCompat.getColor(context, R.color.notification_log_text_secondary)
            )
            binding.txtSilent.setTextColor(
                ContextCompat.getColor(context, com.google.android.material.R.color.design_default_color_on_primary)
            )

            binding.txtSilent.isVisible = silent
            binding.txtSilent.text =
                if (silent) context.getString(R.string.importance_silent) else ""

            binding.textHasRule.isVisible = log.hasRule
            binding.textHasRule.text =
                if (log.hasRule) context.getString(R.string.has_rule_text) else ""

            binding.btnAddRule.setOnClickListener {
                onAddRuleClicked(log)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<NotificationLogListItem>() {
            override fun areItemsTheSame(oldItem: NotificationLogListItem, newItem: NotificationLogListItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: NotificationLogListItem, newItem: NotificationLogListItem): Boolean =
                oldItem == newItem
        }
    }
}
