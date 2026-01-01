package com.example.smartnotifier.ui.rules

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.databinding.ItemRuleBinding

/**
 * ルール一覧表示用の RecyclerView.Adapter
 *
 * ・1行 = RuleEntity 1件
 * ・ListAdapter を使って差分更新（DiffUtil）を自動処理
 */
class RulesAdapter(
    private val onItemClicked: (RuleEntity) -> Unit = {},
    private val onSwitchToggled: (RuleEntity, Boolean) -> Unit = { _, _ -> }
) : ListAdapter<RuleEntity, RulesAdapter.RuleViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRuleBinding.inflate(inflater, parent, false)
        return RuleViewHolder(binding, onItemClicked, onSwitchToggled)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RuleViewHolder(
        private val binding: ItemRuleBinding,
        private val onItemClicked: (RuleEntity) -> Unit,
        private val onSwitchToggled: (RuleEntity, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RuleEntity) {
            // 表示項目はお好みで調整してね
            binding.textPackageName.text = item.packageName
            binding.textTitle.text = item.srhTitle
            binding.switchEnabled.isChecked = item.enabled

            // 行タップ
            binding.root.setOnClickListener {
                onItemClicked(item)
            }

            // 有効/無効の切り替え
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onSwitchToggled(item, isChecked)
            }
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
