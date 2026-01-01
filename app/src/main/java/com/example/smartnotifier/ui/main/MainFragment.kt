package com.example.smartnotifier.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.databinding.FragmentMainBinding
import com.example.smartnotifier.ui.rules.RulesAdapter
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val rulesAdapter = RulesAdapter(
        onItemClicked = { rule ->
            // 行タップ時の処理
        },
        onSwitchToggled = { rule, enabled ->
            // 有効/無効切り替え時の処理（viewModel.update(...) など）
        }
    )

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
//        val dummy = listOf(
//            RuleEntity(
//                id = 1,
//                channelId = "test",
//                packageName = "com.example.app",
//                srhTitle = "テストタイトル",
//                notificationIcon = null,
//                voiceMsg = null,
//                enabled = true
//            )
//        )
//        rulesAdapter.submitList(dummy)
        binding.recyclerRules.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRules.adapter = rulesAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rules(orderNewest = true).collect { list ->
                rulesAdapter.submitList(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
