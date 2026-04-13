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

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.databinding.ItemRuleBinding
import com.example.smartnotifier.ui.common.util.ContainsFilterAdapter
import com.example.smartnotifier.ui.common.util.IconCache

/**
 * ルール一覧表示用の RecyclerView.Adapter
 *
 * 検索タイトル・音声メッセージの保存は、
 * フォーカスロスト時と候補選択時に行う。
 */
class RulesAdapter(
    private val onEnabledChanged: (RuleEntity) -> Unit,
    private val onCopyClicked: (RuleEntity) -> Unit,
    private val onDeleteClicked: (RuleEntity) -> Unit,
    private val onPlayClicked: (RuleEntity) -> Unit,
    private val onRuleUpdatedImmediate: (RuleEntity) -> Unit,
    private val getTitleSuggestions: (RuleEntity) -> List<String>
) : ListAdapter<RuleEntity, RulesAdapter.RuleViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private var notificationAccessGranted: Boolean = false

    private object PayloadPermissionChanged

    fun setNotificationAccessGranted(granted: Boolean) {
        if (notificationAccessGranted == granted) return
        notificationAccessGranted = granted
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount, PayloadPermissionChanged)
    }

    inner class RuleViewHolder(
        private val binding: ItemRuleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentRule: RuleEntity? = null
        private var suppressTextCallback = false
        private var titleAdapter: ContainsFilterAdapter? = null

        init {
            binding.editSrhTitle.setOnClickListener {
                binding.editSrhTitle.showDropDown()
            }
            binding.editSrhTitle.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val text = binding.editSrhTitle.text?.toString().orEmpty()
                    if (text.isNotEmpty()) {
                        titleAdapter?.filter?.filter(text) { count ->
                            if (count > 0) {
                                binding.editSrhTitle.showDropDown()
                            }
                        }
                    }
                } else {
                    val rule = currentRule ?: return@OnFocusChangeListener
                    val text = binding.editSrhTitle.text?.toString().orEmpty()
                    if (text != rule.srhTitle) {
                        onRuleUpdatedImmediate(rule.copy(srhTitle = text))
                    }
                }
            }
            binding.editVoiceMsg.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val rule = currentRule ?: return@OnFocusChangeListener
                    val text = binding.editVoiceMsg.text?.toString().orEmpty()
                    if (text != rule.voiceMsg) {
                        onRuleUpdatedImmediate(rule.copy(voiceMsg = text))
                    }
                }
            }

            binding.editSrhTitle.setOnItemClickListener { _, _, position, _ ->
                val rule = currentRule ?: return@setOnItemClickListener
                val selected = titleAdapter?.getItem(position).orEmpty()
                if (selected != rule.srhTitle) {
                    onRuleUpdatedImmediate(rule.copy(srhTitle = selected))
                }
            }
        }

        fun bind(rule: RuleEntity) {
            val context = binding.root.context
            currentRule = rule

            binding.txtAppName.text = rule.appLabel
            binding.imgAppIcon.setImageBitmap(IconCache.getAppIcon(context, rule.packageName))
            binding.txtChannelName.text = rule.channelName

            val suggestions = getTitleSuggestions(rule)
            titleAdapter = ContainsFilterAdapter(context, suggestions)
            binding.editSrhTitle.setAdapter(titleAdapter)

            suppressTextCallback = true
            try {
                if (!binding.editSrhTitle.hasFocus()) {
                    val want = rule.srhTitle
                    val now = binding.editSrhTitle.text?.toString().orEmpty()
                    if (now != want) {
                        binding.editSrhTitle.setText(want, false)
                    }
                }

                if (!binding.editVoiceMsg.hasFocus()) {
                    val wantVoice = rule.voiceMsg
                    val nowVoice = binding.editVoiceMsg.text?.toString().orEmpty()
                    if (nowVoice != wantVoice) {
                        binding.editVoiceMsg.setText(wantVoice)
                    }
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
        private val DiffCallback = object : DiffUtil.ItemCallback<RuleEntity>() {
            override fun areItemsTheSame(oldItem: RuleEntity, newItem: RuleEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: RuleEntity, newItem: RuleEntity): Boolean =
                oldItem == newItem
        }
    }
}
