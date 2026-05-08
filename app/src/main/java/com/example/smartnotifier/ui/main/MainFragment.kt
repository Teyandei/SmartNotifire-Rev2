package com.example.smartnotifier.ui.main

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

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.example.smartnotifier.data.db.NotificationLogListItem

/**
 * 画面の表示とユーザー入力の収集を担当するメイン画面 Fragment。
 *
 * 設計書の MVVM 方針に従い、Fragment は UI 表示とイベント受付に限定し、
 * ルール操作やログ操作などのロジックは [MainViewModel] に委譲する。
 */
class MainFragment : Fragment() {
    private companion object {
        const val DIALOG_HORIZONTAL_PADDING_DP = 24
        const val DIALOG_MESSAGE_CHECKBOX_GAP_DP = 16
    }

    private val viewModel: MainViewModel by viewModels()
    private var _binding: FragmentMainBinding? = null

    private var pendingScrollRuleId: Long? = null   // スクロール表示するルールのId

    /**
     * ViewBinding への非nullアクセサ。
     *
     * バインディングは View のライフサイクルに紐づくため、
     * 有効期間は onCreateView から onDestroyView までに限定する。
     */
    private val binding get() = _binding!!

    /**
     * テスト通知送信など、通知に関する補助処理を行うヘルパー。
     */
    private lateinit var notificationHelper: NotificationHelper

    /**
     * NotificationLogのソートに使うSpinnerの初期値取得準備完了フラグ
     */
    private var readyToPersistSpinner = false

    /**
     * ルールの音声メッセージ試聴などに利用する TTS 管理オブジェクト。
     *
     * View の破棄に合わせて終了処理を行い、リソースリークを防ぐ。
     */
    private var ttsManager: TtsManager? = null

    /**
     * Android 13+ の通知権限（POST_NOTIFICATIONS）要求用ランチャー。
     *
     * 許可された場合はテスト通知を送信し、拒否された場合はメッセージを表示する。
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) sendTestNotification()
        else Snackbar.make(binding.root, R.string.msg_notification_permission_denied, Snackbar.LENGTH_LONG).show()
    }

    /**
     * 通知検出ルール一覧（Rules）を表示する RecyclerView 用アダプター。
     */
    private lateinit var rulesAdapter: RulesAdapter

    private lateinit var logAdapter: NotificationLogAdapter

    /**
     * ルール一覧の収集（Flow collect）を管理するジョブ。
     *
     * 並び順変更などで収集条件が変わる場合はジョブを差し替える。
     */
    private var rulesCollectJob: Job? = null

    private var titleSuggestionsMap: Map<Pair<String, String>, List<String>> = emptyMap()

    /**
     * ViewBinding を初期化し、Fragment のルートビューを生成する。
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * 画面生成後の初期化処理を行う。
     *
     * RecyclerView のセットアップ、UI イベント設定、StateFlow の購読などを開始する。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ①～⑧ ルールアダプター
        rulesAdapter = RulesAdapter(
            onEnabledChanged = { rule ->
                viewModel.updateRuleOfEnabled(rule.id, rule.enabled)
            },
            onCopyClicked = { rule -> viewModel.duplicateRule(rule) },
            onDeleteClicked = { rule -> showDeleteConfirmDialog(rule) },
            onPlayClicked = { rule ->
                val msg = rule.voiceMsg
                if (msg.isNotBlank()) {
                    ttsManager?.speak(msg)
                } else {
                    ttsManager?.speak(getString(R.string.spk_msg_default, rule.appLabel))
                }
            },
            onRuleUpdatedImmediate = { rule -> viewModel.updateImmediate(rule) },
            getTitleSuggestions = { rule ->
                titleSuggestionsMap[rule.packageName to rule.channelId].orEmpty()
            }
        )

        /**
         * 通知ログ一覧（NotificationLog）を表示する RecyclerView 用アダプター。
         *
         * ログ行のダブルタップ操作は、設計書の「通知ログからルール追加」動作に対応し、
         * [MainViewModel.addRuleFromLog] に委譲する。
         */
        logAdapter = NotificationLogAdapter(
            onAddRuleClicked = { log ->
                if (log.importance < NotificationManager.IMPORTANCE_DEFAULT) {
                    showSilentWarning(log)
                } else {
                    viewModel.addRuleFromLog(log)
                }
            }
        )

