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

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnotifier.R
import com.example.smartnotifier.core.datastore.AppPrefs
import com.example.smartnotifier.core.datastore.appPrefsDataStore
import com.example.smartnotifier.data.db.DatabaseProvider
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.data.repository.NotificationLogRepository
import com.example.smartnotifier.data.repository.RulesRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

sealed interface AddRuleFromLogEvent {
    data class Success(val title: String) : AddRuleFromLogEvent
    data object TooManySameNames : AddRuleFromLogEvent
    data object Failed : AddRuleFromLogEvent
    data object FailedCopy : AddRuleFromLogEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * アプリケーション全体の状態とビジネスロジックを管理する ViewModel。
     *
     * MVVM 設計に基づき、UI（Fragment）からの要求を受け取り、
     * データストア・データベース・リポジトリとの橋渡しを行う。
     */

    private val appContext = application.applicationContext
    private val dataStore = appContext.appPrefsDataStore
    private val db = DatabaseProvider.get(appContext)
    val rulesRepo = RulesRepository(db)

    private val notificationLogRepo = NotificationLogRepository(db)

    /**
     * ルール更新要求を一時的にバッファリングする SharedFlow。
     *
     * Debounce 処理により、入力中の頻繁な更新をまとめて保存する。
     */
    private val _ruleUpdateBuffer = MutableSharedFlow<RuleEntity>(replay = 0)

    /**
     * UI に通知するエラーメッセージ用 Flow。
     */
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    /**
     * UIに通知するメッセージ用 Flow。
     */
    private val _addRuleFromLogEvent = MutableSharedFlow<AddRuleFromLogEvent>()
    val addRuleFromLogEvent = _addRuleFromLogEvent.asSharedFlow()

    /**
     * 最新の通知ログ一覧。
     *
     * NotificationLogDao から取得し、StateFlow として公開する。
     */
    val notificationLogs: StateFlow<List<NotificationLogEntity>> = notificationLogRepo.observeLatestLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * 通知ログ一覧を表示中かどうかを示す状態。
     */
    private val _isShowingLogList = MutableStateFlow(false)
    val isShowingLogList = _isShowingLogList.asStateFlow()

    /**
     * 通知アクセス権限が付与されているかどうかを示す状態。
     */
    private val _notificationAccessGranted = MutableStateFlow(false)
    val notificationAccessGranted: StateFlow<Boolean> = _notificationAccessGranted.asStateFlow()

    /**
     * 通知アクセス権限の付与状態を更新する。
     */
    fun setNotificationAccessGranted(granted: Boolean) {
        _notificationAccessGranted.value = granted
    }

    init {
        observeRuleUpdates()
    }

     @OptIn(FlowPreview::class)
     /**
      * ルール更新バッファを監視し、Debounce 後に安全に保存する。
      */
    private fun observeRuleUpdates() {
        viewModelScope.launch {
            _ruleUpdateBuffer
                .debounce(500.milliseconds)
                .collect { saveRuleSafely(it) }
        }
    }

    /**
     * ルールを安全に保存する。
     *
     * 重複制約違反やその他の例外を捕捉し、UI へエラーメッセージを通知する。
     */
    private suspend fun saveRuleSafely(rule: RuleEntity) {
        try {
            rulesRepo.update(rule)
        } catch (_: SQLiteConstraintException) {
            _errorMessage.emit(appContext.getString(R.string.msg_wrn_dup_same_name))
        } catch (_: Exception) {
            _errorMessage.emit(appContext.getString(R.string.msg_err_save_failed))
        }
    }

    /**
     * 通知ログ一覧表示の ON/OFF を切り替える。
     */
    fun setShowingLogList(show: Boolean) {
        _isShowingLogList.value = show
    }

