package com.example.smartnotifier.ui.rules

import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
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
    private val onEnabledChanged: (RuleEntity, Boolean, CompoundButton) -> Unit,
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
    private var notificationAccessGranted: Boolean = false

    fun setNotificationAccessGranted(granted: Boolean) {
        notificationAccessGranted = granted
        notifyDataSetChanged()
    }

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
                    if (text != (rule.voiceMsg
                            ?: "")
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
                if (newText != (rule.voiceMsg ?: "")) onRuleUpdated(rule.copy(voiceMsg = newText))
            }
        }
        // ① アイコン（notificationIcon が読めればそれ、ダメならアプリのデフォルトアイコン）
        private fun bindAppInfo(rule: RuleEntity) {
            val context = binding.root.context
            val pm = context.packageManager

            // ② アプリ名（packageName -> label）
            try {
                val appInfo = pm.getApplicationInfo(rule.packageName, 0)
                binding.txtAppName.text = pm.getApplicationLabel(appInfo).toString()
                binding.imgAppIcon.setImageBitmap(IconCache.getAppIcon(context, rule.packageName))
            } catch (_: PackageManager.NameNotFoundException) {
                // 取れない場合は packageName を表示
                binding.txtAppName.text = rule.packageName
            } catch (e: Exception) {
                Log.e("RulesAdapter", "Exception in bindAppInfo", e)
            }
        }
        fun bind(rule: RuleEntity) {
            // ①② アプリアイコン・アプリ名を表示
            bindAppInfo(rule)

            currentRule = rule

            // スイッチは従来通りリスナー付け直しでOK
            binding.swEnabled.setOnCheckedChangeListener(null)
            // 権限がないなら OFF 表示に寄せる（DBがtrueでもUIはOFF）
            val effectiveEnabled = notificationAccessGranted && rule.enabled
            binding.swEnabled.isChecked = effectiveEnabled

            // 権限がない間は操作不能
            binding.swEnabled.isEnabled = notificationAccessGranted

            binding.swEnabled.setOnCheckedChangeListener { button, isChecked ->
                if (!notificationAccessGranted) {
                    // 保険（基本ここには来ない）
                    button.setOnCheckedChangeListener(null)
                    button.isChecked = false
                    return@setOnCheckedChangeListener
                }
                onEnabledChanged(rule, isChecked, button)
            }

            // ★ setTextは「プログラム更新時だけ」発火させる
            //    その間 watcher を黙らせる（無限ループ防止）
            suppressTextCallback = true
            try {
                if (!binding.editSrhTitle.hasFocus()) {
                    val want = rule.srhTitle
                    val now = binding.editSrhTitle.text?.toString().orEmpty()
                    if (now != want) binding.editSrhTitle.setText(want)
                }

                val wantVoice = rule.voiceMsg ?: ""
                if (!binding.editVoiceMsg.hasFocus()) {
                    val nowVoice = binding.editVoiceMsg.text?.toString().orEmpty()
                    if (nowVoice != wantVoice) binding.editVoiceMsg.setText(wantVoice)
                }
            } finally {
                suppressTextCallback = false
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
