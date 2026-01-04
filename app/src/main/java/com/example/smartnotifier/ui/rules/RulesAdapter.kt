package com.example.smartnotifier.ui.rules

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.databinding.ItemRuleBinding

/**
 * ルール一覧表示用の RecyclerView.Adapter
 * 設計書の画面レイアウト項目 ①～⑧ に対応
 * トランザクションと保存最適化 (Debounce/FocusLost) を考慮
 */
class RulesAdapter(
    private val onEnabledChanged: (RuleEntity, Boolean) -> Unit,
    private val onCopyClicked: (RuleEntity) -> Unit,
    private val onDeleteClicked: (RuleEntity) -> Unit,
    private val onPlayClicked: (RuleEntity) -> Unit,
    private val onRuleUpdated: (RuleEntity) -> Unit,         // Debounce保存用
    private val onRuleUpdatedImmediate: (RuleEntity) -> Unit // 即時保存用 (フォーカスロスト時)
) : ListAdapter<RuleEntity, RulesAdapter.RuleViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RuleViewHolder(private val binding: ItemRuleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rule: RuleEntity) {
            val context = binding.root.context
            val pm = context.packageManager

            // --- アプリ情報の表示 ---
            try {
                val appInfo = pm.getApplicationInfo(rule.packageName, 0)
                binding.txtAppName.text = pm.getApplicationLabel(appInfo).toString()
                if (rule.notificationIcon != null) {
                    binding.imgAppIcon.setImageURI(rule.notificationIcon)
                } else {
                    binding.imgAppIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
                }
            } catch (e: Exception) {
                binding.txtAppName.text = rule.packageName
                binding.imgAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            // --- リスナーのリセット ---
            binding.swEnabled.setOnCheckedChangeListener(null)
            binding.editSrhTitle.onFocusChangeListener = null
            binding.editVoiceMsg.onFocusChangeListener = null

            // --- データセット ---
            binding.swEnabled.isChecked = rule.enabled
            binding.editSrhTitle.setText(rule.srhTitle)
            binding.editVoiceMsg.setText(rule.voiceMsg)

            // --- イベント設定 ---
            
            // 有効スイッチ (即時保存)
            binding.swEnabled.setOnCheckedChangeListener { _, isChecked ->
                onEnabledChanged(rule, isChecked)
            }

            // フォーカスロスト時の即時保存 (確定処理)
            binding.editSrhTitle.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val currentText = binding.editSrhTitle.text.toString()
                    if (currentText != rule.srhTitle) {
                        onRuleUpdatedImmediate(rule.copy(srhTitle = currentText))
                    }
                }
            }
            binding.editVoiceMsg.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val currentText = binding.editVoiceMsg.text.toString()
                    if (currentText != (rule.voiceMsg ?: "")) {
                        onRuleUpdatedImmediate(rule.copy(voiceMsg = currentText))
                    }
                }
            }

            // テキスト入力中の Debounce 更新
            binding.editSrhTitle.doAfterTextChanged { text ->
                val newText = text.toString()
                if (newText != rule.srhTitle) {
                    onRuleUpdated(rule.copy(srhTitle = newText))
                }
            }
            binding.editVoiceMsg.doAfterTextChanged { text ->
                val newText = text.toString()
                if (newText != (rule.voiceMsg ?: "")) {
                    onRuleUpdated(rule.copy(voiceMsg = newText))
                }
            }

            // ボタン類
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