    /**
     * 通知ログから新しいルールを生成して追加する。
     *
     * 同一タイトルが存在する場合は連番を付与する。
     */
    fun addRuleFromLog(log: NotificationLogEntity) {
        viewModelScope.launch {
            try {
                val ruleBase = RuleEntity(
                    id = 0,
                    packageName = log.packageName,
                    appLabel = log.appLabel,
                    channelId = log.channelId,
                    srhTitle = "",
                    voiceMsg = "",
                    enabled = false,
                    channelName = log.channelName
                )

                when (val result =
                    rulesRepo.insertFromLog(ruleBase, "")
                ) {
                    is RulesRepository.InsertRuleResult.Success -> {
                        setShowingLogList(false)
                        _addRuleFromLogEvent.emit(
                            AddRuleFromLogEvent.Success(result.adoptedTitle)
                        )
                    }

                    RulesRepository.InsertRuleResult.TooManySameNames -> {
                        _addRuleFromLogEvent.emit(
                            AddRuleFromLogEvent.TooManySameNames
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MainViewModel", "addRuleFromLog failed", e)
                _addRuleFromLogEvent.emit(AddRuleFromLogEvent.Failed)
            }
        }
    }

    /**
     * 新しい順（ID 降順相当）で並べたルール一覧。
     */
    private val rulesByOrderNewest = rulesRepo
        .observeAllRulesOrderByNewest()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * パッケージ名昇順で並べたルール一覧。
     */
    private val rulesByOrderByPackageAsc = rulesRepo
        .observeRulesOrderByPackageAsc()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * 指定された並び順に応じたルール一覧を返す。
     */
    fun rules(orderNewest: Boolean = true): StateFlow<List<RuleEntity>> =
        if (orderNewest) rulesByOrderNewest else rulesByOrderByPackageAsc

    /**
     * ルールを新規追加する。
     */
    suspend fun insert(rule: RuleEntity) = rulesRepo.insert(rule)

    /**
     * ルールを即時保存する。
     *
     * フォーカスロスト時など、遅延せずに反映したい場合に使用する。
     */
    fun updateImmediate(rule: RuleEntity) {
        viewModelScope.launch {
            saveRuleSafely(rule)
        }
    }

    /**
     * ルール更新を Debounce 対象としてバッファに送る。
     */
    fun updateRuleDebounced(rule: RuleEntity) {
        viewModelScope.launch {
            _ruleUpdateBuffer.emit(rule)
        }
    }

    /**
     * 指定されたルールを削除する。
     */
    suspend fun delete(rule: RuleEntity) = rulesRepo.delete(rule)

    /**
     * 指定されたルールを複製する。
     */
    fun duplicateRule(rule: RuleEntity) {
        viewModelScope.launch {
            try {
                when (rulesRepo.duplicateRule(rule.id)) {
                    is RulesRepository.InsertRuleResult.Success -> {
                        // UI通知不要なら何もしない
                    }
                    RulesRepository.InsertRuleResult.TooManySameNames -> {
                        _addRuleFromLogEvent.emit(
                            AddRuleFromLogEvent.TooManySameNames
                        )
                    }
                }
            } catch (_: Exception) {
                _addRuleFromLogEvent.emit(AddRuleFromLogEvent.FailedCopy)
            }
        }
    }

    /**
     * ルール一覧の並び順設定。
     *
     * true の場合はパッケージ名昇順、false の場合は新しい順。
     */
    val sortList: StateFlow<Boolean> =
        dataStore.data
            .map { prefs -> prefs[AppPrefs.KEY_SORT_LIST_ORDER] ?: false }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * ルール一覧の並び順設定を更新する。
     */
    fun updateSortList(orderByPackageAsc: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AppPrefs.KEY_SORT_LIST_ORDER] = orderByPackageAsc
            }
        }
    }

    /**
     * テスト通知送信用の通知タイトル。
     */
    val notificationTitle: StateFlow<String> =
        dataStore.data
            .map { prefs -> 
                prefs[AppPrefs.KEY_NOTIFICATION_TITLE] ?: appContext.getString(R.string.default_check_notification_title) 
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, appContext.getString(R.string.default_check_notification_title))

    /**
     * 通知タイトルを更新する。
     */
    fun updateNotificationTitle(title: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AppPrefs.KEY_NOTIFICATION_TITLE] = title
            }
        }
    }

    /**
     * Rulesの指定したidのenabledを更新する。
     */
    fun updateRuleOfEnabled(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            rulesRepo.updateEnabled(id, enabled)
        }
    }
}
