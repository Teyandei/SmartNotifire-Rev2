package com.example.smartnotifier.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartnotifier.databinding.FragmentMainBinding
import com.example.smartnotifier.ui.rules.RulesAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    // 設計書 ①～⑧ に対応するアダプターの初期化
    private val rulesAdapter = RulesAdapter(
        onEnabledChanged = { rule, enabled ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.update(rule.copy(enabled = enabled))
            }
        },
        onCopyClicked = { rule ->
            viewModel.duplicateRule(rule)
        },
        onDeleteClicked = { rule ->
            // TODO: 設計書 ⑥ 削除前に確認ダイアログを表示する
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.delete(rule)
            }
        },
        onPlayClicked = { rule ->
            // TODO: 設計書 ⑧ TTSで再生 (TTSManager等の連携)
        },
        onRuleUpdated = { rule ->
            // テキスト入力中の Debounce 保存
            viewModel.updateRuleDebounced(rule)
        },
        onRuleUpdatedImmediate = { rule ->
            // フォーカスロスト時などの即時保存
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.update(rule)
            }
        }
    )

    private var rulesCollectJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView セットアップ
        binding.recyclerRules.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rulesAdapter
        }

        // ⑪: 追加ボタン
        binding.btnAddRow.setOnClickListener {
            // TODO: 設計書 ⑪ 通知ログリストを表示する
        }

        // ⑬: 通知ボタン (テスト通知)
        binding.btnConfirm.setOnClickListener {
            // TODO: 設計書 ⑬ テスト通知の発行ロジック
        }

        // ⑨: 並び順スイッチと DataStore の連動
        setupSortListUi()

        // ⑫: 通知タイトルと DataStore の連動
        setupNotificationTitleUi()
    }

    private fun setupSortListUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sortList.collect { orderByPackageAsc ->
                if (binding.swSortList.isChecked != orderByPackageAsc) {
                    binding.swSortList.isChecked = orderByPackageAsc
                }
                restartRulesCollect(orderByPackageAsc)
            }
        }

        binding.swSortList.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateSortList(isChecked)
        }
    }

    private fun restartRulesCollect(orderByPackageAsc: Boolean) {
        rulesCollectJob?.cancel()
        rulesCollectJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rules(orderNewest = !orderByPackageAsc).collect { list ->
                rulesAdapter.submitList(list)
            }
        }
    }

    private fun setupNotificationTitleUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationTitle.collect { title ->
                if (binding.editNotificationTitle.text?.toString() != title) {
                    binding.editNotificationTitle.setText(title)
                }
            }
        }

        binding.editNotificationTitle.doAfterTextChanged { text ->
            viewModel.updateNotificationTitle(text?.toString() ?: "")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rulesCollectJob?.cancel()
        _binding = null
    }
}