        val orderNames = listOf(
            getString(R.string.list_log_order_nerswst),
            getString(R.string.list_log_order_app),
            getString(R.string.list_log_order_recvCount)
        )

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            orderNames
        )
        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        binding.spinnerSort.adapter = spinnerAdapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sortLogOrder.collect { order ->
                binding.spinnerSort.setSelection(order.ordinal)
            }
        }

        notificationHelper = NotificationHelper(requireContext())
        ttsManager = TtsManager(requireContext())

        setupRecyclerViews()
        setupObservers()
        setupClickListeners()
        setupSortListUi()
        setupNotificationTitleUi()
        setNotificationOrderBySpinner()
    }

    private var permissionDialogShowing = false

    /**
     * 画面復帰時に通知アクセス権限の状態を反映し、未許可なら案内ダイアログを表示する。
     */
    override fun onResume() {
        super.onResume()

        val granted = SmartNotificationListenerService.isPermissionGranted(requireContext())

        viewModel.setNotificationAccessGranted(granted)

        if (!granted) {
            showNotificationAccessGuideDialog()
        } else {
            permissionDialogShowing = false
            viewModel.showGettingStartedIfNeeded()
        }
    }

    /**
     * ルール一覧と通知ログ一覧の RecyclerView を初期化する。
     */
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

    /**
     * ViewModel の公開する StateFlow を購読し、UI へ反映する。
     *
     * View（Fragment）は表示に徹し、データ加工や保存のロジックは ViewModel に委譲する。
     */
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { eff ->
                    when (eff) {
                        is UiEffect.ShowSnackbar ->
                            Snackbar.make(binding.root, eff.message, Snackbar.LENGTH_LONG).show()

                        is UiEffect.ScrollToRule -> {
                            scrollToRuleId(eff.ruleId)
                        }
                        is UiEffect.ShowLogList ->
                            viewModel.setShowingLogList(eff.show)

                        UiEffect.ShowTtsHint ->
                            showTtsHintDialog()

                        UiEffect.ShowGettingStarted ->
                            showGettingStartedDialog()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isShowingLogList.collect { isShowing ->
                binding.layoutLogList.isVisible = isShowing
                binding.btnAddRow.isVisible = !isShowing
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationAccessGranted.collect { granted ->
                rulesAdapter.setNotificationAccessGranted(granted)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logItems.collect { logItems ->
                    logAdapter.submitList(logItems) {
                        binding.recyclerLogs.post {
                            binding.recyclerLogs.scrollToPosition(0)
                        }
                    }
                }
            }
        }
    }

    /**
     * 指定ルールをスクロール表示する
     *
     * @param ruleId スクロール表示するルールのId
     */
    private fun scrollToRuleId(ruleId: Long) {
        pendingScrollRuleId = ruleId
        tryScrollToPending()
    }

    /**
     * [pendingScrollRuleId]によるスクロール表示
     * pendingScrollRuleIdがリスト内にあれば、該当ルールがUI上見える位置に一度だけスクロールする。
     */
    private fun tryScrollToPending() {
        val id = pendingScrollRuleId ?: return
        val pos = rulesAdapter.currentList.indexOfFirst { it.id.toLong() == id }
        if (pos >= 0) {
            pendingScrollRuleId = null
            binding.recyclerRules.post {
                binding.recyclerRules.smoothScrollToPosition(pos)
            }
        }
    }

    /**
     * UI 部品のクリックイベントを設定する。
     */
    private fun setupClickListeners() {
        binding.btnAddRow.setOnClickListener {
            viewModel.setShowingLogList(true)
        }
        binding.btnCloseLog.setOnClickListener {
            viewModel.setShowingLogList(false)
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

    /**
     * ルール削除前の確認ダイアログを表示する。
     *
     * 削除確定時の処理は [MainViewModel.delete] に委譲する。
     */
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

    /**
     * 通知リスナー権限（Notification Access）が未許可の場合に表示する案内ダイアログ。
     *
     * 設定画面（Notification Listener Settings）へ誘導する。
     * 通知リスナー権限の承認ができないユーザーに対してはアプリの終了を選択可能とする。
     */
    private fun showNotificationAccessGuideDialog() {
        if (permissionDialogShowing) return
        permissionDialogShowing = true

        val sb = SpannableStringBuilder()
        sb.appendBody(getString(R.string.dlg_msg_notification_access_required))
        sb.appendURL(getString(R.string.msg_privacy_title), getString(R.string.msg_privacy_url))
        sb.appendBody(getString(R.string.dlg_msg_notification_access_required_part2))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dlg_title_notification_access_required))
            .setMessage(sb)
            .setCancelable(false)
            .setNegativeButton(getString(R.string.btn_exit)) {_,_ ->
                activity?.finish()
            }
            .setPositiveButton(getString(R.string.btn_ok)) { _, _ ->
                permissionDialogShowing = false
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .show()

        enableDialogMessageLinks(dialog)

    }

    /**
     * 通知権限を確認し、条件を満たす場合にテスト通知を送信する。
     *
     * Android 13+ は POST_NOTIFICATIONS の実行時権限が必要となる。
     */
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

    /**
     * 見出し風効果の装飾を入れたtextをsbに追加
     *
     * @param text 装飾対象文字列
     * @param rep textの末尾に改行を入れる個数
     */
    private fun SpannableStringBuilder.appendHeading(text: String, rep: Int = 1) {
        val start = length
        append(text).append("\n".repeat(rep))
        val end = length

        setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setSpan(
            RelativeSizeSpan(1.15f),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     *  sbへのテキスト追加
     *
     *  @param text 追加するテキスト
     *  @param rep textの末尾に改行を入れる個数
     */
    private fun SpannableStringBuilder.appendBody(text: String, rep: Int = 1) {
        append(text).append("\n".repeat(rep))
    }

    /**
     *  Markdown風([text](url))の装飾をつけたテキストをsbに追加
     *
     * @param text 装飾対象文字列
     * @param url リンク先URL
     */
    private fun SpannableStringBuilder.appendURL(text: String, url: String = text) {
        val start = length
        append(text)
        val end = length

        setSpan(
            URLSpan(url),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        append("\n")
    }

    /**
     * ダイアログの文字列装飾
     */
    private fun buildHelpText(): CharSequence {
        val sb = SpannableStringBuilder()

        sb.appendHeading(getString(R.string.appName), 0)
        sb.appendBody(" ${BuildConfig.VERSION_NAME}")
        sb.appendBody(getString(R.string.msg_about_app_part1), 2)
        sb.appendHeading(getString(R.string.msg_about_app_part2))
        sb.appendBody(getString(R.string.msg_about_app_part3), 1)
        sb.appendURL(getString(R.string.msg_about_app_part4), getString(R.string.msg_about_aoo_part5))
        sb.appendURL(getString(R.string.msg_privacy_title), getString(R.string.msg_privacy_url))

        return sb
    }

    /**
     * 初回操作説明ダイアログの文字列装飾。
     */
    private fun buildGettingStartedText(): CharSequence {
        val sb = SpannableStringBuilder()

        sb.appendBody(getString(R.string.dlg_msg_getting_started_intro), 2)
        sb.appendBody(getString(R.string.dlg_msg_getting_started_try), 2)
        sb.appendBody(getString(R.string.dlg_msg_getting_started_steps), 2)
        sb.appendBody(getString(R.string.dlg_msg_getting_started_link_label))
        sb.appendURL(
            getString(R.string.getting_started_link_text),
            getString(R.string.getting_started_url)
        )

        return sb
    }

    /**
     *  ヘルプダイアログの表示
     */
    private fun showHelpDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.help_dialog_title)
            .setMessage(buildHelpText())
            .setPositiveButton(R.string.captionClose, null)
            .show()

        enableDialogMessageLinks(dialog)
    }

    /**
     * NLS 権限が許可された直後に、最初の操作だけを案内する。
     */
    private fun showGettingStartedDialog() {
        val (contentView, doNotShowAgainCheck) = createMessageCheckDialogContent(
            message = buildGettingStartedText(),
            checkBoxText = getString(R.string.chk_do_not_show_again)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dlg_title_getting_started)
            .setView(contentView)
            .setPositiveButton(R.string.captionClose) { _, _ ->
                if (doNotShowAgainCheck.isChecked) {
                    viewModel.disableGettingStarted()
                }
            }
            .show()
    }

    /**
     * 画面で指定された通知タイトルを用いてテスト通知を送信する。
     */
    private fun sendTestNotification() {
        notificationHelper.sendTestNotification(viewModel.notificationTitle.value)
    }

    /**
     * サイレント通知をルールに追加する際の⚠警告ダイアログ
     *
     * @param log ルールに追加する通知ログ内容
     */
    private fun showSilentWarning(log: NotificationLogListItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.wrn_dnd_title)
            .setMessage(R.string.wrn_dnd_msg)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.captionAddBtn) { _, _ ->
                viewModel.addRuleFromLog(log)
            }
            .show()
    }

    /**
     * 音声案内が聞こえない場合に確認する端末設定のヒントを表示する。
     */
    private fun showTtsHintDialog() {
        val (contentView, doNotShowAgainCheck) = createMessageCheckDialogContent(
            message = getString(R.string.dlg_msg_tts_hint),
            checkBoxText = getString(R.string.chk_do_not_show_again)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dlg_title_tts_hint)
            .setView(contentView)
            .setPositiveButton(R.string.captionClose) { _, _ ->
                if (doNotShowAgainCheck.isChecked) {
                    // ユーザーが明示した場合のみ、次回以降の表示を止める。
                    viewModel.disableTtsHint()
                }
            }
            .show()
    }

    /**
     * AlertDialog の message TextView にリンク処理を有効化する。
     */
    private fun enableDialogMessageLinks(dialog: AlertDialog) {
        dialog.findViewById<TextView>(android.R.id.message)?.apply {
            movementMethod = LinkMovementMethod.getInstance()
            linksClickable = true
        }
    }

    /**
     * 本文とチェックボックスを同時に表示するためのダイアログ本文 View を作成する。
     */
    private fun createMessageCheckDialogContent(
        message: CharSequence,
        checkBoxText: CharSequence
    ): Pair<LinearLayout, CheckBox> {
        val contentView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dpToPx(DIALOG_HORIZONTAL_PADDING_DP)
            setPadding(padding, 0, padding, 0)
        }
        val messageView = TextView(requireContext()).apply {
            text = message
            movementMethod = LinkMovementMethod.getInstance()
            linksClickable = true
        }
        val doNotShowAgainCheck = CheckBox(requireContext()).apply {
            text = checkBoxText
        }
        val messageCheckGap = Space(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(DIALOG_MESSAGE_CHECKBOX_GAP_DP)
            )
        }

        contentView.addView(messageView)
        contentView.addView(messageCheckGap)
        contentView.addView(doNotShowAgainCheck)

        return contentView to doNotShowAgainCheck
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    /**
     * ルール一覧の並び順スイッチ（sortList）に関する UI と購読処理を設定する。
     *
     * 設計書の「並び順」仕様に対応し、切り替えに応じてルール一覧の収集を再開する。
     */
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

    /**
     * ルール一覧の収集ジョブをキャンセルし、指定の並び順で再収集を開始する。
     *
     * @param orderByPackageAsc true の場合は PackageName 昇順、false の場合は新しい順（ID 降順相当）
     */
    private fun restartRulesCollect(orderByPackageAsc: Boolean) {
        rulesCollectJob?.cancel()
        rulesCollectJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rules(orderNewest = !orderByPackageAsc).collect { list ->
                titleSuggestionsMap = viewModel.loadRecentTitleStringsMap(list)
                rulesAdapter.submitList(list) {     // ルール表示
                    tryScrollToPending()            // 新規ルールのスクロール表示を試みる
                }
            }
        }
    }

    /**
     * テスト通知タイトル入力（ntfTitle）に関する UI と購読処理を設定する。
     *
     * テキスト変更は [MainViewModel.updateNotificationTitle] に委譲する。
     */
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

    private fun setNotificationOrderBySpinner() {
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!readyToPersistSpinner) {
                    readyToPersistSpinner = true
                    return
                }

                viewModel.onLogOrderSelected(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    /**
     * View 破棄時の後始末を行う。
     *
     * ViewBinding と TTS を解放し、Flow 収集ジョブをキャンセルしてリークを防ぐ。
     */
    override fun onDestroyView() {
        super.onDestroyView()
        ttsManager?.shutdown()
        ttsManager = null
        rulesCollectJob?.cancel()
        _binding = null
    }
}
