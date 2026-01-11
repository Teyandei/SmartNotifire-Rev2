package com.example.smartnotifier.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartnotifier.core.notification.NotificationHelper
import com.example.smartnotifier.core.service.SmartNotificationListenerService
import com.example.smartnotifier.core.tts.TtsManager
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.databinding.FragmentMainBinding
import com.example.smartnotifier.ui.rules.RulesAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.smartnotifier.R
import com.example.smartnotifier.ui.log.NotificationLogAdapter
import com.example.smartnotifier.BuildConfig

class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()
    private var _binding: FragmentMainBinding? = null

    private val binding get() = _binding!!

    private lateinit var notificationHelper: NotificationHelper
    private var ttsManager: TtsManager? = null

   // 権限リクエスト用
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) sendTestNotification()
        else Snackbar.make(binding.root, R.string.msg_notification_permission_denied, Snackbar.LENGTH_LONG).show()
    }

    private lateinit var enabledChangeListener: (RuleEntity, Boolean, CompoundButton) -> Unit
    private lateinit var rulesAdapter: RulesAdapter


    // 通知ログアダプター
    private val logAdapter by lazy {
        NotificationLogAdapter(
            onLogDoubleTapped = { log ->
                viewModel.addRuleFromLog(log)
            }
        )
    }

    private var rulesCollectJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ①～⑧ ルールアダプター
        enabledChangeListener = enabled@{ rule, enabled, button ->
            if (!SmartNotificationListenerService.isPermissionGranted(requireContext())) {
                // 念のため戻す
                button.setOnCheckedChangeListener(null)
                button.isChecked = false
                button.setOnCheckedChangeListener { _, checked ->
                    enabledChangeListener(rule, checked, button)
                }
                return@enabled
            }
            viewModel.updateImmediate(rule.copy(enabled = enabled))
        }
        rulesAdapter = RulesAdapter(
            onEnabledChanged = enabledChangeListener,
            onCopyClicked = { rule -> viewModel.duplicateRule(rule) },
            onDeleteClicked = { rule -> showDeleteConfirmDialog(rule) },
            onPlayClicked = { rule ->
                val msg = rule.voiceMsg
                if (!msg.isNullOrBlank()) {
                    ttsManager?.speak(msg)
                } else {
                    Snackbar.make(binding.root, R.string.msg_no_voice_message, Snackbar.LENGTH_SHORT).show()
                }
            },
            onRuleUpdated = { rule -> viewModel.updateRuleDebounced(rule) },
            onRuleUpdatedImmediate = { rule -> viewModel.updateImmediate(rule) }
        )
        notificationHelper = NotificationHelper(requireContext())
        ttsManager = TtsManager(requireContext())

        setupRecyclerViews()
        setupObservers()
        setupClickListeners()
        setupSortListUi()
        setupNotificationTitleUi()
    }

    private var permissionDialogShowing = false

    override fun onResume() {
        super.onResume()

        val granted = SmartNotificationListenerService.isPermissionGranted(requireContext())
        viewModel.setNotificationAccessGranted(granted)

        if (!granted) {
            showNotificationAccessGuideDialog()
        } else {
            permissionDialogShowing = false
        }
    }

    private fun setupRecyclerViews() {
        binding.recyclerRules.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rulesAdapter
            (binding.recyclerRules.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
        binding.recyclerLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = logAdapter
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isShowingLogList.collect { isShowing ->
                binding.layoutLogList.isVisible = isShowing
                binding.btnAddRow.isVisible = !isShowing
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationLogs.collect { logs ->
                logAdapter.submitList(logs)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationAccessGranted.collect { granted ->
                rulesAdapter.setNotificationAccessGranted(granted)
            }
        }

    }

    private fun setupClickListeners() {
        binding.btnAddRow.setOnClickListener {
            viewModel.setShowingLogList(true)
        }
        binding.btnConfirm.setOnClickListener {
            checkPermissionAndSendNotification()
        }
        binding.mainRoot.setOnClickListener {
            if (viewModel.isShowingLogList.value) {
                viewModel.setShowingLogList(false)
            }
        }
        binding.imageButtonHelp.setOnClickListener {
            showHelpDialog()
        }
    }

    private fun showDeleteConfirmDialog(rule: RuleEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dlg_title_delete_confirm)
            .setMessage(R.string.dlg_msg_delete_confirm)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.delete(rule)
                    Snackbar.make(binding.root, R.string.dle_msg_delete_sucess, Snackbar.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showNotificationAccessGuideDialog() {
        if (permissionDialogShowing) return
        permissionDialogShowing = true

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dlg_title_notification_access_required))
            .setMessage(getString(R.string.dlg_msg_notification_access_required))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.btn_ok)) { _, _ ->
                permissionDialogShowing = false
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .show()
    }

    private fun checkPermissionAndSendNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) 
                        == PackageManager.PERMISSION_GRANTED -> sendTestNotification()
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            sendTestNotification()
        }
    }

    private fun buildHelpText(): CharSequence {
        val sb = SpannableStringBuilder()

        fun appendHeading(text: String, rep: Int = 1) {
            val start = sb.length
            sb.append(text).append("\n".repeat(rep))
            val end = sb.length - 1

            sb.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            sb.setSpan(
                RelativeSizeSpan(1.15f),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        fun appendBody(text: String, rep: Int = 1) {
            sb.append(text).append("\n".repeat(rep))
        }

        fun appendURL(text: String) {
            val start = sb.length
            sb.append(text)
            val end = sb.length - 1

            sb.setSpan(
                URLSpan(text),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        appendHeading(getString(R.string.appName), 0)
        appendBody(" ${BuildConfig.VERSION_NAME}")
        appendBody(getString(R.string.msg_about_app_part1), 2)
        appendHeading(getString(R.string.msg_about_app_part2))
        appendBody(getString(R.string.msg_about_app_part3), 1)
        appendURL(getString(R.string.msg_about_app_part4))

        return sb
    }


    private fun showHelpDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.help_dialog_title)
            .setMessage(buildHelpText())
            .setPositiveButton(R.string.captionClose, null)
            .show()
    }

    private fun sendTestNotification() {
        notificationHelper.sendTestNotification(viewModel.notificationTitle.value)
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
        ttsManager?.shutdown()
        ttsManager = null
        rulesCollectJob?.cancel()
        _binding = null
    }
}
