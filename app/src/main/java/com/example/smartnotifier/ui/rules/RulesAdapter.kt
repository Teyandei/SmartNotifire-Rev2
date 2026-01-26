package com.example.smartnotifier.ui.rules

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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.databinding.ItemRuleBinding
import com.example.smartnotifier.ui.common.util.IconCache

/**
 * ルール一覧表示用の RecyclerView.Adapter
 * 設計書の画面レイアウト項目 ①～⑧ に対応
 * トランザクションと保存最適化 (Debounce/FocusLost) を考慮
 */
class RulesAdapter(
    private val onEnabledChanged: (RuleEntity) -> Unit,
    private val onCopyClicked: (RuleEntity) -> Unit,
    private val onDeleteClicked: (RuleEntity) -> Unit,
    private val onPlayClicked: (RuleEntity) -> Unit,
    private val onRuleUpdated: (RuleEntity) -> Unit,         // Debounce保存用
    private val onRuleUpdatedImmediate: (RuleEntity) -> Unit // 即時保存用 (フォーカスロスト時)
) : ListAdapter<RuleEntity, RulesAdapter.RuleViewHolder>(DiffCallback) {

    /**
     * RecyclerView 用 ViewHolder を生成する。
     *
     * 行レイアウトは [ItemRuleBinding] を使用し、
     * ルール1件分の表示と編集を担当する [RuleViewHolder] を返す。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuleViewHolder(binding)
    }

    /**
     * 本来のonBindViewHolderは使わないので空実装
     */
    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private var notificationAccessGranted: Boolean = false

    // イベントの種類を表す型安全なトークン
    private object PayloadPermissionChanged

    /**
     * 通知アクセス権限の付与状態を設定する。
     *
     * 権限状態はスイッチの有効/無効や表示状態に影響するため、
     * 変更後は一覧全体を再描画する。
     */
    fun setNotificationAccessGranted(granted: Boolean) {
        if (notificationAccessGranted == granted) return
        notificationAccessGranted = granted
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount, PayloadPermissionChanged)
    }

    /**
     * ルール1行分の表示と編集を担当する ViewHolder。
     *
     * 検索タイトル・音声メッセージの編集、スイッチ操作、
     * 各種アクションボタン（コピー・削除・再生）を管理する。
     */
    inner class RuleViewHolder(private val binding: ItemRuleBinding)
        : RecyclerView.ViewHolder(binding.root) {

        private var currentRule: RuleEntity? = null
        private var suppressTextCallback = false

        init {
           // フォーカスロスト時の即時保存
            binding.editSrhTitle.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val rule = currentRule ?: return@OnFocusChangeListener
                    val text = binding.editSrhTitle.text?.toString().orEmpty()
                    if (text != rule.srhTitle) onRuleUpdatedImmediate(rule.copy(srhTitle = text))
                }
            }
            binding.editVoiceMsg.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val rule = currentRule ?: return@OnFocusChangeListener
                    val text = binding.editVoiceMsg.text?.toString().orEmpty()
                    if (text != rule.voiceMsg
                    ) onRuleUpdatedImmediate(rule.copy(voiceMsg = text))
                }
            }

            // Debounce保存（watcherは1回だけ）
            binding.editSrhTitle.doAfterTextChanged { editable ->
                if (suppressTextCallback) return@doAfterTextChanged
                if (!binding.editSrhTitle.hasFocus()) return@doAfterTextChanged
                val rule = currentRule ?: return@doAfterTextChanged
                val newText = editable?.toString().orEmpty()
                if (newText != rule.srhTitle) onRuleUpdated(rule.copy(srhTitle = newText))
            }
            binding.editVoiceMsg.doAfterTextChanged { editable ->
                if (suppressTextCallback) return@doAfterTextChanged
                if (!binding.editVoiceMsg.hasFocus()) return@doAfterTextChanged
                val rule = currentRule ?: return@doAfterTextChanged
                val newText = editable?.toString().orEmpty()
                if (newText != rule.voiceMsg) onRuleUpdated(rule.copy(voiceMsg = newText))
            }
        }

        /**
         * 指定されたルールを UI に反映する。
         *
         * スイッチ状態、テキスト入力欄、各種ボタンのイベントを設定し、
         * フォーカス状態と Debounce 保存仕様を考慮して表示を行う。
         */
        fun bind(rule: RuleEntity) {
            val context = binding.root.context
            currentRule = rule

            // ①② アプリアイコン・アプリ名を表示
            binding.txtAppName.text = rule.appLabel
            binding.imgAppIcon.setImageBitmap(IconCache.getAppIcon(context, rule.packageName))

            // ★ setTextは「プログラム更新時だけ」発火させる
            //    その間 watcher を黙らせる（無限ループ防止）
            suppressTextCallback = true
            try {
                if (!binding.editSrhTitle.hasFocus()) {
                    val want = rule.srhTitle
                    val now = binding.editSrhTitle.text?.toString().orEmpty()
                    if (now != want) binding.editSrhTitle.setText(want)
                }

                val wantVoice = rule.voiceMsg
                if (!binding.editVoiceMsg.hasFocus()) {
                    val nowVoice = binding.editVoiceMsg.text?.toString().orEmpty()
                    if (nowVoice != wantVoice) binding.editVoiceMsg.setText(wantVoice)
                }
            } finally {
                suppressTextCallback = false
            }

            binding.swEnabled.setOnCheckedChangeListener(null)
            binding.swEnabled.isChecked = rule.enabled
            binding.swEnabled.setOnCheckedChangeListener { _, isChecked ->
                onEnabledChanged(rule.copy(enabled = isChecked))
            }
            binding.btnCopyRow.setOnClickListener { onCopyClicked(rule) }
            binding.btnDeleteRow.setOnClickListener { onDeleteClicked(rule) }
            binding.btnPlayVoice.setOnClickListener { onPlayClicked(rule) }
        }
    }


    companion object {

        /**
         * RuleEntity 用 DiffUtil。
         *
         * ID による同一性判定と、データクラスの equals による内容比較を行う。
         */
        private val DiffCallback = object : DiffUtil.ItemCallback<RuleEntity>() {
            override fun areItemsTheSame(oldItem: RuleEntity, newItem: RuleEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: RuleEntity, newItem: RuleEntity): Boolean =
                oldItem == newItem
        }
    }
}
