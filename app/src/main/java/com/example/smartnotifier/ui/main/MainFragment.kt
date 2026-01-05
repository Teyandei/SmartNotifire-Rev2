package com.example.smartnotifier.ui.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartnotifier.R
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.core.tts.SpeechQueue
import com.example.smartnotifier.databinding.FragmentMainBinding
import com.example.smartnotifier.ui.logs.NotificationLogBottomSheet
import com.example.smartnotifier.ui.rules.RulesAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
            confirmDelete(rule)
        },
        onPlayClicked = { rule ->
            val message = rule.voiceMsg?.takeIf { it.isNotBlank() }
                ?: rule.srhTitle
            SpeechQueue.speakImmediately(requireContext(), message)
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

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                sendTestNotification()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.notification_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

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
            showNotificationLogSheet()
        }

        // ⑬: 通知ボタン (テスト通知)
        binding.btnConfirm.setOnClickListener {
            requestNotificationPermissionAndSend()
        }

        // ⑨: 並び順スイッチと DataStore の連動
        setupSortListUi()

        // ⑫: 通知タイトルと DataStore の連動
        setupNotificationTitleUi()

        SpeechQueue.initialize(requireContext())
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

    private fun confirmDelete(rule: RuleEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_message))
            .setPositiveButton(getString(R.string.dialog_delete_positive)) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.delete(rule)
                }
            }
            .setNegativeButton(getString(R.string.dialog_delete_negative), null)
            .show()
    }

    private fun showNotificationLogSheet() {
        val sheet = NotificationLogBottomSheet()
        binding.btnAddRow.hide()
        sheet.onClosed = {
            binding.btnAddRow.show()
        }
        sheet.show(parentFragmentManager, "notificationLogSheet")
    }

    private fun requestNotificationPermissionAndSend() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        sendTestNotification()
    }

    private fun sendTestNotification() {
        val context = requireContext()
        val notificationManager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHECK_CHANNEL_ID,
                getString(R.string.notification_channel_check_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_check_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = binding.editNotificationTitle.text?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: viewModel.notificationTitle.value

        val notification = NotificationCompat.Builder(context, CHECK_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(getString(R.string.notification_check_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(CHECK_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHECK_NOTIFICATION_ID = 1001
        private const val CHECK_CHANNEL_ID = "check"
    }
}
